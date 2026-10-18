package org.example;


import java.io.Serializable;
import java.util.concurrent.locks.Lock;

public class JvnObjectImpl implements JvnObject {


    private final int id;
    private Serializable sharedObject;
    private transient LockState lock;
    private transient LockState pending;   // demande en cours auprès du coordinateur

    public JvnObjectImpl(int id, Serializable o) {
        this.id = id;
        this.sharedObject = o;
        this.lock = LockState.NL;
    }

    void resetLock() {
        pending = null ;
        lock = LockState.NL;
    }


    public void jvnLockRead() throws JvnException {
        synchronized (this) {
            switch (lock) {
                case RC -> { setLock(LockState.R,   "cache, pas de réseau"); return; }
                case WC -> { setLock(LockState.RWC, "cache, pas de réseau"); return; }
                case NL -> pending = LockState.R;               // le "post-it"
                default -> throw new JvnException(
                        "lockRead: lock already held on object " + id + " (state " + lock + ")");
            }
        }
        Serializable s;
        try {
            s = JvnServerImpl.jvnGetServer().jvnLockRead(id);
        } catch (JvnException | RuntimeException e) {
            synchronized (this) { pending = null; notifyAll(); }
            throw e;
        }
        synchronized (this) {
            if (s != null) sharedObject = s;
            setLock(LockState.R, "Appel Coordinateur");
            pending = null;                                     // on retire le post-it
            notifyAll();                                        // réveille les invalidations en attente
        }
    }

    public void jvnLockWrite() throws JvnException {
        synchronized (this) {
            switch (lock) {
                case WC -> { setLock(LockState.W, "cache, pas de réseau"); return; }
                case NL, RC -> pending = LockState.W;
                default -> throw new JvnException(
                        "lockWrite: lock already held on object " + id + " (state " + lock + ")");
            }
        }
        Serializable s;
        try {
            s = JvnServerImpl.jvnGetServer().jvnLockWrite(id);
        } catch (JvnException | RuntimeException e) {
            synchronized (this) { pending = null; notifyAll(); }
            throw e;
        }
        synchronized (this) {
            if (s != null) sharedObject = s;
            setLock(LockState.W, "Appel Coordinateur");
            pending = null;
            notifyAll();
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
        while (lock == LockState.R  || pending == LockState.R) {
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
        while (lock == LockState.W || lock == LockState.RWC  || pending == LockState.W) {
           // System.out.println("  [Objet " + id + "] invalidateWriter : attente du unlock...");
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
        while (lock == LockState.W  || pending == LockState.W) {
          //  System.out.println("  [Objet " + id + "] invalidateWriterForReader : attente du unlock...");
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