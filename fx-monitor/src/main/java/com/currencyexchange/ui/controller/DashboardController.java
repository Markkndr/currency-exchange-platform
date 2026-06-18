package com.currencyexchange.ui.controller;

import com.currencyexchange.dto.auth.AuthResponseDTO;
import com.currencyexchange.dto.exchange.ExchangeRateDTO;
import com.currencyexchange.entity.Wallet;
import com.currencyexchange.repository.WalletRepository;
import com.currencyexchange.service.AuthService;
import com.currencyexchange.service.ExchangeRateService;
import com.currencyexchange.ui.util.SceneNavigator;
import com.currencyexchange.ui.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class DashboardController {

    private static final String[] WATCHED_CURRENCIES = {"EUR", "USD", "JPY", "GBP", "CNY"};

    @Autowired private WalletRepository walletRepository;
    @Autowired private AuthService authService;
    @Autowired private ExchangeRateService exchangeRateService;
    @Autowired private ApplicationContext applicationContext;

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label pageTitle;
    @FXML private VBox contentArea;
    @FXML private FlowPane ratesPane;

    @FXML
    public void initialize() {
        AuthResponseDTO session = SessionManager.getSession();
        if (session != null) {
            userNameLabel.setText(session.getFullName());
            userEmailLabel.setText(session.getEmail());
        }
        showWallets();
        loadExchangeRates();
    }

    private void loadExchangeRates() {
        Label loading = new Label("Loading rates...");
        loading.getStyleClass().add("muted-text");
        ratesPane.getChildren().setAll(loading);

        Thread t = new Thread(() -> {
            try {
                ExchangeRateDTO rates = exchangeRateService.getRates("USD");
                Platform.runLater(() -> displayRates(rates));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Label err = new Label("Rates unavailable");
                    err.getStyleClass().add("muted-text");
                    ratesPane.getChildren().setAll(err);
                });
            }
        }, "rate-loader");
        t.setDaemon(true);
        t.start();
    }

    private void displayRates(ExchangeRateDTO rates) {
        ratesPane.getChildren().clear();
        for (String currency : WATCHED_CURRENCIES) {
            BigDecimal rate = rates.getRates().get(currency);
            if (rate != null) {
                ratesPane.getChildren().add(buildRateCard(currency, rate));
            }
        }
    }

    private VBox buildRateCard(String currency, BigDecimal rate) {
        VBox card = new VBox(3);
        card.getStyleClass().add("rate-card");

        Label currencyLabel = new Label(currency);
        currencyLabel.getStyleClass().add("rate-currency");

        Label rateLabel = new Label();
        rateLabel.getStyleClass().add("rate-value");

        Label descLabel = new Label();
        descLabel.getStyleClass().add("rate-description");

        if ("USD".equals(currency)) {
            rateLabel.setText("BASE");
            descLabel.setText("reference");
        } else {
            int scale = "JPY".equals(currency) ? 2 : 4;
            rateLabel.setText(rate.setScale(scale, RoundingMode.HALF_UP).toPlainString());
            descLabel.setText("per 1 USD");
        }

        card.getChildren().addAll(currencyLabel, rateLabel, descLabel);
        return card;
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
