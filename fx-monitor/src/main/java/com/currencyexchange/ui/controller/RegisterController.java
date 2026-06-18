package com.currencyexchange.ui.controller;

import com.currencyexchange.dto.auth.AuthResponseDTO;
import com.currencyexchange.dto.auth.RegisterRequestDTO;
import com.currencyexchange.exception.UserAlreadyExistsException;
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
public class RegisterController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ApplicationContext applicationContext;

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField phoneField;
    @FXML private TextField countryField;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleRegister() {
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String phone = phoneField.getText().trim();
        String country = countryField.getText().trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Full name, email and password are required.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        registerButton.setDisable(true);
        errorLabel.setVisible(false);

        new Thread(() -> {
            try {
                RegisterRequestDTO request = new RegisterRequestDTO(
                        email, password, fullName,
                        phone.isEmpty() ? null : phone,
                        country.isEmpty() ? null : country
                );
                AuthResponseDTO response = authService.register(request);
                SessionManager.setSession(response);

                Platform.runLater(() -> {
                    try {
                        SceneNavigator.navigate(emailField, "/fxml/dashboard.fxml",
                                900, 600, applicationContext);
                    } catch (Exception e) {
                        showError("Failed to load dashboard.");
                    }
                });
            } catch (UserAlreadyExistsException e) {
                Platform.runLater(() -> {
                    showError("An account with this email already exists.");
                    registerButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Registration failed. Please try again.");
                    registerButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void navigateToLogin() {
        try {
            SceneNavigator.navigate(emailField, "/fxml/login.fxml",
                    420, 520, applicationContext);
        } catch (Exception e) {
            showError("Navigation failed.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
