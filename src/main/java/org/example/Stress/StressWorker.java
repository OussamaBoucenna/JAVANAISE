package org.example.Stress;


import org.example.Proxy.HandlerProxy;
import org.example.Services.JvnServerImpl;
import org.example.interfaces.ICounter;
import org.example.interfaces.JvnObject;

import java.util.Random;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client de stress : utilise le proxy dynamique (annotations @JvnRead / @JvnWrite),
 * donc plus aucun jvnLockRead/jvnLockWrite/jvnUnLock manuel.
 * Arguments : nom objet iterations writePct departA [threads=1]
 */
public class StressWorker {
    public static void main(String[] args) throws Exception {
        String objName = args[1];
        int iterations = Integer.parseInt(args[2]);
        int writePct = Integer.parseInt(args[3]);
        long departA = Long.parseLong(args[4]);
        int nbThreads = args.length > 5 ? Integer.parseInt(args[5]) : 1;

        JvnServerImpl js = JvnServerImpl.jvnGetServer();
        JvnObject jo = js.jvnLookupObject(objName);
        if (jo == null) {
            System.out.println("ERREUR objet introuvable");
            System.exit(2);
        }
        ICounter counter = (ICounter) HandlerProxy.newInstance(jo, ICounter.class);

        // Attendre le départ commun : tous les clients démarrent ensemble
        long attente = departA - System.currentTimeMillis();
        if (attente > 0) Thread.sleep(attente);

        AtomicInteger writes = new AtomicInteger();
        AtomicInteger reculs = new AtomicInteger();

        Thread[] ts = new Thread[nbThreads];
        for (int t = 0; t < nbThreads; t++) {
            ts[t] = new Thread(() -> {
                Random rnd = new Random();
                int last = -1;   // dernière valeur LUE par ce thread (un compteur ne doit jamais reculer)
                for (int i = 0; i < iterations; i++) {
                    if (rnd.nextInt(100) < writePct) {
                        counter.increment();          // verrou W pris/relâché par le proxy
                        writes.incrementAndGet();
                    } else {
                        int v = counter.read();       // verrou R pris/relâché par le proxy
                        if (v < last) reculs.incrementAndGet();
                        last = v;
                    }
                }
            });
            ts[t].start();
        }
        for (Thread t : ts) t.join();

        System.out.println("RESULT " + writes.get() + " " + reculs.get());

        // Le coordinateur récupère notre dernier état dans jvnTerminate avant de nous oublier
        js.jvnTerminate();
        System.exit(0);
    }
}