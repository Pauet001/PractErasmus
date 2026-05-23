package it.pissir.breakout.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;
import java.sql.Connection;

// Configurem la connexió a la base de dades.
// Intentem connectar a MySQL. Si no funciona perquè està apagat,
// creem un DataSource d'H2 local per poder jugar igualment sense que peti el programa.
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(
            org.springframework.core.env.Environment env) {

        // Provem si connecta a MySQL
        String mysqlUrl  = env.getProperty("spring.datasource.url");
        String mysqlUser = env.getProperty("spring.datasource.username");
        String mysqlPass = env.getProperty("spring.datasource.password");

        try {
            DataSource mysql = DataSourceBuilder.create()
                .url(mysqlUrl)
                .username(mysqlUser)
                .password(mysqlPass)
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
            // Provem si realment podem agafar una connexió
            try (Connection c = mysql.getConnection()) {
                System.out.println("[DataSource] ✓ MySQL connectat: " + mysqlUrl);
                return mysql;
            }
        } catch (Exception e) {
            System.out.println("[DataSource] ✗ MySQL no disponible → Mode Edge (H2 en fitxer)");
            System.out.println("[DataSource]   Motiu: " + e.getMessage());
        }

        // Si ha fallat MySQL, tirem de H2 (base de dades local).
        // Si som una instància secundària (port 0), l'hem de crear en memòria
        // per evitar errors de bloqueig de fitxers si ja hi ha una instància oberta.
        boolean isSecondary = "0".equals(System.getProperty("server.port"));
        String h2Url = isSecondary
            ? "jdbc:h2:mem:breakout_edge_" + System.currentTimeMillis() + ";DB_CLOSE_DELAY=-1"
            : "jdbc:h2:file:./edge_h2_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

        System.out.println("[DataSource] → H2 " + (isSecondary ? "en memòria (instància secundària)" : "en fitxer (instància principal)"));

        return DataSourceBuilder.create()
            .url(h2Url)
            .username("sa")
            .password("")
            .driverClassName("org.h2.Driver")
            .build();
    }
}
