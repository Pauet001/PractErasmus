package it.pissir.breakout.controller;

import it.pissir.breakout.edge.EdgeDatabaseManager;
import it.pissir.breakout.model.GameResultRepository;
// Removed unused import: it.pissir.breakout.model.RankingEntry (not present in project)
import it.pissir.breakout.model.Reaction;
import it.pissir.breakout.model.ReactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Controlador per al rànquing i puntuacions.
// Mostra les 5 millors partides i gestiona les reaccions (emojis) dels jugadors.
@RestController
@RequestMapping("/api/ranking")
@CrossOrigin(origins = "*")
public class RankingController {

    private final GameResultRepository mysqlRepo;
    private final EdgeDatabaseManager  edgeDb;
    private final ReactionRepository reactionRepo;

    public RankingController(GameResultRepository mysqlRepo, EdgeDatabaseManager edgeDb, ReactionRepository reactionRepo) {
        this.mysqlRepo = mysqlRepo;
        this.edgeDb    = edgeDb;
        this.reactionRepo = reactionRepo;
    }

    // Retorna el Top 5 de partides filtrat per dificultat
    @GetMapping("/top5")
    public ResponseEntity<?> getTop5(@RequestParam(defaultValue = "normal") String difficulty) {
        try {
            // Busquem totes les partides d'aquesta dificultat a MySQL/H2
            List<it.pissir.breakout.model.GameResult> dbResults = mysqlRepo.findByDifficulty(difficulty);
            
            // Busquem totes les partides locals (Edge) per garantir que no ens perdem res si falla MySQL
            var edgeResults = edgeDb.getAllResults().stream()
                .filter(r -> difficulty.equalsIgnoreCase(r.getDifficulty()))
                .toList();
                
            java.util.List<it.pissir.breakout.model.GameResult> allCombined = new java.util.ArrayList<>();
            allCombined.addAll(dbResults);
            allCombined.addAll(edgeResults);
            
            // Agrupar per Jugador i quedar-nos només amb la millor puntuació
            java.util.Map<String, it.pissir.breakout.model.GameResult> bestPerPlayer = new java.util.HashMap<>();
            for (var r : allCombined) {
                String pId = r.getPlayerId();
                if (!bestPerPlayer.containsKey(pId) || r.getScore() > bestPerPlayer.get(pId).getScore()) {
                    bestPerPlayer.put(pId, r);
                }
            }
            
            // Ara n'agafem el TOP 5
            List<it.pissir.breakout.model.GameResult> top5 = bestPerPlayer.values().stream()
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                .limit(5)
                .toList();

            return ResponseEntity.ok(Map.of(
                "source",  "MySQL + Edge",
                "difficulty", difficulty,
                "count",   top5.size(),
                "ranking", top5
            ));
        } catch (Exception e) {
            var all = edgeDb.getAllResults();
            List<it.pissir.breakout.model.GameResult> localTop5 = all.stream()
                .filter(r -> difficulty.equalsIgnoreCase(r.getDifficulty()))
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                .limit(5)
                .toList();
            return ResponseEntity.ok(Map.of(
                "source",  "Edge (local)",
                "difficulty", difficulty,
                "count",   localTop5.size(),
                "ranking", localTop5
            ));
        }
    }

    // Retorna tot el rànquing local guardat als fitxers JSON d'Edge
    @GetMapping("/local")
    public ResponseEntity<?> getLocalRanking() {
        var all = edgeDb.getAllResults();
        return ResponseEntity.ok(Map.of(
            "source",  "Edge (local)",
            "count",   all.size(),
            "results", all
        ));
    }

    @GetMapping("/{gameId}/reactions")
    public ResponseEntity<?> getReactions(@PathVariable String gameId) {
        List<Reaction> reactions = reactionRepo.findByGameId(gameId);
        return ResponseEntity.ok(reactions);
    }

    @PostMapping("/{gameId}/react")
    public ResponseEntity<?> toggleReaction(@PathVariable String gameId, @RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String type = payload.get("type");

        if (reactionRepo.existsByGameIdAndUsernameAndReactionType(gameId, username, type)) {
            // Si ja havíem clicat aquest emoji, el treiem (toggle)
            List<Reaction> existing = reactionRepo.findByGameId(gameId);
            existing.stream()
                    .filter(r -> r.getUsername().equals(username) && r.getReactionType().equals(type))
                    .findFirst()
                    .ifPresent(r -> {
                        if (r != null) {
                            reactionRepo.delete(r);
                        }
                    });
            return ResponseEntity.ok(Map.of("success", true, "action", "removed"));
        } else {
            Reaction r = new Reaction(gameId, username, type);
            if (r != null) {
                reactionRepo.save(r);
            }
            return ResponseEntity.ok(Map.of("success", true, "action", "added"));
        }
    }
}
