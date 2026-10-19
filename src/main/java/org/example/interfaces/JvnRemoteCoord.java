package org.example.interfaces;

import org.example.Exceptions.JvnException;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JvnRemoteCoord extends Remote {
    public int jvnGetObjectId() throws RemoteException, JvnException;
    public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
            throws RemoteException, JvnException;
    public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
            throws RemoteException, JvnException;

    public Serializable jvnLockRead(int joi, JvnRemoteServer js) throws RemoteException, JvnException;
    public Serializable jvnLockWrite(int joi, JvnRemoteServer js) throws RemoteException, JvnException;
   /* public void jvnUnLock(int joi, Serializable state, JvnRemoteServer js)
            throws RemoteException, JvnException;  */

    public void jvnTerminate(JvnRemoteServer js) throws RemoteException, JvnException;
}
