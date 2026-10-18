package org.example;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JvnCoordImpl  extends UnicastRemoteObject implements JvnRemoteCoord {

    private int nextId = 0;
    private final Map<String, Integer> names = new HashMap<>();                 // nom -> id
    private final Map<Integer, JvnObject> objects = new ConcurrentHashMap<>();            // id -> JvnObject
    private final Map<Integer, Serializable> states = new HashMap<>();          // id -> dernier état validé
    private final Map<Integer, Set<JvnRemoteServer>> readers = new HashMap<>(); // id -> lecteurs
    private final Map<Integer, JvnRemoteServer> writer = new HashMap<>();       // id -> écrivain (ou absent)

    protected JvnCoordImpl() throws RemoteException {
    }


    public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js)
            throws RemoteException, JvnException {

        if (writer.get(joi) != null && !writer.get(joi).equals(js)){

            try {
                Serializable state =   writer.get(joi).jvnInvalidateWriterForReader(joi);
                states.put(joi,state);
                JvnRemoteServer w = writer.get(joi);
                readers.get(joi).add(w);
                writer.remove(joi);
            } catch (RemoteException e) {
                // L'écrivain est injoignable (panne) : on l'oublie, on garde le dernier état connu
                System.out.println("[Coord] écrivain injoignable sur l'objet " + joi + " : retiré");
                removeDeadClient(writer.get(joi));
            }
        }

        readers.get(joi).add(js);
        return states.get(joi);
    }


    public synchronized Serializable jvnLockWrite(int joi, JvnRemoteServer js)
            throws RemoteException, JvnException {

        // invalider le writer

        if (writer.get(joi) != null && !writer.get(joi).equals(js)){
          try{
              Serializable state =   writer.get(joi).jvnInvalidateWriter(joi);
              states.put(joi,state);
          }catch (RemoteException e) {
              System.out.println("[Coord] écrivain injoignable sur l'objet " + joi + " : retiré");
              removeDeadClient(writer.get(joi));
          }
        }
        // invalider tous les readers
        Set<JvnRemoteServer> rd = readers.getOrDefault(joi,new HashSet<>());
        for(JvnRemoteServer r : rd ){
            if(!r.equals(js)){
                try {
                    r.jvnInvalidateReader(joi);
                } catch (RemoteException e) {
                    System.out.println("[Coord] lecteur injoignable sur l'objet " + joi + " : retiré");
                    removeDeadClient(r);
                }            }
        }

        readers.put(joi,new HashSet<>());
        writer.put(joi,js);

        return states.get(joi);
    }



    public synchronized int jvnGetObjectId() throws RemoteException, JvnException {
        return nextId++;
    }

    public synchronized void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
            throws RemoteException, JvnException {
        if (names.containsKey(jon)) {
            throw new JvnException("Name already registered: " + jon);
        }
        int id = jo.jvnGetObjectId();
        names.put(jon, id);
        objects.put(id, jo);
        states.put(id, jo.jvnGetSharedObject());
        readers.putIfAbsent(id, new HashSet<>());
    }

    public synchronized JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
            throws RemoteException, JvnException {
        Integer id = names.get(jon);
        if (id == null) {
            return null;
        }
        return objects.get(id);
    }

    public synchronized void jvnTerminate(JvnRemoteServer js)
            throws RemoteException, JvnException {

        // 1. Si ce client est l'écrivain d'un objet, récupérer sa dernière valeur AVANT de l'oublier
        for (Map.Entry<Integer, JvnRemoteServer> e : writer.entrySet()) {
            if (e.getValue().equals(js)) {
                try {
                    Serializable state = js.jvnInvalidateWriter(e.getKey());
                    states.put(e.getKey(), state);
                } catch (RemoteException | JvnException ex) {
                    // client injoignable : on garde le dernier état connu et on continue le nettoyage
                }
            }
        }

        // 2. Ensuite seulement, l'oublier (comme avant)
        for (Set<JvnRemoteServer> r : readers.values()) {
            r.remove(js);
        }
        writer.values().removeIf(w -> w.equals(js));
        notifyAll();
    }



    private void removeDeadClient(JvnRemoteServer dead) {
        for (Set<JvnRemoteServer> r : readers.values()) {
            r.remove(dead);
        }
        writer.values().removeIf(w -> w.equals(dead));
    }
}
