package org.example.Tests;

import org.example.Stress.StressLauncher;

public class RunStress {
    public static void main(String[] args) throws Exception {
        // clients, opérations par client, % d'écritures, timeout (s)
        StressLauncher.main(new String[]{"4", "50000", "50", "120"});
    }
}