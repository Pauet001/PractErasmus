package it.pissir.breakout.game;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

// Aquesta classe conté tota la informació d'una partida de Breakout que s'està jugant.
// Guarda:
//   - Les pilotes que hi ha en joc, la pala del jugador i la graella de blocs
//   - La puntuació, les vides que li queden al jugador i l'estat actual de la partida (jugant, pausada, acabada...)
//   - Els power-ups que estan actius o caient per la pantalla
// Per evitar problemes de concurrència entre el fil de la física (BallThread) i les peticions REST de la web,
// fem servir variables 'volatile', 'synchronized' i llistes concurrents (CopyOnWriteArrayList).
public class GameInstance {

    public enum Status { ACTIVE, PAUSED, GAME_OVER, COMPLETED }

    // --- IDENTITAT I DADES BÀSIQUES ---
    private final String gameId;
    private final String playerId;
    private final String difficulty;
    private final Instant createdAt = Instant.now();

    // --- ELEMENTS DEL JOC ---
    // Fem servir CopyOnWriteArrayList perquè el fil de física pot estar movent-les
    // mentre el controlador REST llegeix la llista per enviar-la al frontend web.
    private final List<Ball>  balls;
    private final Paddle      paddle;
    private final List<Block> blocks;

    // --- DADES DEL MARCADOR ---
    private final AtomicInteger score = new AtomicInteger(0);
    private volatile int        lives = 3;
    private volatile Status     status = Status.ACTIVE;

    // --- LIMITS DE LA PANTALLA ---
    private final int boardWidth;
    private final int boardHeight;

    // Llista concurrent per als power-ups que estan caient cap a la pala
    private final List<FallingPowerUp> fallingPowerUps = new CopyOnWriteArrayList<>();

    // Temps de caducitat dels power-ups (en mil·lisegons)
    @JsonIgnore
    private volatile long doubleScoreExpiresAt = 0;

    @JsonIgnore
    private final Set<PowerUpType> activatedPowerUps = new java.util.concurrent.CopyOnWriteArraySet<>();

    // Comptador senzill per anar donant IDs únics a les pilotes noves
    @JsonIgnore
    private final AtomicInteger ballIdCounter = new AtomicInteger(1);

    // Velocitat de la pilota segons la dificultat escollida
    private final double initialSpeed;

    public GameInstance(String gameId, String playerId, String difficulty,
                        int boardWidth, int boardHeight,
                        int paddleWidth, int paddleHeight, int paddleStep,
                        int blockCols, int blockRows, double initialSpeed) {
        this.gameId      = gameId;
        this.playerId    = playerId;
        this.difficulty  = difficulty;
        this.boardWidth  = boardWidth;
        this.boardHeight = boardHeight;
        this.initialSpeed= speedForDifficulty(difficulty, initialSpeed);

        // Creem la pala del jugador
        this.paddle = new Paddle(boardWidth, boardHeight,
                                 paddleWidth, paddleHeight, paddleStep);

        // Posem la primera pilota en joc
        this.balls = new CopyOnWriteArrayList<>();
        spawnBall();

        // Generem tots els blocs del tauler
        this.blocks = new CopyOnWriteArrayList<>(
            buildBlocks(blockCols, blockRows));
    }

    // --- INICIALITZACIÓ DE JOC ---

    // Crea una pilota nova al mig del tauler amb un angle aleatori cap amunt
    public Ball spawnBall() {
        int id = ballIdCounter.getAndIncrement();
        // Angle a l'atzar entre 30 i 150 graus per apuntar cap a dalt
        double angle = Math.toRadians(30 + new Random().nextInt(120));
        double vx = initialSpeed * Math.cos(angle);
        double vy = -Math.abs(initialSpeed * Math.sin(angle)); // La VY ha de ser negativa per anar cap amunt
        Ball b = new Ball(id, boardWidth / 2.0, boardHeight * 0.7, vx, vy);
        balls.add(b);
        return b;
    }

