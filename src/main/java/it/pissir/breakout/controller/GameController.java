package it.pissir.breakout.controller;

import it.pissir.breakout.game.GameInstance;
import it.pissir.breakout.game.GameManager;
import it.pissir.breakout.sync.SyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

// Aquest és el controlador principal de l'API de Breakout.
// Defineix totes les rutes que es poden provar amb Postman per jugar o veure l'estat.
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")   // permet peticions des del frontend web
public class GameController {

    private final GameManager gameManager;
    private final SyncService syncService;

    public GameController(GameManager gameManager, SyncService syncService) {
        this.gameManager = gameManager;
        this.syncService = syncService;
    }

    // ── A. Crear una nova partida ─────────────────────────────────────────────
    // Per començar una partida nova (enviem playerId i dificultat)
    @PostMapping("/games/start")
    public ResponseEntity<?> startGame(@RequestBody Map<String, String> body) {
        String playerId   = body.getOrDefault("playerId",   "anonymous");
        String difficulty = body.getOrDefault("difficulty", "normal");

        if (!difficulty.matches("easy|normal|hard")) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Dificultat invàlida. Usa: easy, normal o hard"));
        }

        GameInstance game = gameManager.createGame(playerId, difficulty);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(buildStatusResponse(game));
    }



    // ── Llista totes les partides actives ─────────────────────────────────────
    // Retorna totes les partides que s'estan jugant en aquest ordinador
    @GetMapping("/games")
    public ResponseEntity<?> listGames() {
        // Obtenir partides locals
        java.util.List<Map<String, Object>> allGames = new java.util.ArrayList<>(
            gameManager.getAllGames().stream().map(this::buildStatusResponse).toList()
        );

        
        return ResponseEntity.ok(allGames);
    }



    // ── B. Mou la pala ────────────────────────────────────────────────────────
    // Mètode per moure la pala (left o right)
    @PutMapping("/games/{gameId}/move")
    public ResponseEntity<?> movePaddle(@PathVariable String gameId,
                                        @RequestBody  Map<String, String> body) {
        GameInstance game = gameManager.getGame(gameId);
        if (game == null) return notFound(gameId);

        if (game.getStatus() == GameInstance.Status.GAME_OVER ||
            game.getStatus() == GameInstance.Status.COMPLETED) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "La partida ja ha acabat",
                             "status", game.getStatus()));
        }

        String direction = body.getOrDefault("direction", "").toLowerCase();
        switch (direction) {
            case "left"  -> game.getPaddle().moveLeft();
            case "right" -> game.getPaddle().moveRight();
            default -> { return ResponseEntity.badRequest()
                .body(Map.of("error", "Direcció invàlida. Usa: left o right")); }
        }

        return ResponseEntity.ok(Map.of(
            "gameId",       gameId,
            "paddleX",      game.getPaddle().getX(),
            "paddleWidth",  game.getPaddle().getWidth(),
            "direction",    direction
        ));
    }

    // ── C. Consultar estat actual ──────────────────────────────────────────────
    // Ruta per aconseguir tota la info actual de la partida (pilota, vides, blocs...)
    @GetMapping("/games/{gameId}/status")
    public ResponseEntity<?> getStatus(@PathVariable String gameId) {
        GameInstance game = gameManager.getGame(gameId);
        if (game == null) return notFound(gameId);
        return ResponseEntity.ok(buildStatusResponse(game));
    }

    // ── Pausa / reprèn ────────────────────────────────────────────────────────
    // Per pausar o despausar la partida
    @PutMapping("/games/{gameId}/pause")
    public ResponseEntity<?> togglePause(@PathVariable String gameId) {
        GameInstance game = gameManager.togglePause(gameId);
        if (game == null) return notFound(gameId);
        return ResponseEntity.ok(Map.of(
            "gameId", gameId,
            "status", game.getStatus()
        ));
    }

    // ── Abandona una partida ──────────────────────────────────────────────────
    // Per si un jugador vol abandonar i parar el fil del joc
    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<?> deleteGame(@PathVariable String gameId) {
        boolean removed = gameManager.deleteGame(gameId);
        if (!removed) return notFound(gameId);
        return ResponseEntity.ok(Map.of(
            "gameId",  gameId,
            "message", "Partida abandonada i guardada localment"
        ));
    }

    // ── D. Forçar sincronització ───────────────────────────────────────────────
    // Executa la sincronització a MySQL manualment
    @PostMapping("/sync")
    public ResponseEntity<?> forceSync() {
        SyncService.SyncReport report = syncService.sync();
        return ResponseEntity.ok(Map.of(
            "pending",      report.pending(),
            "synced",       report.synced(),
            "failed",       report.failed(),
            "mysqlOnline",  report.mysqlOnline(),
            "message",      report.synced() + " registres sincronitzats a MySQL"
        ));
    }

    // ── Estat de la connectivitat ─────────────────────────────────────────────
    // Diu si MySQL està connectat o si estem en mode local
    @GetMapping("/sync/status")
    public ResponseEntity<?> syncStatus() {
        return ResponseEntity.ok(Map.of(
            "mysqlOnline", syncService.isMysqlAvailable(),
            "mode",        syncService.isMysqlAvailable() ? "ONLINE" : "EDGE (local)"
        ));
    }

    // ── Health check ──────────────────────────────────────────────────────────
    // Ruta simple per comprovar si el servidor està actiu
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        int localCount = gameManager.getAllGames().size();
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "activeGames", localCount,
            "mysqlOnline", syncService.isMysqlAvailable()
        ));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> buildStatusResponse(GameInstance game) {
        return Map.of(
            "gameId",          game.getGameId(),
            "playerId",        game.getPlayerId(),
            "difficulty",      game.getDifficulty(),
            "status",          game.getStatus(),
            "score",           game.getScore(),
            "lives",           game.getLives(),
            "blocksRemaining", game.getBlocksRemaining(),
            "balls",           game.getBalls(),
            "paddle",          Map.of(
                "x",     game.getPaddle().getX(),
                "y",     game.getPaddle().getY(),
                "width", game.getPaddle().getWidth()
            )
        );
    }

    private ResponseEntity<?> notFound(String gameId) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Partida no trobada: " + gameId));
    }


}
