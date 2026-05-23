package it.pissir.breakout.game;

import java.util.ArrayList;
import java.util.List;

// Aquest és el fil de execució que gestiona tota la física de les pilotes en segon pla.
// Es crea un fil per cada partida que està activa. Cada X mil·lisegons (tickMs) fa el següent:
//   1. Mou totes les pilotes que estiguin en joc.
//   2. Comprova si la pilota xoca contra les parets, la pala o algun bloc.
//   3. Si hi ha xoc amb un bloc, el destrueix o li treu vida, i pot fer caure un power-up.
//   4. Si una pilota cau a baix de tot del tauler, la treu i resta una vida.
// Quan el joc s'acaba (perquè has guanyat o has perdut), aquest fil s'atura tot sol.
public class BallThread extends Thread {

    private final GameInstance game;
    private final int          boardWidth;
    private final int          boardHeight;
    private final long         tickMs;

    // Aquest callback serveix per avisar que el joc ha acabat i desar la partida a la base de dades
    private final GameOverCallback onGameOver;

    @FunctionalInterface
    public interface GameOverCallback {
        void onGameOver(GameInstance game);
    }

    public BallThread(GameInstance game, long tickMs, GameOverCallback onGameOver) {
        super("BallThread-" + game.getGameId());
        setDaemon(true);   // Fil daemon perquè no bloquegi la sortida de l'aplicació si es tanca Spring
        this.game        = game;
        this.boardWidth  = game.getBoardWidth();
        this.boardHeight = game.getBoardHeight();
        this.tickMs      = tickMs;
        this.onGameOver  = onGameOver;
    }

    @Override
    public void run() {
        // Mentre la partida estigui activa o pausada, seguim fent cicles
        while (game.getStatus() == GameInstance.Status.ACTIVE
            || game.getStatus() == GameInstance.Status.PAUSED) {

            // Si està en pausa, ens esperem sense fer res per no consumir CPU a lo boig
            if (game.getStatus() == GameInstance.Status.PAUSED) {
                sleepFor(tickMs);
                continue;
            }

            tick();
            game.checkPowerUpExpiry(); // Mirem si algun power-up ja ha caducat
            sleepFor(tickMs);
        }

        // Si sortim del bucle és que s'ha acabat, cridem el callback per guardar la partida
        if (onGameOver != null) {
            onGameOver.onGameOver(game);
        }
    }

    // --- CICLE FÍSIC DEL TICK ---

    private void tick() {
        // Guardem les pilotes que cauen per borrar-les de cop i no tenir errors de concurrència
        List<Ball> toRemove = new ArrayList<>();

        for (Ball ball : game.getBalls()) {
            ball.move();
            resolveWallCollisions(ball);
            resolvePaddleCollision(ball);
            boolean lost = resolveBlockCollisions(ball);

            // Si la pilota ha caigut per sota del límit de la pantalla, la marquem per esborrar
            if (!lost && ball.getY() - ball.getRadius() > boardHeight) {
                toRemove.add(ball);
            }
        }

        // Esborrem les pilotes perdudes
        for (Ball ball : toRemove) {
            game.ballLost(ball);
        }

        // Fem moure els power-ups que estiguin caient per la pantalla
        List<FallingPowerUp> toRemovePowerUps = new ArrayList<>();
        for (FallingPowerUp pu : game.getFallingPowerUps()) {
            pu.move();
            // Si xoca amb la pala del jugador, l'activem
            if (pu.collidesWith(game.getPaddle())) {
                game.activatePowerUp(pu.getType());
                toRemovePowerUps.add(pu);
            } else if (pu.getY() - FallingPowerUp.RADIUS > boardHeight) {
                // Si surt de la pantalla per sota, el borrem també
                toRemovePowerUps.add(pu);
            }
        }
        game.getFallingPowerUps().removeAll(toRemovePowerUps);
    }

    // Comprova i resol rebots contra les parets de l'esquerra, dreta i el sostre
    private void resolveWallCollisions(Ball ball) {
        double r = ball.getRadius();

        // Esquerra / dreta
        if (ball.getX() - r < 0) {
            ball.setX(r);
            ball.bounceX();
        } else if (ball.getX() + r > boardWidth) {
            ball.setX(boardWidth - r);
            ball.bounceX();
        }

        // Sostre (paret superior)
        if (ball.getY() - r < 0) {
            ball.setY(r);
            ball.bounceY();
        }
    }

    // Comprova si rebota contra la pala i calcula la nova direcció segons on hagi tocat
    private void resolvePaddleCollision(Ball ball) {
        Paddle paddle = game.getPaddle();
        if (!paddle.collidesWith(ball)) return;

        // Mirem a quin lloc ha xocat exactament per calcular el canvi d'angle (de -1 a 1)
        double factor = paddle.hitFactor(ball);
        double speed  = Math.hypot(ball.getVx(), ball.getVy());

        // Recalculem les velocitats VX i VY per donar-li l'efecte de rebot
        ball.setVx(speed * factor);
        ball.setVy(-Math.abs(speed * Math.sqrt(1 - factor * factor)));

        // Una petita empenta cap a dalt perquè no es quedi enganxada dins de la pala
        ball.setY(paddle.getY() - ball.getRadius() - 1);
    }

    // Comprova si la pilota toca algun dels blocs
    private boolean resolveBlockCollisions(Ball ball) {
        for (Block block : game.getBlocks()) {
            if (block.isDestroyed()) continue;
            if (!block.collidesWith(ball)) continue;

            // Determinem si el xoc és horitzontal o vertical per fer el rebot correcte
            double overlapLeft   = (block.getX() + Block.WIDTH)  - (ball.getX() - ball.getRadius());
            double overlapRight  = (ball.getX() + ball.getRadius()) - block.getX();
            double overlapTop    = (block.getY() + Block.HEIGHT) - (ball.getY() - ball.getRadius());
            double overlapBottom = (ball.getY() + ball.getRadius()) - block.getY();

            double minH = Math.min(overlapLeft, overlapRight);
            double minV = Math.min(overlapTop,  overlapBottom);

            if (minH < minV) ball.bounceX();
            else             ball.bounceY();

            // Sumem punts: 10 punts només per tocar-lo
            game.addScore(10);
            boolean destroyed = block.hit(); // Li restem vida al bloc
            if (destroyed) {
                // Si el destruïm del tot, donem els punts extres del bloc
                game.addScore(block.getPoints());
                // I si portava un power-up a dins, el fem caure
                if (block.getPowerUp() != null) {
                    game.dropPowerUp(block.getX() + Block.WIDTH / 2.0, block.getY() + Block.HEIGHT / 2.0, block.getPowerUp());
                }
            }
            // Només un xoc per cicle per evitar rebots estranys en un mateix moment
            break;
        }
        return false;
    }

    // Helper per aturar el fil uns mil·lisegons sense haver d'escriure el try-catch cada vegada
    private void sleepFor(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
