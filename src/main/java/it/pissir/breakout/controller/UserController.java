package it.pissir.breakout.controller;

import it.pissir.breakout.model.GameResult;
import it.pissir.breakout.model.GameResultRepository;
import it.pissir.breakout.model.User;
import it.pissir.breakout.model.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

// Aquest controlador serveix per agafar i actualitzar el perfil de l'usuari
// (la descripció de la bio i la foto de perfil en format text base64).
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameResultRepository gameResultRepository;

    // Retorna les dades del perfil de l'usuari i les seves últimes 10 partides per mostrar-les a la web
    @GetMapping("/{username}")
    public ResponseEntity<?> getUserProfile(@PathVariable String username) {
        Optional<User> userOpt = userRepository.findById(username != null ? username : "");
        // Si no el trobem, donem un error 404
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        // Busquem les 10 partides més recents ordenades per data
        List<GameResult> recentGames = gameResultRepository.findTop10ByPlayerIdOrderByFinishedAtDesc(username);

        return ResponseEntity.ok(Map.of(
            "username", user.getUsername(),
            "description", user.getDescription() != null ? user.getDescription() : "",
            "base64Photo", user.getBase64Photo() != null ? user.getBase64Photo() : "",
            "recentGames", recentGames
        ));
    }

    // Per actualitzar la descripció o la foto del perfil
    @PostMapping("/{username}/update")
    public ResponseEntity<?> updateProfile(@PathVariable String username, @RequestBody Map<String, String> payload) {
        Optional<User> userOpt = userRepository.findById(username != null ? username : "");
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        // Només actualitzem si el payload porta les claus, així no es borren si estan a mitges
        if (payload.containsKey("description")) {
            user.setDescription(payload.get("description"));
        }
        if (payload.containsKey("base64Photo")) {
            user.setBase64Photo(payload.get("base64Photo"));
        }

        // Ensure user is non-null for repositories that enforce @NonNull parameters
        Objects.requireNonNull(user, "user");
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
