package org.example.interfaces;

import org.example.Exceptions.JvnException;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JvnRemoteServer extends Remote {
    // Les méthodes d'invalidation viendront avec la version cache
    void jvnInvalidateReader(int id) throws RemoteException, JvnException;
    Serializable jvnInvalidateWriter(int id) throws RemoteException, JvnException;
    Serializable jvnInvalidateWriterForReader(int id) throws RemoteException, JvnException;
}