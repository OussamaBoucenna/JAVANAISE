package org.example;


import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestCoord {
    public static void main(String[] args) {
        try {
            Registry reg;
            try {
                reg = LocateRegistry.createRegistry(1099);
            } catch (Exception e) {
                // un registre existe déjà sur le port 1099 : on le réutilise
                reg = LocateRegistry.getRegistry(1099);
            }

            JvnCoordImpl coord = new JvnCoordImpl();
            reg.rebind("JvnCoord", coord);

            System.out.println("Coordinateur prêt sur le port 1099.");
            System.out.println("Lance maintenant les TestClient.");
        } catch (Exception e) {
            System.out.println("Erreur au démarrage du coordinateur : " + e);
            e.printStackTrace();
        }
    }
}