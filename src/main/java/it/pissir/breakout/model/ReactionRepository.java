package it.pissir.breakout.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// Repositori JPA per a gestionar les reaccions (m'agrades, etc.) de les partides
@Repository
public interface ReactionRepository extends CrudRepository<Reaction, Long> {
    
    // Busca totes les reaccions d'una partida en concret
    List<Reaction> findByGameId(String gameId);
    
    // Mira si un usuari concret ja ha fet una reacció concreta en una partida concreta (per no repetir-les)
    boolean existsByGameIdAndUsernameAndReactionType(String gameId, String username, String reactionType);
}
