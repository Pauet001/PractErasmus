package it.pissir.breakout.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Repositori de JPA per a les partides. Ens permet fer consultes a la taula 'game_results' de MySQL.
@Repository
public interface GameResultRepository extends JpaRepository<GameResult, String> {

    // Retorna totes les partides que encara no s'han sincronitzat (synced = false)
    List<GameResult> findBySyncedFalse();

    // Retorna les últimes 10 partides d'un jugador ordenades per data
    List<GameResult> findTop10ByPlayerIdOrderByFinishedAtDesc(String playerId);

    // Retorna el top 5 de partides d'una dificultat concreta ordenades per puntuació
    List<GameResult> findTop5ByDifficultyOrderByScoreDesc(String difficulty);

    // Retorna totes les partides d'una dificultat concreta
    List<GameResult> findByDifficulty(String difficulty);
}
