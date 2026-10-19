package org.example.Tests;


import org.example.Exceptions.JvnException;
import org.example.Models.Counter;
import org.example.Proxy.HandlerProxy;
import org.example.Services.JvnServerImpl;
import org.example.interfaces.ICounter;
import org.example.interfaces.JvnObject;

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
        ICounter obj = (ICounter) HandlerProxy.newInstance(jo,ICounter.class) ;
        System.out.println("Commandes : inc | r | burst <n> | hold | q");
        Scanner in = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String line = in.nextLine().trim();

            if (line.equals("inc")) {
               obj.increment();

            } else if (line.equals("r")) {
                int v = obj.read();
                System.out.println("Valeur : " + v);

            } else if (line.startsWith("burst ")) {

            } else if (line.equals("hold")) {

            } else if (line.equals("q")) {

            } else if (!line.isEmpty()) {
                System.out.println("Commande inconnue.");
            }
        }
    }
}