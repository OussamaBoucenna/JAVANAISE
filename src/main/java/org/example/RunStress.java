package org.example;

public class RunStress {
    public static void main(String[] args) throws Exception {
        // clients, opérations par client, % d'écritures, timeout (s)
        StressLauncher.main(new String[]{"7", "50000", "10000", "120"});
    }
}