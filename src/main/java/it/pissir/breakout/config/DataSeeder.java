package it.pissir.breakout.config;

import it.pissir.breakout.model.GameResult;
import it.pissir.breakout.model.GameResultRepository;
import it.pissir.breakout.model.User;
import it.pissir.breakout.model.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final GameResultRepository gameResultRepository;

    public DataSeeder(UserRepository userRepository, GameResultRepository gameResultRepository) {
        this.userRepository = userRepository;
        this.gameResultRepository = gameResultRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // Sempre posem loggedIn a false per a tothom en arrencar.
            // Així, si tanquem el servidor a sac, no es queden sessions enganxades.
            userRepository.findAll().forEach(u -> {
                if (u.isLoggedIn()) {
                    u.setLoggedIn(false);
                    userRepository.save(u);
                }
            });
            System.out.println("[DataSeeder] Sessions actives resetejades correctament.");

            // Si no hi ha usuaris, creem uns quants jugadors de prova
            if (userRepository.count() == 0 || !userRepository.existsById("MasterPlayer")) {
                System.out.println("[DataSeeder] Creant usuaris falsos...");
                userRepository.saveAll(new ArrayList<>(Arrays.asList(
                    new User("MasterPlayer", "1234"),
                    new User("NoobSlayer99", "1234"),
                    new User("BreakoutKing", "1234"),
                    new User("CasualGamer", "1234"),
                    new User("ProDestroyer", "1234")
                )));
            }

            // Si no hi ha partides registrades, en generem unes quantes de prova amb punts aleatoris
            if (gameResultRepository.count() == 0) {
                System.out.println("[DataSeeder] Creant partides de prova...");
                
                String[] difficulties = {"easy", "normal", "hard"};
                String[] users = {"MasterPlayer", "NoobSlayer99", "BreakoutKing", "CasualGamer", "ProDestroyer"};
                
                int[][] scoreRanges = {
                    {10, 50}, // easy
                    {20, 80}, // normal
                    {50, 150}  // hard
                };
                
                for (int d = 0; d < difficulties.length; d++) {
                    String diff = difficulties[d];
                    int minScore = scoreRanges[d][0];
                    int maxScore = scoreRanges[d][1];
                    
                    for (int i = 0; i < 5; i++) {
                        String user = users[i];
                        int score = minScore + (int)(Math.random() * (maxScore - minScore));
                        
                        GameResult result = new GameResult(
                            UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                            user,
                            score,
                            "COMPLETED",
                            diff,
                            40 + (int)(Math.random() * 20)
                        );
                        result.setSynced(true); // Les posem ja sincronitzades perquè no intenti resincronitzar-les
                        gameResultRepository.save(result);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DataSeeder] No s'ha pogut sembrar dades perquè MySQL no està disponible o l'esquema falla.");
        }
    }
}
