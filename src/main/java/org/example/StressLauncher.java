package org.example;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StressLauncher {

    public static void main(String[] args) throws Exception {
        int nbClients  = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int iterations = args.length > 1 ? Integer.parseInt(args[1]) : 2000;
        int writePct   = args.length > 2 ? Integer.parseInt(args[2]) : 50;
        int timeoutSec = args.length > 3 ? Integer.parseInt(args[3]) : 120;

        System.out.printf("Test : %d clients x %d opérations, %d%% d'écritures, timeout %ds%n",
                nbClients, iterations, writePct, timeoutSec);

        // 1. Créer un compteur neuf, avec un nom unique pour ce test
        String objName = "stress-" + System.currentTimeMillis();
        JvnServerImpl js = JvnServerImpl.jvnGetServer();
        JvnObject jo = js.jvnCreateObject(new Counter());
        js.jvnRegisterObject(objName, jo);

        // 2. Lancer les clients, avec un départ commun dans 3 secondes
        long departA = System.currentTimeMillis() + 3000;
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String cp = System.getProperty("java.class.path");
        List<Process> procs = new ArrayList<>();
        List<List<String>> sorties = new ArrayList<>();

        for (int i = 0; i < nbClients; i++) {
            Process p = new ProcessBuilder(java, "-cp", cp, "org.example.StressWorker",
                    "C" + i, objName, String.valueOf(iterations),
                    String.valueOf(writePct), String.valueOf(departA))
                    .redirectErrorStream(true).start();
            List<String> lignes = new ArrayList<>();
            sorties.add(lignes);
            procs.add(p);
            new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String l;
                    while ((l = r.readLine()) != null) {
                        synchronized (lignes) { lignes.add(l); }
                    }
                } catch (Exception ignored) { }
            }).start();
        }

        // 3. Attendre que chaque client ait affiché son RESULT (ou le timeout)
        long limite = System.currentTimeMillis() + timeoutSec * 1000L;
        long attendu = 0;
        long reculs = 0;
        boolean[] fini = new boolean[nbClients];
        int nbFinis = 0;

        while (nbFinis < nbClients && System.currentTimeMillis() < limite) {
            for (int i = 0; i < nbClients; i++) {
                if (fini[i]) continue;
                synchronized (sorties.get(i)) {
                    for (String l : sorties.get(i)) {
                        if (l.startsWith("RESULT")) {
                            String[] t = l.split(" ");
                            attendu += Long.parseLong(t[1]);
                            reculs += Long.parseLong(t[2]);
                            fini[i] = true;
                            nbFinis++;
                        }
                    }
                }
                if (!fini[i] && !procs.get(i).isAlive()) {
                    System.out.println("*** Le client C" + i + " s'est arrêté sans résultat : ***");
                    synchronized (sorties.get(i)) { sorties.get(i).forEach(System.out::println); }
                    procs.forEach(Process::destroyForcibly);
                    System.exit(1);
                }
            }
            Thread.sleep(100);
        }
        long duree = System.currentTimeMillis() - departA;

        if (nbFinis < nbClients) {
            System.out.println("\n*** ÉCHEC : " + (nbClients - nbFinis) + " client(s) bloqué(s) après "
                    + timeoutSec + "s -> interblocage probable ***");
            System.out.println("Astuce : 'jstack <pid>' sur une JVM bloquée pour voir où elle attend.");
            procs.forEach(Process::destroyForcibly);
            System.exit(1);
        }

        // 4. Lire la valeur finale PENDANT que les clients sont encore vivants :
        //    le coordinateur invalide le dernier écrivain et récupère sa copie.
        jo.jvnLockRead();
        int obtenu = ((Counter) jo.jvnGetSharedObject()).read();
        jo.jvnUnLock();

        // 5. Seulement maintenant, arrêter les clients
        procs.forEach(Process::destroyForcibly);

        System.out.println("\n========== RÉSULTAT ==========");
        System.out.println("Valeur attendue  : " + attendu);
        System.out.println("Valeur obtenue   : " + obtenu);
        System.out.println("Reculs observés  : " + reculs);
        System.out.printf("Durée            : %.1f s (%.0f opérations/s)%n",
                duree / 1000.0, nbClients * iterations * 1000.0 / duree);
        System.out.println(obtenu == attendu && reculs == 0 ? "VERDICT : OK" : "VERDICT : ÉCHEC");
        System.exit(0);
    }
}