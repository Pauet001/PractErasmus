package it.pissir.breakout.controller;

import it.pissir.breakout.model.User;
import it.pissir.breakout.model.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

// Controlador per gestionar el registre i el login dels jugadors.
// S'encarrega de registrar nous usuaris, comprovar les contrasenyes i tancar les sessions.
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // Endpoint per registrar un usuari nou
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");

        // Validació senzilla perquè no ens enviïn camps buits
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "L'usuari i la contrasenya són obligatoris"));
        }

        // Si l'usuari ja existeix a la base de dades, donem error
        if (userRepository.existsById(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "L'usuari ja existeix"));
        }

        // Guardem el nou usuari
        User user = new User(username, password);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Usuari registrat"));
    }

    // Endpoint per iniciar sessió
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        // Validació bàsica per evitar passar null a findById (que requereix un id no-null)
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "L'usuari i la contrasenya són obligatoris"));
        }

        Optional<User> userOpt = userRepository.findById(username);
        // Si l'usuari existeix i la contrasenya coincideix
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            User user = userOpt.get();
            
            // Si ja hi ha una sessió oberta, no el deixem entrar per evitar problemes concurrents
            if (user.isLoggedIn()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "L'usuari '" + username + "' ja té una sessió activa en una altra finestra"));
            }
            
            // El marquem com a connectat
            user.setLoggedIn(true);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "username", username));
        }

        // Si falla la contrasenya o l'usuari
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credencials invàlides"));
    }

    // Endpoint per tancar la sessió
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        if (username == null) return ResponseEntity.badRequest().body(Map.of("error", "Falta username"));

        Optional<User> userOpt = userRepository.findById(username);
        userOpt.ifPresent(user -> {
            // El tornem a posar com a desconnectat
            user.setLoggedIn(false);
            userRepository.save(user);
        });
        return ResponseEntity.ok(Map.of("success", true));
    }
}
