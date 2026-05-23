package it.pissir.breakout;

import it.pissir.breakout.game.GameManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BreakoutIntegrationTest {

    @Autowired MockMvc    mvc;
    @Autowired GameManager gameManager;

    private static String createdGameId;

    // ── T1: Health check ──────────────────────────────────────────────────────
    @Test @Order(1)
    @DisplayName("T1 – GET /api/health retorna status UP")
    void healthCheck() throws Exception {
        mvc.perform(get("/api/health"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("UP"));
    }

    // ── T2: Crear partida ─────────────────────────────────────────────────────
    @Test @Order(2)
    @DisplayName("T2 – POST /api/games/start crea una partida i arrenca un fil")
    void startGame() throws Exception {
        String resp = mvc.perform(post("/api/games/start")
               .contentType(MediaType.APPLICATION_JSON_VALUE)
               .content("""
                   { "playerId": "testUser", "difficulty": "normal" }
                   """))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.gameId").exists())
           .andExpect(jsonPath("$.status").value("ACTIVE"))
           .andExpect(jsonPath("$.lives").value(3))
           .andReturn().getResponse().getContentAsString();

        // Extreu el gameId per als tests següents
        createdGameId = resp.replaceAll(".*\"gameId\":\"([A-Z0-9]+)\".*", "$1");
        assertFalse(createdGameId.isBlank());
    }

    // ── T3: Mou la pala a l'esquerra ──────────────────────────────────────────
    @Test @Order(3)
    @DisplayName("T3 – PUT /api/games/{id}/move esquerra funciona")
    void movePaddleLeft() throws Exception {
        mvc.perform(put("/api/games/" + createdGameId + "/move")
               .contentType(MediaType.APPLICATION_JSON_VALUE)
               .content("{ \"direction\": \"left\" }"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.direction").value("left"))
           .andExpect(jsonPath("$.paddleX").isNumber());
    }

    // ── T4: Mou la pala a la dreta ────────────────────────────────────────────
    @Test @Order(4)
    @DisplayName("T4 – PUT /api/games/{id}/move dreta funciona")
    void movePaddleRight() throws Exception {
        mvc.perform(put("/api/games/" + createdGameId + "/move")
               .contentType(MediaType.APPLICATION_JSON_VALUE)
               .content("{ \"direction\": \"right\" }"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.direction").value("right"));
    }

    // ── T5: Direcció invàlida ─────────────────────────────────────────────────
    @Test @Order(5)
    @DisplayName("T5 – Direcció invàlida retorna 400")
    void invalidDirection() throws Exception {
        mvc.perform(put("/api/games/" + createdGameId + "/move")
               .contentType(MediaType.APPLICATION_JSON_VALUE)
               .content("{ \"direction\": \"up\" }"))
           .andExpect(status().isBadRequest());
    }

    // ── T6: Estat de la partida ───────────────────────────────────────────────
    @Test @Order(6)
    @DisplayName("T6 – GET /api/games/{id}/status retorna posició pilota")
    void getStatus() throws Exception {
        Thread.sleep(200); // dona temps al fil de física per moure's
        mvc.perform(get("/api/games/" + createdGameId + "/status"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.balls").isArray())
           .andExpect(jsonPath("$.balls[0].x").isNumber())
           .andExpect(jsonPath("$.balls[0].y").isNumber())
           .andExpect(jsonPath("$.blocksRemaining").isNumber());
    }

    // ── T7: Pausa / reprèn ────────────────────────────────────────────────────
    @Test @Order(7)
    @DisplayName("T7 – PUT /api/games/{id}/pause alterna l'estat")
    void pauseGame() throws Exception {
        mvc.perform(put("/api/games/" + createdGameId + "/pause"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("PAUSED"));

        mvc.perform(put("/api/games/" + createdGameId + "/pause"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ── T8: Llista de partides ────────────────────────────────────────────────
    @Test @Order(8)
    @DisplayName("T8 – GET /api/games retorna la llista de partides actives")
    void listGames() throws Exception {
        mvc.perform(get("/api/games"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$").isArray())
           .andExpect(jsonPath("$[0]").exists());
    }

    // ── T9: Partida inexistent retorna 404 ────────────────────────────────────
    @Test @Order(9)
    @DisplayName("T9 – Partida inexistent retorna 404")
    void notFound() throws Exception {
        mvc.perform(get("/api/games/XXXXXXXX/status"))
           .andExpect(status().isNotFound());
    }

    // ── T10: Sync status ──────────────────────────────────────────────────────
    @Test @Order(10)
    @DisplayName("T10 – GET /api/sync/status retorna l'estat de MySQL")
    void syncStatus() throws Exception {
        mvc.perform(get("/api/sync/status"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.mode").exists());
    }

    // ── T11: Force sync ───────────────────────────────────────────────────────
    @Test @Order(11)
    @DisplayName("T11 – POST /api/sync executa la sincronització")
    void forceSync() throws Exception {
        mvc.perform(post("/api/sync"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.pending").isNumber())
           .andExpect(jsonPath("$.synced").isNumber());
    }

    // ── T12: Abandona partida ─────────────────────────────────────────────────
    @Test @Order(12)
    @DisplayName("T12 – DELETE /api/games/{id} abandona la partida")
    void deleteGame() throws Exception {
        mvc.perform(delete("/api/games/" + createdGameId))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.message").exists());
    }

    // ── T13: Múltiples partides concurrents ───────────────────────────────────
    @Test @Order(13)
    @DisplayName("T13 – Tres partides concurrents funcionen sense errors")
    void concurrentGames() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/games/start")
                   .contentType(MediaType.APPLICATION_JSON_VALUE)
                   .content("{ \"playerId\": \"player" + i + "\", \"difficulty\": \"easy\" }"))
               .andExpect(status().isCreated());
        }
        mvc.perform(get("/api/games"))
           .andExpect(jsonPath("$[2]").exists());
    }

    // ── T14: Dificultat invàlida retorna 400 ──────────────────────────────────
    @Test @Order(14)
    @DisplayName("T14 – Dificultat invàlida retorna 400")
    void invalidDifficulty() throws Exception {
        mvc.perform(post("/api/games/start")
               .contentType(MediaType.APPLICATION_JSON_VALUE)
               .content("{ \"playerId\": \"x\", \"difficulty\": \"impossible\" }"))
           .andExpect(status().isBadRequest());
    }

    // ── T15: Classificació local sempre disponible ────────────────────────────
    @Test @Order(15)
    @DisplayName("T15 – GET /api/ranking/local funciona sense MySQL")
    void localRanking() throws Exception {
        mvc.perform(get("/api/ranking/local"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.source").value("Edge (local)"));
    }
}
