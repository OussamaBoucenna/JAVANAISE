package org.example.Services;


import org.example.Enums.LockState;
import org.example.Exceptions.JvnException;
import org.example.interfaces.JvnObject;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {


    private final int id;
    private Serializable sharedObject;
    private transient LockState lock;
    private transient LockState pending;   // demande en cours auprès du coordinateur
    private transient int readers;            // nombre de threads LOCAUX en train de lire


    public JvnObjectImpl(int id, Serializable o) {
        this.id = id;
        this.sharedObject = o;
        this.lock = LockState.NL;
    }

    void resetLock() {
        pending = null ;
        lock = LockState.NL;
        readers = 0;

    }


    public void jvnLockRead() throws JvnException {
        synchronized (this) {                                   // TEMPS 1 : court
            while (true) {
                if (pending != null || lock == LockState.W) {   // un autre thread a une demande en vol, ou écrit
                    waitSafe();
                    continue;
                }
                switch (lock) {
                    case RC  -> { readers = 1; setLock(LockState.R,   "cache, pas de réseau"); return; }
                    case WC  -> { readers = 1; setLock(LockState.RWC, "cache, pas de réseau"); return; }
                    case R, RWC -> { readers++; return; }       // NOUVEAU : un lecteur de plus, en parallèle
                    case NL  -> pending = LockState.R;          // le "post-it"
                    default  -> throw new JvnException("lockRead: état inattendu " + lock);
                }
                break;                                          // on a posé le post-it : direction le réseau
            }
        }
        Serializable s;                                         // TEMPS 2 : réseau, SANS moniteur
        try {
            s = JvnServerImpl.jvnGetServer().jvnLockRead(id);
        } catch (JvnException | RuntimeException e) {
            synchronized (this) { pending = null; notifyAll(); }
            throw e;
        }
        synchronized (this) {                                   // TEMPS 3 : court
            if (s != null) sharedObject = s;
            readers = 1;
            setLock(LockState.R, "Appel Coordinateur");
            pending = null;
            notifyAll();
        }
    }

    public void jvnLockWrite() throws JvnException {
        synchronized (this) {
            while (true) {
                // on attend : demande en vol, écrivain local, ou lecteurs locaux encore en train de lire
                if (pending != null || lock == LockState.W || lock == LockState.R || lock == LockState.RWC) {
                    waitSafe();
                    continue;
                }
                switch (lock) {
                    case WC -> { setLock(LockState.W, "cache, pas de réseau"); return; }
                    case NL, RC -> pending = LockState.W;
                    default -> throw new JvnException("lockWrite: état inattendu " + lock);
                }
                break;
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






    private void waitSafe() throws JvnException {
        try {
            wait();
        } catch (InterruptedException e) {
            throw new JvnException("interrompu en attendant le verrou sur l'objet " + id);
        }
    }

    public synchronized void jvnUnLock() throws JvnException {
        switch (lock) {
            case R   -> { if (--readers <= 0) { readers = 0; setLock(LockState.RC, "dernier lecteur parti, gardé en cache"); } }
            case RWC -> { if (--readers <= 0) { readers = 0; setLock(LockState.WC, "dernier lecteur parti, gardé en cache"); } }
            case W   -> setLock(LockState.WC, "unlock, gardé en cache");
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



/* public void jvnLockRead() throws JvnException {
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

*/