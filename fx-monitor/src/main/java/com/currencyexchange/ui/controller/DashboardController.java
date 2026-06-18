package com.currencyexchange.ui.controller;

import com.currencyexchange.dto.auth.AuthResponseDTO;
import com.currencyexchange.entity.Wallet;
import com.currencyexchange.repository.WalletRepository;
import com.currencyexchange.service.AuthService;
import com.currencyexchange.ui.util.SceneNavigator;
import com.currencyexchange.ui.util.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DashboardController {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private ApplicationContext applicationContext;

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label pageTitle;
    @FXML private VBox contentArea;

    @FXML
    public void initialize() {
        AuthResponseDTO session = SessionManager.getSession();
        if (session != null) {
            userNameLabel.setText(session.getFullName());
            userEmailLabel.setText(session.getEmail());
        }
        showWallets();
    }

    @FXML
    private void showWallets() {
        pageTitle.setText("My Wallets");
        contentArea.getChildren().clear();

        AuthResponseDTO session = SessionManager.getSession();
        if (session == null) return;

        List<Wallet> wallets = walletRepository.findByUserId(session.getUserId());

        if (wallets.isEmpty()) {
            Label empty = new Label("No wallets yet. Wallets will appear here once created.");
            empty.getStyleClass().add("muted-text");
            contentArea.getChildren().add(empty);
            return;
        }

        FlowPane cards = new FlowPane();
        cards.setHgap(16);
        cards.setVgap(16);
        for (Wallet wallet : wallets) {
            cards.getChildren().add(buildWalletCard(wallet));
        }
        contentArea.getChildren().add(cards);
    }

    @FXML
    private void showTransactions() {
        pageTitle.setText("Transactions");
        contentArea.getChildren().clear();
        Label placeholder = new Label("Transaction history coming soon.");
        placeholder.getStyleClass().add("muted-text");
        contentArea.getChildren().add(placeholder);
    }

    @FXML
    private void showProfile() {
        pageTitle.setText("Profile");
        contentArea.getChildren().clear();

        AuthResponseDTO session = SessionManager.getSession();
        if (session == null) return;

        VBox card = new VBox(12);
        card.getStyleClass().add("profile-card");

        Label name = new Label("Name:   " + session.getFullName());
        name.getStyleClass().add("body-text");
        Label email = new Label("Email:  " + session.getEmail());
        email.getStyleClass().add("body-text");

        card.getChildren().addAll(name, email);
        contentArea.getChildren().add(card);
    }

    @FXML
    private void handleLogout() {
        try {
            AuthResponseDTO session = SessionManager.getSession();
            SessionManager.clearSession();
            if (session != null) {
                authService.logout(session.getUserId());
            }
            SceneNavigator.navigate(userNameLabel, "/fxml/login.fxml",
                    420, 520, applicationContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox buildWalletCard(Wallet wallet) {
        VBox card = new VBox(8);
        card.getStyleClass().add("wallet-card");
        card.setPrefWidth(200);
        card.setPadding(new Insets(20));

        Label currency = new Label(wallet.getCurrency());
        currency.getStyleClass().add("wallet-currency");

        Label balance = new Label(wallet.getBalance().toPlainString());
        balance.getStyleClass().add("wallet-balance");

        Label available = new Label("Available: " + wallet.getAvailableBalance().toPlainString());
        available.getStyleClass().add("wallet-available");

        card.getChildren().addAll(currency, balance, available);
        return card;
    }
}
