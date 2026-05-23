package it.pissir.breakout;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punt d'entrada de la plataforma Breakout.
 *
 * Arrenca el servidor Spring Boot amb:
 * - API REST (port 8080) → accessible via Postman
 * - SyncService programat → sincronitza edge → MySQL
 * - Servidor estàtic → pàgina de classificació web
 */

@SpringBootApplication
@EnableScheduling
public class BreakoutApplication {
    public static void main(String[] args) {
        // Comprovar si el port 8080 ja està en ús
        boolean portAvailable = true;
        try (java.net.ServerSocket ignored = new java.net.ServerSocket(8080)) {
            // El port està lliure
        } catch (java.io.IOException e) {
            // El port està ocupat per una altra instància
            portAvailable = false;
        }

        if (!portAvailable) {
            System.out.println(
                    "El port 8080 ja està en ús. Iniciant la partida en un port aleatori per permetre múltiples instàncies...");
            System.setProperty("server.port", "0");
        }

        
        new org.springframework.boot.builder.SpringApplicationBuilder(BreakoutApplication.class)
                .headless(false)
                .run(args);
    }
}
