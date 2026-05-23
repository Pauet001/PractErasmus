package it.pissir.breakout.edge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pissir.breakout.game.GameInstance;
import it.pissir.breakout.model.GameResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// Aquest gestor serveix per desar les dades de les partides localment (l'Edge).
// Si la base de dades MySQL cau o no està disponible, guardem els resultats en un fitxer
// JSON local que es diu 'edge_data.json'. Després, el SyncService anirà llegint d'aquí
// per sincronitzar les partides quan la base de dades torni a estar activa.
// Fem servir un ReentrantReadWriteLock per protegir el fitxer de problemes d'accés concurrent
// per si es llegeix i s'escriu al mateix temps.
@Service
public class EdgeDatabaseManager {

    @Value("${edge.storage.file}")
    private String storageFile;

    private final ObjectMapper mapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public EdgeDatabaseManager() {
        this.mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // --- ESCRIPTURA ---

    // Agafa una partida de la memòria de joc i la converteix en un GameResult per guardar-la al JSON
    public void saveResult(GameInstance game) {
        long blocksTotal     = (long) game.getBlocks().size();
        long blocksRemaining = game.getBlocksRemaining();
        long blocksDestroyed = blocksTotal - blocksRemaining;

        GameResult result = new GameResult(
            game.getGameId(),
            game.getPlayerId(),
            game.getScore(),
            game.getStatus().name(),
            game.getDifficulty(),
            blocksDestroyed
        );
        saveResult(result);
    }

    // Guarda el GameResult directament al fitxer JSON. Això també ho crida el SyncService
    // quan marca una partida com a ja sincronitzada (synced = true) per actualitzar-la en el JSON.
    public void saveResult(GameResult result) {
        lock.writeLock().lock();
        try {
            File currentDir = new File(".");
            File[] edgeFiles = currentDir.listFiles((dir, name) -> name.startsWith("edge_data") && name.endsWith(".json"));
            
            boolean found = false;
            if (edgeFiles != null) {
                for (File file : edgeFiles) {
                    List<GameResult> fileResults = new ArrayList<>();
                    try {
                        if (file.exists()) fileResults = mapper.readValue(file, new TypeReference<List<GameResult>>() {});
                    } catch (Exception e) {}
                    
                    boolean removed = fileResults.removeIf(r -> r.getGameId().equals(result.getGameId()));
                    if (removed) {
                        fileResults.add(result);
                        mapper.writerWithDefaultPrettyPrinter().writeValue(file, fileResults);
                        found = true;
                        break;
                    }
                }
            }
            
            if (!found) {
                File myFile = new File(storageFile);
                List<GameResult> myResults = new ArrayList<>();
                try {
                    if (myFile.exists()) myResults = mapper.readValue(myFile, new TypeReference<List<GameResult>>() {});
                } catch (Exception e) {}
                myResults.add(result);
                mapper.writerWithDefaultPrettyPrinter().writeValue(myFile, myResults);
            }
            System.out.printf("[EdgeDB] Resultat guardat: %s | Jugador: %s | Punts: %d%n",
                result.getGameId(), result.getPlayerId(), result.getScore());
        } catch (Exception e) {
            System.err.println("[EdgeDB] Error guardant resultat: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    // --- LECTURA ---

    // Retorna només els resultats del fitxer JSON que tenen 'synced = false'
    public List<GameResult> getPendingResults() {
        lock.readLock().lock();
        try {
            return readAll().stream()
                            .filter(r -> !r.isSynced())
                            .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    // Retorna absolutament tots els resultats que hi ha guardats al fitxer local
    public List<GameResult> getAllResults() {
        lock.readLock().lock();
        try {
            return readAll();
        } finally {
            lock.readLock().unlock();
        }
    }

    // --- AJUDANTS PRIVATS ---

    // Llegeix tots els fitxers edge_data que trobi al directori actual
    private List<GameResult> readAll() {
        List<GameResult> allResults = new ArrayList<>();
        File currentDir = new File(".");
        File[] edgeFiles = currentDir.listFiles((dir, name) -> name.startsWith("edge_data") && name.endsWith(".json"));
        
        if (edgeFiles != null) {
            for (File file : edgeFiles) {
                try {
                    List<GameResult> fileResults = mapper.readValue(file, new TypeReference<List<GameResult>>() {});
                    allResults.addAll(fileResults);
                } catch (Exception e) {
                    System.err.println("[EdgeDB] Error llegint fitxer local: " + file.getName() + " - " + e.getMessage());
                }
            }
        }
        return allResults;
    }
}
