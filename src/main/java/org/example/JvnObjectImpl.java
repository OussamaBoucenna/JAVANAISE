package org.example;


import java.io.Serializable;
import java.util.concurrent.locks.Lock;

public class JvnObjectImpl implements JvnObject {


    private final int id;
    private Serializable sharedObject;
    private transient LockState lock;

    public JvnObjectImpl(int id, Serializable o) {
        this.id = id;
        this.sharedObject = o;
        this.lock = LockState.NL;
    }

    void resetLock() {
        lock = LockState.NL;
    }

    public synchronized void jvnLockRead() throws JvnException {
        switch (lock) {
            case NL -> {
                Serializable s = JvnServerImpl.jvnGetServer().jvnLockRead(id);
                if (s != null) sharedObject = s;
                setLock(LockState.R, "Appel Coordinateur");
               // lock = LockState.R;
            }
            case RC -> /* lock = LockState.R */ setLock(LockState.R, "cache, pas de réseau") ;
            case WC -> /* lock = LockState.RWC */ setLock(LockState.RWC, "cache, pas de réseau");
            default -> throw new JvnException(
                    "lockRead: lock already held on object " + id + " (state " + lock + ")");
        }
    }

    public synchronized void jvnLockWrite() throws JvnException {
        switch (lock) {
            case NL, RC -> {
                // Une lecture en cache ne suffit pas pour écrire : coordinateur
                Serializable s = JvnServerImpl.jvnGetServer().jvnLockWrite(id);
                if (s != null) sharedObject = s;
               // lock = LockState.W;
                setLock(LockState.W, "Appel Coordinateur");
            }
            case WC -> /*lock = LockState.W*/ setLock(LockState.W, "cache, pas de réseau");   // écriture en cache : pas de réseau
            default -> throw new JvnException(
                    "lockWrite: lock already held on object " + id + " (state " + lock + ")");
        }
    }

    public synchronized void jvnUnLock() throws JvnException {
        switch (lock) {
            case R   -> setLock(LockState.RC, "unlock, gardé en cache");
            case W   -> setLock(LockState.WC, "unlock, gardé en cache");
            case RWC -> setLock(LockState.WC, "unlock, gardé en cache");
            default  -> throw new JvnException(
                    "Unlock without lock on object " + id + " (state " + lock + ")");
        }
        notifyAll();
    }

    public int jvnGetObjectId() throws JvnException {
        return id;
    }

    public synchronized Serializable jvnGetSharedObject() throws JvnException {
        return sharedObject;
    }

    @Override
    public synchronized void jvnInvalidateReader() throws JvnException {
        // Lecture en cours : on attend le unLock de l'application (R -> RC)
        while (lock == LockState.R) {
            System.out.println("  [Objet " + id + "] invalidateReader : attente du unlock...");
            try {
                wait();
            } catch (InterruptedException e) {
                throw new JvnException("invalidateReader interrompu sur l'objet " + id);
            }
        }
        setLock(LockState.NL, "invalidateReader");
    }

    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException {
        while (lock == LockState.W || lock == LockState.RWC) {
            System.out.println("  [Objet " + id + "] invalidateWriter : attente du unlock...");
            try {
                wait();
            } catch (InterruptedException e) {
                throw new JvnException("invalidateWriter interrompu sur l'objet " + id);
            }
        }
        setLock(LockState.NL, "invalidateWriter");
        return sharedObject;
    }

    @Override
    public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
        while (lock == LockState.W) {
            System.out.println("  [Objet " + id + "] invalidateWriterForReader : attente du unlock...");
            try {
                wait();
            } catch (InterruptedException e) {
                throw new JvnException("invalidateWriterForReader interrompu sur l'objet " + id);
            }
        }
        switch (lock) {
            case RWC -> setLock(LockState.R, "invalidateWriterForReader");
            case WC  -> setLock(LockState.RC, "invalidateWriterForReader");
            default  -> { }
        }
        return sharedObject;
    }


    private void setLock(LockState nouveau, String raison) {
       // System.out.println("  [Objet " + id + "] " + lock + " -> " + nouveau + "   (" + raison + ")");
        lock = nouveau;
    }
}