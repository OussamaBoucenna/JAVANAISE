package org.example;


import java.util.Random;

import java.util.Random;

public class StressWorker {
    public static void main(String[] args) throws Exception {
        String nom = args[0];
        String objName = args[1];
        int iterations = Integer.parseInt(args[2]);
        int writePct = Integer.parseInt(args[3]);
        long departA = Long.parseLong(args[4]);

        JvnServerImpl js = JvnServerImpl.jvnGetServer();
        JvnObject jo = js.jvnLookupObject(objName);
        if (jo == null) {
            System.out.println("ERREUR objet introuvable");
            System.exit(2);
        }

        // Attendre le départ commun : tous les clients démarrent ensemble
        long attente = departA - System.currentTimeMillis();
        if (attente > 0) Thread.sleep(attente);

        Random rnd = new Random();
        int writes = 0;
        int reculs = 0;
        int last = -1;

        for (int i = 0; i < iterations; i++) {
            if (rnd.nextInt(100) < writePct) {
                jo.jvnLockWrite();
                Counter c = (Counter) jo.jvnGetSharedObject();
                int avant = c.read();
                if (avant < last) reculs++;
                c.increment();
                last = avant + 1;
                jo.jvnUnLock();
                writes++;
            } else {
                jo.jvnLockRead();
                int v = ((Counter) jo.jvnGetSharedObject()).read();
                jo.jvnUnLock();
                if (v < last) reculs++;
                last = v;
            }
        }

        System.out.println("RESULT " + writes + " " + reculs);

        // Rester vivant : le lanceur lira la valeur finale, puis nous arrêtera.
        // Pas de jvnTerminate : le coordinateur pourra encore nous invalider.
        Thread.sleep(Long.MAX_VALUE);
    }
}