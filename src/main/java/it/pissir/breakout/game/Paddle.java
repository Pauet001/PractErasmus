package it.pissir.breakout.game;

// Aquesta classe representa la pala que mou el jugador.
// Es mou de dreta a esquerra horitzontalment i no es pot sortir de la pantalla.
public class Paddle {

    private volatile double x;       // posició X del centre de la pala
    private final    int    y;       // posició Y (és fixa, a baix de tot del tauler)
    private volatile int    width;   // amplada de la pala
    private final    int    height;  // alçada de la pala
    private final    int    boardWidth; // mida total de l'amplada del tauler
    private final    int    step;    // píxels que es mou la pala amb cada moviment

    public Paddle(int boardWidth, int boardHeight, int width, int height, int step) {
        this.boardWidth   = boardWidth;
        this.width        = width;
        this.height       = height;
        this.step         = step;
        this.x            = boardWidth / 2.0;    // es col·loca al mig
        this.y            = boardHeight - height - 10; // i una mica separat de la vora inferior
    }

    // --- MOVIMENT ---

    // Mou la pala a l'esquerra sense deixar que surti del tauler
    public synchronized void moveLeft() {
        x = Math.max(width / 2.0, x - step);
    }

    // Mou la pala a la dreta sense deixar que surti del tauler
    public synchronized void moveRight() {
        x = Math.min(boardWidth - width / 2.0, x + step);
    }

    // --- COL·LISIONS ---

    // Mira si la pilota ha xocat amb la pala
    // Comprovem les coordenades de la pilota respecte la pala i si la pilota està baixant
    public synchronized boolean collidesWith(Ball ball) {
        double bx = ball.getX();
        double by = ball.getY();
        double r  = ball.getRadius();
        double left  = x - width / 2.0;
        double right = x + width / 2.0;
        return bx + r > left && bx - r < right
            && by + r > y    && by - r < y + height
            && ball.getVy() > 0;  // la pilota ha d'anar cap avall
    }

    // Aquesta funció calcula on ha xocat la pilota exactament.
    // Si xoca al mig, la pilota surt recta cap a dalt.
    // Si xoca als costats, surt rebotada amb més o menys angle segons on hagi donat.
    public synchronized double hitFactor(Ball ball) {
        double factor = (ball.getX() - x) / (width / 2.0);
        return Math.max(-0.95, Math.min(0.95, factor));
    }

    // --- GETTERS ---

    public synchronized double getX()      { return x;      }
    public int                 getY()      { return y;      }
    public synchronized int    getWidth()  { return width;  }
    public int                 getHeight() { return height; }
}
