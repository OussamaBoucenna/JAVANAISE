package org.example.Tests;


import org.example.Models.Counter;
import org.example.Services.JvnServerImpl;
import org.example.interfaces.JvnObject;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

public class DemoGui {

    private static JvnServerImpl js;
    private static JvnObject jo;

    public static void main(String[] args) throws Exception {
        String nom = args.length > 0 ? args[0] : "Client";

        // ---------- Fenêtre ----------
        JFrame f = new JFrame("Javanaise - " + nom);
        JLabel valeur = new JLabel("?", SwingConstants.CENTER);
        valeur.setFont(new Font("SansSerif", Font.BOLD, 48));
        JTextArea logs = new JTextArea(15, 45);
        logs.setEditable(false);
        logs.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JButton lire = new JButton("Lire");
        JButton incr = new JButton("Incrémenter");
        JButton quitter = new JButton("Quitter");
        JPanel boutons = new JPanel();
        boutons.add(lire);
        boutons.add(incr);
        boutons.add(quitter);

        f.setLayout(new BorderLayout());
        f.add(valeur, BorderLayout.NORTH);
        f.add(new JScrollPane(logs), BorderLayout.CENTER);
        f.add(boutons, BorderLayout.SOUTH);
        f.pack();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);

        // Les System.out (tes traces) s'affichent dans la zone de logs
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                SwingUtilities.invokeLater(() -> logs.append(String.valueOf((char) b)));
            }
        }, true));

        // ---------- Connexion à l'objet partagé ----------
        js = JvnServerImpl.jvnGetServer();
        jo = js.jvnLookupObject("compteur");
        if (jo == null) {
            jo = js.jvnCreateObject(new Counter());
            js.jvnRegisterObject("compteur", jo);
            System.out.println(nom + " : compteur créé");
        } else {
            System.out.println(nom + " : compteur trouvé");
        }

        // ---------- Actions ----------
        lire.addActionListener(e -> enArrierePlan(() -> {
            jo.jvnLockRead();
            int v = ((Counter) jo.jvnGetSharedObject()).read();
            jo.jvnUnLock();
            SwingUtilities.invokeLater(() -> valeur.setText(String.valueOf(v)));
            System.out.println(nom + " lit : " + v);
        }));

        incr.addActionListener(e -> enArrierePlan(() -> {
            jo.jvnLockWrite();
            ((Counter) jo.jvnGetSharedObject()).increment();
            jo.jvnUnLock();
            System.out.println(nom + " a incrémenté");
        }));

        quitter.addActionListener(e -> enArrierePlan(() -> {
            js.jvnTerminate();
            System.exit(0);
        }));
    }

    /** Une action Javanaise, exécutée hors du thread graphique. */
    interface Action { void run() throws Exception; }

    private static void enArrierePlan(Action a) {
        new Thread(() -> {
            try {
                a.run();
            } catch (Exception ex) {
                System.out.println("ERREUR : " + ex.getMessage());
            }
        }).start();
    }
}