package org.example.Services;


import org.example.Exceptions.JvnException;
import org.example.interfaces.JvnLocalServer;
import org.example.interfaces.JvnObject;
import org.example.interfaces.JvnRemoteCoord;
import org.example.interfaces.JvnRemoteServer;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class JvnServerImpl extends UnicastRemoteObject
        implements JvnLocalServer, JvnRemoteServer {

    private static JvnServerImpl js = null;
    private final JvnRemoteCoord coord;
   // private final Map<Integer, JvnObject> objects = new HashMap<>();
   private final Map<Integer, JvnObject> objects =  new java.util.concurrent.ConcurrentHashMap<>();

    private JvnServerImpl() throws Exception {
        super();
        Registry reg = LocateRegistry.getRegistry("localhost", 1099);
        coord = (JvnRemoteCoord) reg.lookup("JvnCoord");
    }

    public static synchronized JvnServerImpl jvnGetServer() {
        if (js == null) {
            try {
                js = new JvnServerImpl();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return js;
    }

    public JvnObject jvnCreateObject(Serializable o) throws JvnException {
        try {
            int id = coord.jvnGetObjectId();
            JvnObject jo = new JvnObjectImpl(id, o);
            objects.put(id, jo);
            return jo;
        } catch (RemoteException e) {
            throw new JvnException("Create failed: " + e.getMessage());
        }
    }

    public void jvnRegisterObject(String jon, JvnObject jo) throws JvnException {
        try {
            coord.jvnRegisterObject(jon, jo, this);
        } catch (RemoteException e) {
            throw new JvnException("Register failed: " + e.getMessage());
        }
    }

    public JvnObject jvnLookupObject(String jon) throws JvnException {
        try {
            JvnObject jo = coord.jvnLookupObject(jon, this);

            if (jo != null) {
                JvnObject existant = objects.get(jo.jvnGetObjectId());
                if (existant != null) return existant;   // déjà connu : on garde le même
                ((JvnObjectImpl) jo).resetLock();    // état du verrou remis à NL
                objects.put(jo.jvnGetObjectId(), jo);
            }
            return jo;
        } catch (RemoteException e) {
            throw new JvnException("Lookup failed: " + e.getMessage());
        }
    }

    public Serializable jvnLockRead(int joi) throws JvnException {
        try {
            return coord.jvnLockRead(joi, this);
        } catch (RemoteException e) {
            throw new JvnException("LockRead failed: " + e.getMessage());
        }
    }

    public Serializable jvnLockWrite(int joi) throws JvnException {
        try {
            return coord.jvnLockWrite(joi, this);
        } catch (RemoteException e) {
            throw new JvnException("LockWrite failed: " + e.getMessage());
        }
    }

 /*   public void jvnUnLock(int joi, Serializable state) throws JvnException {
        try {
           // this.objects.put(joi, (JvnObject) state);
            coord.jvnUnLock(joi, state, this);
        } catch (RemoteException e) {
            throw new JvnException("UnLock failed: " + e.getMessage());
        }
    } */

    public void jvnTerminate() throws JvnException {
        try {
            coord.jvnTerminate(this);
            UnicastRemoteObject.unexportObject(this, true);
            js = null;
        } catch (RemoteException e) {
            throw new JvnException("Terminate failed: " + e.getMessage());
        }
    }

    @Override
    public void jvnInvalidateReader(int id) throws RemoteException, JvnException {
        JvnObject jo = objects.get(id);
        if(jo == null) throw new JvnException("objet" + id + "inconnu de ce serveur");
         jo.jvnInvalidateReader();
    }

    @Override
    public Serializable jvnInvalidateWriter(int id) throws RemoteException, JvnException {
        JvnObject jo = objects.get(id);
        if (jo == null) throw new JvnException("objet " + id + " inconnu de ce serveur");
        return jo.jvnInvalidateWriter();
    }

    @Override
    public Serializable jvnInvalidateWriterForReader(int id) throws RemoteException, JvnException {
        JvnObject jo = objects.get(id);
        if (jo == null) throw new JvnException("objet " + id + " inconnu de ce serveur");
        return jo.jvnInvalidateWriterForReader();
    }
}