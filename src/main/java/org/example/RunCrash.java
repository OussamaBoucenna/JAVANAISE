package org.example;


public class RunCrash {
    public static void main(String[] args) throws Exception {
        // clients, opérations par client, % d'écritures, timeout (s), mode panne
        StressLauncher.main(new String[]{"4", "300000", "50", "60", "crash"});
    }
}