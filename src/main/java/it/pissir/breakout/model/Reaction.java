package it.pissir.breakout.model;

import jakarta.persistence.*;

// Aquesta entitat representa una reacció (com un m'agrada "LIKE", foc "FIRE" o sorprès "SURPRISED")
// que un jugador posa a la partida d'un altre mentre la mira.
@Entity
@Table(name = "reactions")
public class Reaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String gameId;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String reactionType; // Exemple: "LIKE", "FIRE", "SURPRISED"

    // Constructor buit per a Hibernate
    public Reaction() {}

    public Reaction(String gameId, String username, String reactionType) {
        this.gameId = gameId;
        this.username = username;
        this.reactionType = reactionType;
    }

    // Getters i setters habituals
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getReactionType() { return reactionType; }
    public void setReactionType(String reactionType) { this.reactionType = reactionType; }
}
