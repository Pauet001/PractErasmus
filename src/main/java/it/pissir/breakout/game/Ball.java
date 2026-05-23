package it.pissir.breakout.game;

// Aquesta classe representa una pilota de joc amb la seva posició i velocitat.
// Hi pot haver més d'una pilota alhora gràcies al power-up de pilota extra (EXTRA_BALL).
// Fem servir variables 'volatile' perquè el BallThread (que calcula la física) i els
// controladors REST de Spring puguin llegir i escriure les posicions sense problemes de sincronització.
public class Ball {

    private static final double RADIUS = 8.0; // El radi de la pilota, que és fix

    private volatile double x;
    private volatile double y;
    private volatile double vx;   // velocitat en l'eix X (píxels per cicle)
    private volatile double vy;   // velocitat en l'eix Y (píxels per cicle)
    private final    int    id;   // identificador de la pilota per si n'hi ha més d'una

    public Ball(int id, double x, double y, double vx, double vy) {
        this.id = id;
        this.x  = x;
        this.y  = y;
        this.vx = vx;
        this.vy = vy;
    }

    // --- FÍSICA I MOVIMENT ---

    // Mou la pilota sumant-li la velocitat actual a la seva posició
    public void move() {
        x += vx;
        y += vy;
    }

    // Rebota contra les parets de l'esquerra o dreta (gira la direcció X)
    public void bounceX() { vx = -vx; }

    // Rebota contra el sostre o la pala (gira la direcció Y)
    public void bounceY() { vy = -vy; }

    // Per modificar la velocitat de la pilota (per exemple, per fer-la més lenta o més ràpida)
    public void scaleSpeed(double factor) {
        vx *= factor;
        vy *= factor;
    }

    // --- GETTERS I SETTERS ---

    public int    getId()     { return id; }
    public double getX()      { return x;  }
    public double getY()      { return y;  }
    public double getVx()     { return vx; }
    public double getVy()     { return vy; }
    public double getRadius() { return RADIUS; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setVx(double vx) { this.vx = vx; }
    public void setVy(double vy) { this.vy = vy; }
}
