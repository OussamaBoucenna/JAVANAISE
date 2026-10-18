package org.example;


import java.util.Scanner;

public class TestClient {
    public static void main(String[] args) throws Exception {
        JvnServerImpl js = JvnServerImpl.jvnGetServer();
        if (js == null) {
            System.out.println("Impossible de contacter le coordinateur");
            return;
        }

        JvnObject jo = js.jvnLookupObject("COUNTER");
        if (jo == null) {
            jo = js.jvnCreateObject(new Counter());
            try {
                js.jvnRegisterObject("COUNTER", jo);
            } catch (JvnException e) {
                jo = js.jvnLookupObject("COUNTER");
            }
        }

        System.out.println("Commandes : inc | r | burst <n> | hold | q");
        Scanner in = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String line = in.nextLine().trim();

            if (line.equals("inc")) {
                jo.jvnLockWrite();
                ((Counter) jo.jvnGetSharedObject()).increment();
                jo.jvnUnLock();
                System.out.println("Incrémenté.");

            } else if (line.equals("r")) {
                jo.jvnLockRead();
                int v = ((Counter) jo.jvnGetSharedObject()).read();
                jo.jvnUnLock();
                System.out.println("Valeur : " + v);

            } else if (line.startsWith("burst ")) {
                int n = Integer.parseInt(line.substring(6).trim());
                jo.jvnLockWrite();
                for (int i = 0; i < n; i++) {

                    ((Counter) jo.jvnGetSharedObject()).increment();

                }
                jo.jvnUnLock();
                System.out.println(n + " incréments faits.");

            } else if (line.equals("hold")) {
                jo.jvnLockWrite();
                System.out.println("Verrou en écriture pris. Appuie sur Entrée pour le relâcher...");
                in.nextLine();
                jo.jvnUnLock();
                System.out.println("Verrou relâché.");

            } else if (line.equals("q")) {
                js.jvnTerminate();
                System.out.println("Bye.");
                System.exit(0);

            } else if (!line.isEmpty()) {
                System.out.println("Commande inconnue.");
            }
        }
    }
}