package it.pissir.breakout.ui;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class LoginPanel extends JPanel {

    private final GameWindow gameWindow;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel errorLabel;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // L'autenticació SEMPRE va al servidor principal (port 8080)
    private static final int AUTH_PORT = 8080;

    public LoginPanel(GameWindow gameWindow) {
        this.gameWindow = gameWindow;
        initUI();
    }

    private void initUI() {
        setLayout(new GridBagLayout());
        setBackground(new Color(20, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("BREAKOUT LOGIN");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(new Color(0, 255, 200));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(titleLabel, gbc);

        JLabel userLabel = new JLabel("Usuari:");
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        add(userLabel, gbc);

        usernameField = new JTextField(15);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridx = 1;
        add(usernameField, gbc);

        JLabel passLabel = new JLabel("Contrasenya:");
        passLabel.setForeground(Color.WHITE);
        passLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(passLabel, gbc);

        passwordField = new JPasswordField(15);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridx = 1;
        add(passwordField, gbc);

        errorLabel = new JLabel("");
        errorLabel.setForeground(Color.RED);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(errorLabel, gbc);

        JButton loginButton = new JButton("Entrar");
        loginButton.setFont(new Font("Arial", Font.BOLD, 18));
        loginButton.setBackground(new Color(0, 200, 100));
        loginButton.setForeground(Color.WHITE);
        loginButton.addActionListener(e -> attemptAuth("/api/auth/login"));
        gbc.gridy = 4;
        add(loginButton, gbc);

        JButton registerButton = new JButton("Registrar-se");
        registerButton.setFont(new Font("Arial", Font.BOLD, 18));
        registerButton.setBackground(new Color(100, 100, 255));
        registerButton.setForeground(Color.WHITE);
        registerButton.addActionListener(e -> attemptAuth("/api/auth/register"));
        gbc.gridy = 5;
        add(registerButton, gbc);
    }

    private void attemptAuth(String endpoint) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Omple els dos camps");
            return;
        }

        try {
            // Sempre fem auth contra el servidor principal (8080)
            Map<String, String> payload = Map.of("username", username, "password", password);
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + AUTH_PORT + endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                gameWindow.setAuthenticatedUser(username);
                gameWindow.showMenu();
            } else {
                Map<String, Object> respMap = objectMapper.readValue(response.body(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                errorLabel.setText((String) respMap.getOrDefault("error", "Error desconegut"));
            }
        } catch (Exception ex) {
            errorLabel.setText("Error de connexió al servidor (8080)");
            ex.printStackTrace();
        }
    }

    /**
     * Crida logout al servidor principal per alliberar la sessió de l'usuari.
     */
    public static void doLogout(String username) {
        if (username == null) return;
        try {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(Map.of("username", username));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + AUTH_PORT + "/api/auth/logout"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            client.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("[Auth] Logout completat per: " + username);
        } catch (Exception e) {
            System.err.println("[Auth] Error al fer logout: " + e.getMessage());
        }
    }
}