    // Aquesta funció genera tota la graella de blocs segons la dificultat
    private List<Block> buildBlocks(int cols, int rows) {
        List<Block> list = new ArrayList<>();
        int marginX = 30;
        int marginY = 60;
        int gapX    = 5;
        int gapY    = 5;
        Random rng  = new Random();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int bx = marginX + col * (Block.WIDTH + gapX);
                int by = marginY + row * (Block.HEIGHT + gapY);

                int hits = 1;
                // Si és dificultat normal o mitjana, alternem les files per fer blocs més durs
                if ("normal".equalsIgnoreCase(this.difficulty) || "medium".equalsIgnoreCase(this.difficulty)) {
                    hits = (row % 2 == 0) ? 2 : 1;
                } else if ("hard".equalsIgnoreCase(this.difficulty)) {
                    // Dificultat difícil: blocs indestructibles, espais buits i més resistència
                    if (row == 0) hits = 3;
                    else if (row == 1) hits = 2;
                    else if (row == 2 && (col == 2 || col == cols - 3)) hits = Integer.MAX_VALUE; // Indestructible
                    else if (row == 3 && (col % 2 == 0)) hits = 3;
                    else if (row == 4 && (col == 3 || col == cols - 4)) continue; // Aquí no posem bloc
                    else hits = 1;
                }

                // 15% de probabilitat que el bloc contingui un power-up (excepte si és indestructible)
                PowerUpType pu = null;
                if (hits != Integer.MAX_VALUE && rng.nextInt(100) < 15) {
                    PowerUpType[] types = PowerUpType.values();
                    pu = types[rng.nextInt(types.length)];
                }

                list.add(new Block(bx, by, hits, pu));
            }
        }
        return list;
    }

    // --- GESTIÓ DE POWER-UPS ---

    // Fa aparèixer un power-up físicament caient des de la posició del bloc destruït
    public void dropPowerUp(double x, double y, PowerUpType type) {
        fallingPowerUps.add(new FallingPowerUp(x, y, type));
    }

    // Activa l'efecte del power-up quan la pala l'agafa
    public void activatePowerUp(PowerUpType type) {
        long now = System.currentTimeMillis();
        switch (type) {
            case EXTRA_BALL  -> spawnBall(); // Llança una altra pilota a jugar
            case DOUBLE_SCORE -> doubleScoreExpiresAt = now + 10_000; // Activa puntuació doble durant 10 segons
        }
    }

    // Comprova si els power-ups ja s'han caducat (es fa al treure o afegir)
    public void checkPowerUpExpiry() {
        // En aquesta versió només tenim el de puntuació doble que ja comprovem al moment
    }

    // --- LÒGICA PRINCIPAL DEL JOC ---

    // Cridat per la física quan una pilota surt per sota del tauler
    public synchronized int ballLost(Ball ball) {
        balls.remove(ball);
        // Si no ens queden més pilotes en pantalla, perdem una vida
        if (balls.isEmpty()) {
            lives--;
            if (lives <= 0) {
                status = Status.GAME_OVER; // Joc acabat si no tenim més vides
            } else {
                spawnBall(); // Si encara ens queden vides, en llancem una de nova al mig
            }
        }
        return lives;
    }

    // Afegeix punts a la partida i comprova si hem destruït tots els blocs per guanyar
    public void addScore(int pts) {
        // Si el power-up de puntuació doble està actiu, dupliquem els punts rebuts
        if (doubleScoreExpiresAt > System.currentTimeMillis()) {
            pts *= 2;
        }
        score.addAndGet(pts);
        
        // Mirem si tots els blocs trencables ja s'han destruït. Si és així, victòria!
        if (blocks.stream().allMatch(b -> b.isDestroyed() || b.getHitsRemaining() == Integer.MAX_VALUE)) {
            status = Status.COMPLETED;
        }
    }

    // --- GETTERS I SETTERS ---

    public String      getGameId()    { return gameId;    }
    public String      getPlayerId()  { return playerId;  }
    public String      getDifficulty(){ return difficulty;}
    public Instant     getCreatedAt() { return createdAt; }
    public List<Ball>  getBalls()     { return balls;     }
    public Paddle      getPaddle()    { return paddle;    }
    public List<Block> getBlocks()    { return blocks;    }
    public List<FallingPowerUp> getFallingPowerUps() { return fallingPowerUps; }
    public int         getScore()     { return score.get();}
    public int         getLives()     { return lives;     }
    public Status      getStatus()    { return status;    }
    public int         getBoardWidth(){ return boardWidth; }
    public int         getBoardHeight(){ return boardHeight;}

    public void setStatus(Status s)   { this.status = s; }

    // Retorna quants blocs queden per trencar
    public long getBlocksRemaining() {
        return blocks.stream().filter(b -> !b.isDestroyed() && b.getHitsRemaining() != Integer.MAX_VALUE).count();
    }

    // Retorna el percentatge de temps que queda de puntuació doble (de 0.0 a 1.0)
    public double getDoubleScoreTimeRemaining() {
        long now = System.currentTimeMillis();
        if (doubleScoreExpiresAt <= now) return 0.0;
        return (doubleScoreExpiresAt - now) / 10000.0;
    }

    // Ajusta la velocitat inicial de la pilota segons la dificultat
    private double speedForDifficulty(String diff, double base) {
        return switch (diff.toLowerCase()) {
            case "easy"   -> base * 1.0;
            case "hard"   -> base * 1.6;
            default       -> base * 1.25; // per defecte o dificultat mitjana (normal)
        };
    }
}
