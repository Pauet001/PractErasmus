package it.pissir.breakout.game;

// Aquesta classe representa un power-up que cau pel tauler.
// Si el jugador l'agafa amb la pala, rep un benefici (com punts dobles o una pilota extra).
public class FallingPowerUp {
    private double x;
    private double y;
    private final double vy = 3.0; // Velocitat fixa a la qual cau cap avall
    private final PowerUpType type;
    public static final int RADIUS = 10; // El radi del power-up en pantalla

    public FallingPowerUp(double x, double y, PowerUpType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    // El fem moure cap avall sumant-li la velocitat a la coordenada Y
    public void move() {
        y += vy;
    }

    // Comprova si el power-up que cau toca la pala del jugador
    public boolean collidesWith(Paddle paddle) {
        double pX = paddle.getX() - paddle.getWidth() / 2.0;
        double pY = paddle.getY();
        double pW = paddle.getWidth();
        double pH = paddle.getHeight();

        // Mirem si les coordenades del cercle del power-up coincideixen amb el rectangle de la pala
        return (x + RADIUS > pX && x - RADIUS < pX + pW &&
                y + RADIUS > pY && y - RADIUS < pY + pH);
    }

    // Getters normals
    public double getX() { return x; }
    public double getY() { return y; }
    public PowerUpType getType() { return type; }
}
