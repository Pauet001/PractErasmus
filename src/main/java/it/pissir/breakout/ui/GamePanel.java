package it.pissir.breakout.ui;

import it.pissir.breakout.game.Ball;
import it.pissir.breakout.game.Block;
import it.pissir.breakout.game.FallingPowerUp;
import it.pissir.breakout.game.GameInstance;
import it.pissir.breakout.game.Paddle;
import it.pissir.breakout.game.PowerUpType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GamePanel extends JPanel {

    private final GameWindow gameWindow;
    private GameInstance game;
    private Timer timer;

    private boolean leftPressed = false;
    private boolean rightPressed = false;

    public GamePanel(GameWindow gameWindow) {
        this.gameWindow = gameWindow;

        setBackground(Color.BLACK);
        setFocusable(true);
        requestFocusInWindow();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = true;
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = true;
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = false;
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = false;
            }
        });

        // 60 FPS refresh rate
        timer = new Timer(16, e -> gameLoop());
    }

    public void setGame(GameInstance game) {
        this.game = game;
        this.leftPressed = false;
        this.rightPressed = false;
        timer.start();
    }

    private void gameLoop() {
        if (game == null) return;

        // Moviment de la pala contínu mentre estiguin premudes les tecles
        if (leftPressed) game.getPaddle().moveLeft();
        if (rightPressed) game.getPaddle().moveRight();

        // Demanem que es redibuixi
        repaint();

        // Comprovem si el joc ha acabat
        if (game.getStatus() == GameInstance.Status.GAME_OVER || game.getStatus() == GameInstance.Status.COMPLETED) {
            timer.stop();
            JOptionPane.showMessageDialog(this, 
                "Partida Finalitzada!\nEstat: " + game.getStatus() + "\nPunts: " + game.getScore(),
                "Breakout", JOptionPane.INFORMATION_MESSAGE);
            gameWindow.showMenu();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (game == null) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dibuixar blocs
        for (Block block : game.getBlocks()) {
            if (!block.isDestroyed()) {
                g2d.setColor(getColorForHits(block.getHitsRemaining()));
                g2d.fillRect(block.getX(), block.getY(), Block.WIDTH, Block.HEIGHT);
                g2d.setColor(Color.WHITE);
                g2d.drawRect(block.getX(), block.getY(), Block.WIDTH, Block.HEIGHT);
                
                // Si té power-up amagat, li donem un toc visual
                if (block.getPowerUp() != null) {
                    if (block.getPowerUp() == PowerUpType.EXTRA_BALL) {
                        g2d.setColor(new Color(255, 255, 255, 150));
                        g2d.fillOval(block.getX() + Block.WIDTH / 2 - 5, block.getY() + Block.HEIGHT / 2 - 5, 10, 10);
                    } else if (block.getPowerUp() == PowerUpType.DOUBLE_SCORE) {
                        g2d.setColor(new Color(255, 215, 0, 150));
                        drawStar(g2d, block.getX() + Block.WIDTH / 2, block.getY() + Block.HEIGHT / 2, 7);
                    }
                }
            }
        }

        // Dibuixar pala
        Paddle paddle = game.getPaddle();
        g2d.setColor(new Color(0, 200, 255));
        g2d.fillRect((int) (paddle.getX() - paddle.getWidth() / 2.0), paddle.getY(), paddle.getWidth(), paddle.getHeight());

        // Dibuixar pilotes
        g2d.setColor(Color.WHITE);
        for (Ball ball : game.getBalls()) {
            int r = (int) ball.getRadius();
            g2d.fillOval((int) ball.getX() - r, (int) ball.getY() - r, r * 2, r * 2);
        }

        // Dibuixar FallingPowerUps
        for (FallingPowerUp pu : game.getFallingPowerUps()) {
            if (pu.getType() == PowerUpType.EXTRA_BALL) {
                g2d.setColor(Color.WHITE);
                int r = FallingPowerUp.RADIUS;
                g2d.fillOval((int) pu.getX() - r, (int) pu.getY() - r, r * 2, r * 2);
            } else if (pu.getType() == PowerUpType.DOUBLE_SCORE) {
                g2d.setColor(new Color(255, 215, 0)); // Daurat
                drawStar(g2d, (int) pu.getX(), (int) pu.getY(), FallingPowerUp.RADIUS);
            }
        }

        // Dibuixar HUD (Puntuació i Vides)
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("Score: " + game.getScore(), 20, 30);
        g2d.drawString("Lives: " + game.getLives(), game.getBoardWidth() - 100, 30);

        // Barra de DOUBLE_SCORE
        double x2time = game.getDoubleScoreTimeRemaining();
        if (x2time > 0) {
            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString("x2 SCORE!", 150, 30);
            g2d.drawRect(250, 15, 100, 15);
            g2d.fillRect(250, 15, (int)(100 * x2time), 15);
        }
    }

    private void drawStar(Graphics2D g2d, int x, int y, int radius) {
        int[] xPoints = new int[10];
        int[] yPoints = new int[10];
        double angle = -Math.PI / 2.0; // comencem dalt
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? radius : radius / 2.0;
            xPoints[i] = x + (int) (r * Math.cos(angle));
            yPoints[i] = y + (int) (r * Math.sin(angle));
            angle += Math.PI / 5.0; // 36 graus
        }
        g2d.fillPolygon(xPoints, yPoints, 10);
    }

    private Color getColorForHits(int hits) {
        if (hits == Integer.MAX_VALUE) return Color.DARK_GRAY;
        return switch (hits) {
            case 3 -> new Color(200, 50, 50);  // Vermell (Més dur)
            case 2 -> new Color(200, 150, 50); // Taronja
            default -> new Color(50, 200, 50); // Verd (Un toc)
        };
    }
}
