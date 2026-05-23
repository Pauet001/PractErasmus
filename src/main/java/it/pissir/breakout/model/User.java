package it.pissir.breakout.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

// Aquesta entitat representa un usuari registrat al joc.
// Guardem el seu nom d'usuari (que és la clau primària), la contrasenya (en text pla, de moment),
// una descripció de perfil, la seva foto en format text de base64 i si ha iniciat sessió (per controlar concurrents).
@Entity
@Table(name = "users")
public class User {
    
    @Id
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(length = 500)
    private String description;
    
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String base64Photo;

    // Per defecte cap usuari comença connectat
    @Column(nullable = false)
    private boolean loggedIn = false;

    // Constructors
    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters i setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBase64Photo() {
        return base64Photo;
    }

    public void setBase64Photo(String base64Photo) {
        this.base64Photo = base64Photo;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
}
