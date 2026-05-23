package it.pissir.breakout.model;

import jakarta.persistence.*;
import java.time.Instant;

// Aquesta entitat representa el resultat d'una partida de Breakout que ja ha acabat.
// Es guarda en local al fitxer JSON de l'Edge, i després es puja a MySQL gràcies al SyncService.
@Entity
@Table(name = "game_results")
public class GameResult {

    @Id
    private String gameId;

    @Column(nullable = false)
    private String playerId;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private String status;      // GAME_OVER o COMPLETED

    @Column(nullable = false)
    private String difficulty;

    @Column(nullable = false)
    private long blocksDestroyed;

    @Column(nullable = false)
    private Instant finishedAt;

    // false = guardada només en local al JSON; true = ja s'ha pujat a MySQL amb èxit
    @Column(nullable = false)
    private boolean synced = false;

    // Constructors necessaris per a Hibernate i per crear-los a mà
    public GameResult() {}

    public GameResult(String gameId, String playerId, int score,
                       String status, String difficulty,
                       long blocksDestroyed) {
        this.gameId         = gameId;
        this.playerId       = playerId;
        this.score          = score;
        this.status         = status;
        this.difficulty     = difficulty;
        this.blocksDestroyed= blocksDestroyed;
        this.finishedAt     = Instant.now();
        this.synced         = false;
    }

    // Getters i Setters normals i corrents
    public String  getGameId()          { return gameId;         }
    public String  getPlayerId()        { return playerId;       }
    public int     getScore()           { return score;          }
    public String  getStatus()          { return status;         }
    public String  getDifficulty()      { return difficulty;     }
    public long    getBlocksDestroyed() { return blocksDestroyed;}
    public Instant getFinishedAt()      { return finishedAt;     }
    public boolean isSynced()           { return synced;         }

    public void setSynced(boolean synced) { this.synced = synced; }
}
