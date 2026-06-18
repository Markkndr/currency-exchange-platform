package com.currencyexchange.ui.controller;

import com.currencyexchange.dto.auth.AuthResponseDTO;
import com.currencyexchange.dto.auth.LoginRequestDTO;
import com.currencyexchange.exception.AuthenticationException;
import com.currencyexchange.service.AuthService;
import com.currencyexchange.ui.util.SceneNavigator;
import com.currencyexchange.ui.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class LoginController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ApplicationContext applicationContext;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        emailField.clear();
        passwordField.clear();
        loginButton.setDisable(false);
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password.");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setVisible(false);

        new Thread(() -> {
            try {
                LoginRequestDTO request = new LoginRequestDTO(email, password);
                AuthResponseDTO response = authService.login(request);
                SessionManager.setSession(response);

                Platform.runLater(() -> {
                    try {
                        SceneNavigator.navigate(emailField, "/fxml/dashboard.fxml",
                                900, 600, applicationContext, true);
                    } catch (Exception e) {
                        showError("Failed to load dashboard.");
                    }
                });
            } catch (AuthenticationException e) {
                Platform.runLater(() -> {
                    showError("Invalid email or password.");
                    loginButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("An error occurred. Please try again.");
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void navigateToRegister() {
        try {
            SceneNavigator.navigate(emailField, "/fxml/register.fxml",
                    420, 600, applicationContext);
        } catch (Exception e) {
            showError("Navigation failed.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
