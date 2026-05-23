package it.pissir.breakout.game;

import it.pissir.breakout.edge.EdgeDatabaseManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servei central que gestiona totes les partides actives.
 *
 * - Crea instàncies de joc i arrenca el seu {@link BallThread}.
 * - Manté un mapa thread-safe de partides per ID.
 * - Quan una partida acaba, delega el guardado a {@link EdgeDatabaseManager}.
 */
@Service
public class GameManager {

    // ── configuració des de application.properties ───────────────────────────
    @Value("${game.board.width}")        private int boardWidth;
    @Value("${game.board.height}")       private int boardHeight;
    @Value("${game.ball.speed.initial}") private double initialSpeed;
    @Value("${game.paddle.width}")       private int paddleWidth;
    @Value("${game.paddle.height}")      private int paddleHeight;
    @Value("${game.paddle.step}")        private int paddleStep;
    @Value("${game.tick.ms}")            private long tickMs;
    @Value("${game.blocks.cols}")        private int blockCols;
    @Value("${game.blocks.rows}")        private int blockRows;

    /** Partides en curs (gameId → GameInstance). */
    private final Map<String, GameInstance> activeGames = new ConcurrentHashMap<>();

    private final EdgeDatabaseManager edgeDb;

    public GameManager(EdgeDatabaseManager edgeDb) {
        this.edgeDb = edgeDb;
    }

    // ── operacions públiques ──────────────────────────────────────────────────

    /**
     * Crea una nova partida, arrenca el seu fil de física i retorna la instància.
     */
    public GameInstance createGame(String playerId, String difficulty) {
        String gameId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        GameInstance game = new GameInstance(
            gameId, playerId, difficulty,
            boardWidth, boardHeight,
            paddleWidth, paddleHeight, paddleStep,
            blockCols, blockRows, initialSpeed
        );

        activeGames.put(gameId, game);

        // Arranca el fil de física per aquesta partida
        BallThread thread = new BallThread(game, tickMs, this::onGameFinished);
        thread.start();

        System.out.printf("[GameManager] Partida %s creada per %s (dificultat: %s) → Thread: %s%n",
            gameId, playerId, difficulty, thread.getName());

        return game;
    }

    /** Retorna una partida per ID, o null si no existeix. */
    public GameInstance getGame(String gameId) {
        return activeGames.get(gameId);
    }

    /** Totes les partides actives. */
    public Collection<GameInstance> getAllGames() {
        return activeGames.values();
    }

    /** Elimina una partida del mapa actiu. */
    public boolean deleteGame(String gameId) {
        GameInstance g = activeGames.remove(gameId);
        if (g != null) g.setStatus(GameInstance.Status.GAME_OVER);
        return g != null;
    }

    /** Pausa o reprèn una partida. */
    public GameInstance togglePause(String gameId) {
        GameInstance g = activeGames.get(gameId);
        if (g == null) return null;
        if (g.getStatus() == GameInstance.Status.ACTIVE) {
            g.setStatus(GameInstance.Status.PAUSED);
        } else if (g.getStatus() == GameInstance.Status.PAUSED) {
            g.setStatus(GameInstance.Status.ACTIVE);
        }
        return g;
    }

    // ── callback de fi de partida ─────────────────────────────────────────────

    /**
     * Cridat pel {@link BallThread} quan la partida acaba.
     * Guarda el resultat localment (Edge) i elimina la partida del mapa actiu.
     */
    private void onGameFinished(GameInstance game) {
        System.out.printf("[GameManager] Partida %s finalitzada → Punts: %d | Estat: %s%n",
            game.getGameId(), game.getScore(), game.getStatus());

        // Guarda el resultat al Edge
        edgeDb.saveResult(game);

        // Elimina del mapa de partides actives
        activeGames.remove(game.getGameId());
    }
}
