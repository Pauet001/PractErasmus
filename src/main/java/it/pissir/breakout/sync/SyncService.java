package it.pissir.breakout.sync;

import it.pissir.breakout.edge.EdgeDatabaseManager;
import it.pissir.breakout.model.GameResult;
import it.pissir.breakout.model.GameResultRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

// Aquest servei és el que s'encarrega d'agafar les partides locals (del JSON) i pujar-les a MySQL.
// S'executa en segon pla cada cert temps.
// Per cada partida que encara no estigui sincronitzada (synced = false):
//   1. Intenta desar-la a MySQL mitjançant el repositori.
//   2. Si funciona, la marca com a sincronitzada (synced = true) al fitxer JSON.
//   3. Si MySQL torna a fallar, ho deixem estar i ho tornarem a intentar al següent cicle.
// També es pot forçar manualment si cridem a la ruta de POST /api/sync.
@Service
public class SyncService {

    private final EdgeDatabaseManager  edgeDb;
    private final GameResultRepository mysqlRepo;

    private volatile boolean mysqlAvailable = false;

    public SyncService(EdgeDatabaseManager edgeDb, GameResultRepository mysqlRepo) {
        this.edgeDb    = edgeDb;
        this.mysqlRepo = mysqlRepo;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            mysqlRepo.count();
            mysqlAvailable = true;
        } catch (Exception e) {
            mysqlAvailable = false;
        }
    }

    // --- EXECUCIÓ EN SEGON PLA ---

    // Execució automàtica programada. El delay inicial de 10 segons és per donar
    // temps a que arrenqui bé tot el sistema de Spring abans de fer res.
    @Scheduled(fixedDelayString = "${edge.sync.interval.ms}", initialDelay = 10_000)
    public void autoSync() {
        sync();
    }

    // --- SINCRONITZACIÓ ---

    // Funció que fa la sincronització de veritat, tant per a la tasca automàtica
    // com si es crida a mà des de l'API.
    public SyncReport sync() {
        List<GameResult> pending = edgeDb.getPendingResults();
        int synced  = 0;
        int failed  = 0;

        System.out.printf("[SyncService] Iniciant sync → %d registres pendents%n", pending.size());

        for (GameResult result : pending) {
            if (result == null) continue;
            try {
                mysqlRepo.save(result);
                result.setSynced(true);
                edgeDb.saveResult(result);   // actualitza el fitxer local posant synced=true
                synced++;
                mysqlAvailable = true;
                System.out.printf("[SyncService] ✓ Sincronitzat: %s (jugador: %s, punts: %d)%n",
                    result.getGameId(), result.getPlayerId(), result.getScore());
            } catch (Exception e) {
                mysqlAvailable = false;
                failed++;
                System.err.printf("[SyncService] ✗ Error sync %s: %s%n",
                    result.getGameId(), e.getMessage());
            }
        }

        SyncReport report = new SyncReport(pending.size(), synced, failed, mysqlAvailable);
        System.out.printf("[SyncService] Sync completat → %d ok, %d fallits%n", synced, failed);
        return report;
    }

    public boolean isMysqlAvailable() { return mysqlAvailable; }

    // --- PETIT DTO PER A LA RESPOSTA DEL SYNC ---
    public record SyncReport(int pending, int synced, int failed, boolean mysqlOnline) {}
}
