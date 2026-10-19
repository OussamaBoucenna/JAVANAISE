package org.example.Tests;

import org.example.Models.Counter;
import org.example.Proxy.HandlerProxy;
import org.example.Services.JvnServerImpl;
import org.example.interfaces.ICounter;
import org.example.interfaces.JvnObject;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Test extension 6 : plusieurs threads dans UNE JVM + un autre client (autre JVM) en même temps. */
public class RunMulti {
    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        int ops     = args.length > 1 ? Integer.parseInt(args[1]) : 2000;
        String name = "MT" + System.currentTimeMillis();

        JvnServerImpl js = JvnServerImpl.jvnGetServer();
        JvnObject jo = js.jvnCreateObject(new Counter());
        js.jvnRegisterObject(name, jo);
        jo.jvnLockWrite(); jo.jvnUnLock();                       // on garde le verrou en cache

        long depart = System.currentTimeMillis() + 2000;
        // client externe (autre JVM), 1 thread
        Process p = new ProcessBuilder("java", "-cp", System.getProperty("java.class.path"),
                "org.example.Stress.StressWorker", "ext", name, String.valueOf(ops), "50", String.valueOf(depart))
                .redirectErrorStream(true).start();

        ICounter c = (ICounter) HandlerProxy.newInstance(jo, ICounter.class);
        AtomicInteger writes = new AtomicInteger(), reculs = new AtomicInteger();
        Thread[] ts = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            ts[t] = new Thread(() -> {
                try {
                    java.util.Random r = new java.util.Random();
                    int last = -1;
                    Thread.sleep(Math.max(0, depart - System.currentTimeMillis()));
                    for (int i = 0; i < ops; i++) {
                        if (r.nextInt(100) < 50) { c.increment(); writes.incrementAndGet(); }
                        else { int v = c.read(); if (v < last) reculs.incrementAndGet(); last = v; }
                    }
                } catch (Throwable e) { e.printStackTrace(); }
            });
            ts[t].start();
        }
        for (Thread t : ts) t.join();

        int extW = 0, extR = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String l;
            while ((l = br.readLine()) != null)
                if (l.startsWith("RESULT")) { String[] x = l.split(" "); extW = Integer.parseInt(x[1]); extR = Integer.parseInt(x[2]); }
        }
        p.waitFor();
        int attendu = writes.get() + extW;
        int obtenu = c.read();
        System.out.println("Threads locaux : " + threads + " x " + ops + " ops, writes=" + writes.get() + ", reculs=" + reculs.get());
        System.out.println("Client externe : writes=" + extW + ", reculs=" + extR);
        System.out.println("Attendu " + attendu + " / Obtenu " + obtenu + " -> " + ((attendu == obtenu && reculs.get() + extR == 0) ? "OK" : "KO"));
        js.jvnTerminate();
        System.exit(0);
    }
}