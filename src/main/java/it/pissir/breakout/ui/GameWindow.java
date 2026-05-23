package it.pissir.breakout.ui;

import it.pissir.breakout.game.GameManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;

@Component
public class GameWindow extends JFrame implements CommandLineRunner, ApplicationListener<WebServerInitializedEvent> {

    private final GameManager gameManager;
    private int serverPort = 8080;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    private MainMenuPanel mainMenuPanel;
    private GamePanel gamePanel;
    private LoginPanel loginPanel;
    private String authenticatedUser = null;

    public void setAuthenticatedUser(String user) { this.authenticatedUser = user; }
    public String getAuthenticatedUser() { return authenticatedUser; }

    @Autowired
    public GameWindow(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public void onApplicationEvent(@NonNull WebServerInitializedEvent event) {
        this.serverPort = event.getWebServer().getPort();
    }

    public int getServerPort() {
        return this.serverPort;
    }

    @Override
    public void run(String... args) {
        // Inicialització UI a l'Event Dispatch Thread per seguretat a Swing
        SwingUtilities.invokeLater(this::initUI);
    }

    private void initUI() {
        setTitle("Breakout Game");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // Fer logout abans de tancar per alliberar la sessió
                LoginPanel.doLogout(authenticatedUser);
                System.exit(0);
            }
        });
        setResizable(false);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setPreferredSize(new Dimension(800, 600));

        // Crear panells
        loginPanel = new LoginPanel(this);
        mainMenuPanel = new MainMenuPanel(this, gameManager);
        gamePanel = new GamePanel(this);

        cardPanel.add(loginPanel, "LOGIN");
        cardPanel.add(mainMenuPanel, "MENU");
        cardPanel.add(gamePanel, "GAME");

        add(cardPanel);

        // Mostrar login per defecte
        showLogin();

        pack();
        setLocationRelativeTo(null); // Centrar a la pantalla un cop tenim la mida correcta
        setVisible(true);
    }

    public void showLogin() {
        cardLayout.show(cardPanel, "LOGIN");
        loginPanel.requestFocus();
    }

    public void showMenu() {
        // En reiniciar el menú, assegurem que les dades de l'usuari estiguin a MainMenuPanel si fos necessari (tot i que ja ho agafa per referència)
        mainMenuPanel.updateUsername();
        cardLayout.show(cardPanel, "MENU");
        // Quan tornem al menú assegurem que el game panel ja no té focus
        mainMenuPanel.requestFocus();
    }

    public void startGame(it.pissir.breakout.game.GameInstance game) {
        gamePanel.setGame(game);
        cardLayout.show(cardPanel, "GAME");
        gamePanel.requestFocusInWindow();
    }
}
