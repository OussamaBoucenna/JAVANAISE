package org.example;




import java.io.*;


public interface JvnObject extends Serializable {
    // Appelées par l'application
    public void jvnLockRead() throws JvnException;
    public void jvnLockWrite() throws JvnException;
    public void jvnUnLock() throws JvnException;
    public int jvnGetObjectId() throws JvnException;
    public Serializable jvnGetSharedObject() throws JvnException;

    public  void jvnInvalidateReader() throws JvnException;
    public Serializable jvnInvalidateWriter() throws JvnException;
    public Serializable jvnInvalidateWriterForReader() throws JvnException;

}