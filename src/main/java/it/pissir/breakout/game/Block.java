package it.pissir.breakout.game;

import com.fasterxml.jackson.annotation.JsonIgnore;

// Aquesta classe representa un bloc del tauler del joc Breakout.
// Cada bloc té una posició (coordenades x, y), un nombre de tocs per ser destruït (hitsRemaining)
// i, opcionalment, un power-up que cau en trencar-se.
public class Block {

    // Posició en píxels (cantonada superior esquerra del bloc)
    private final int x;
    private final int y;

    // Dimensions fixes per a tots els blocs
    public static final int WIDTH  = 70;
    public static final int HEIGHT = 25;

    // Quants tocs li queden al bloc per trencar-se (1=normal, 2=dur, 3=indestructible/molt dur, etc.)
    private int hitsRemaining;

    // El tipus de power-up que deixa anar quan es trenca (pot ser null si no porta res)
    private final PowerUpType powerUp;

    // Quants punts dóna quan el destruïm del tot
    private final int points;

    public Block(int x, int y, int hitsRemaining, PowerUpType powerUp) {
        this.x             = x;
        this.y             = y;
        this.hitsRemaining = hitsRemaining;
        this.powerUp       = powerUp;
        // Com més fort és el bloc, més punts dóna
        this.points = hitsRemaining * 10;
    }

    // --- COL·LISIONS ---

    // Comprova si la pilota xoca amb aquest bloc concret
    public boolean collidesWith(Ball ball) {
        if (hitsRemaining <= 0) return false; // Si ja està destruït, no hi ha col·lisió
        double bx = ball.getX();
        double by = ball.getY();
        double r  = ball.getRadius();
        return bx + r > x && bx - r < x + WIDTH
            && by + r > y && by - r < y + HEIGHT;
    }

    // Li restem un toc al bloc quan la pilota hi xoca.
    // Retorna 'true' si s'acaba de trencar del tot.
    public boolean hit() {
        if (hitsRemaining == Integer.MAX_VALUE) return false; // Per si posem algun bloc indestructible
        hitsRemaining--;
        return hitsRemaining <= 0;
    }

    // --- GETTERS ---

    public int          getX()             { return x;             }
    public int          getY()             { return y;             }
    public int          getHitsRemaining() { return hitsRemaining; }
    public PowerUpType  getPowerUp()       { return powerUp;       }
    public int          getPoints()        { return points;        }

    @JsonIgnore
    public boolean isDestroyed()           { return hitsRemaining <= 0; }
}
