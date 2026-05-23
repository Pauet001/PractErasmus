package it.pissir.breakout.ui;

import it.pissir.breakout.game.GameInstance;
import it.pissir.breakout.game.GameManager;

import javax.swing.*;
import java.awt.*;

public class MainMenuPanel extends JPanel {

    private final GameWindow gameWindow;
    private final GameManager gameManager;

    private JLabel nameLabel;
    private JComboBox<String> difficultyCombo;

    public MainMenuPanel(GameWindow gameWindow, GameManager gameManager) {
        this.gameWindow = gameWindow;
        this.gameManager = gameManager;

        initUI();
    }

    public void updateUsername() {
        if (nameLabel != null) {
            String user = gameWindow.getAuthenticatedUser();
            nameLabel.setText("Benvingut, " + (user != null ? user : "Convidat") + "!");
        }
    }

    private void initUI() {
        setLayout(new GridBagLayout());
        setBackground(new Color(20, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel titleLabel = new JLabel("BREAKOUT");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
        titleLabel.setForeground(new Color(0, 255, 200));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);

        nameLabel = new JLabel("Benvingut!");
        nameLabel.setForeground(new Color(255, 215, 0));
        nameLabel.setFont(new Font("Arial", Font.BOLD, 20));
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(nameLabel, gbc);


        JLabel diffLabel = new JLabel("Dificultat:");
        diffLabel.setForeground(Color.WHITE);
        diffLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        add(diffLabel, gbc);

        difficultyCombo = new JComboBox<>(new String[]{"EASY", "NORMAL", "HARD"});
        difficultyCombo.setSelectedItem("NORMAL");
        difficultyCombo.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        add(difficultyCombo, gbc);

        JButton playButton = new JButton("JUGAR");
        playButton.setFont(new Font("Arial", Font.BOLD, 24));
        playButton.setBackground(new Color(0, 200, 100));
        playButton.setForeground(Color.WHITE);
        playButton.setFocusPainted(false);
        playButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playButton.addActionListener(e -> startGame());

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(30, 10, 10, 10);
        add(playButton, gbc);
    }

    private void startGame() {
        String playerName = gameWindow.getAuthenticatedUser();
        if (playerName == null) playerName = "Guest";
        String difficulty = (String) difficultyCombo.getSelectedItem();

        GameInstance newGame = gameManager.createGame(playerName, difficulty);
        gameWindow.startGame(newGame);
    }
}
