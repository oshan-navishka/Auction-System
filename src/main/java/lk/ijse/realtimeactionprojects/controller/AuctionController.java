package lk.ijse.realtimeactionprojects.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import lk.ijse.realtimeactionprojects.util.AuctionConnection;

import java.io.IOException;

public class AuctionController {

    @FXML
    private Label lblWelcome;

    @FXML
    private Label lblItemName;

    @FXML
    private Label lblStartingPrice;

    @FXML
    private Label lblHighestBid;

    @FXML
    private Label lblHighestBidder;

    @FXML
    private TextArea txtAreaLogs;

    @FXML
    private TextField txtBidAmount;

    @FXML
    private Button btnPlaceBid;

    @FXML
    private Button btnDisconnect;

    @FXML
    private HBox bannerContainer;

    @FXML
    private Label lblBanner;

    @FXML
    private HBox inputPanel;

    private AuctionConnection connection;
    private boolean isUserInitiatedDisconnect = false;


    public void initData(AuctionConnection connection, String joinSuccessResponse) {
        this.connection = connection;
        lblWelcome.setText("Logged in as: " + connection.getUsername());

        String[] parts = joinSuccessResponse.split(":");
        if (parts.length >= 5) {
            lblItemName.setText(parts[1]);
            lblStartingPrice.setText("LKR " + formatCurrency(parts[2]));
            lblHighestBid.setText("LKR " + formatCurrency(parts[3]));
            lblHighestBidder.setText(parts[4]);
        }

        appendLog("[Auction Server Started - Port 6000]");
        appendLog("[SYSTEM] Bidding is open! Starting Price is LKR " + lblStartingPrice.getText().replace("LKR ", ""));
        appendLog("[SYSTEM] Current highest bid: " + lblHighestBid.getText() + " by " + lblHighestBidder.getText());

        Thread listenerThread = new Thread(this::listenForServerUpdates, "ClientListenerThread");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenForServerUpdates() {
        try {
            String message;
            while (connection.isConnected() && (message = connection.readMessage()) != null) {
                final String rawMessage = message;

                Platform.runLater(() -> handleServerMessage(rawMessage));
            }
        } catch (IOException e) {
            if (!isUserInitiatedDisconnect) {
                Platform.runLater(() -> {
                    appendLog("[ERROR] Lost connection to server.");
                    showAlert(Alert.AlertType.ERROR, "Connection Lost", "Disconnected", "The connection to the server was lost.");
                    disableBiddingControls("Connection lost");
                });
            }
        }
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("INFO:")) {
            String info = message.substring(5);
            appendLog("[INFO] " + info);
            
        } else if (message.startsWith("BID_ACCEPTED:")) {
            String[] parts = message.split(":");
            if (parts.length >= 3) {
                String bidder = parts[1];
                double amount = Double.parseDouble(parts[2]);
                
                lblHighestBid.setText(String.format("LKR %.2f", amount));
                lblHighestBidder.setText(bidder);
                appendLog(String.format("[NEW BID] LKR %.2f placed by %s", amount, bidder));
            }
            
        } else if (message.startsWith("REJECT:")) {
            String reason = message.substring(7);
            appendLog("[REJECTED] " + reason);
            showAlert(Alert.AlertType.WARNING, "Bid Rejected", "Invalid Bid Submitted", reason);
            
        } else if (message.startsWith("END:")) {
            String[] parts = message.split(":");
            if (parts.length >= 3) {
                String winner = parts[1];
                double winAmount = Double.parseDouble(parts[2]);
                
                appendLog("=========================================");
                appendLog("          AUCTION HAS ENDED!             ");
                appendLog("=========================================");

                if (winner.equals("No bids were placed")) {
                    appendLog("No winner. No bids.");
                    lblBanner.setText("Auction Closed - No Bids");
                    showAlert(
                            Alert.AlertType.INFORMATION,
                            "Auction Closed",
                            "No Winner",
                            "No one placed a bid.");
                } else {
                    appendLog("Winner: " + winner + " | Bid: LKR " + winAmount);
                    lblBanner.setText("Winner: " + winner);
                    showAlert(
                            Alert.AlertType.INFORMATION,
                            "Auction Closed",
                            "Winner",
                            winner + " won with LKR " + winAmount);
                }
                
                bannerContainer.setVisible(true);
                bannerContainer.setManaged(true);
                disableBiddingControls("Auction closed");
            }
            
        } else if (message.startsWith("MSG:")) {
            String msg = message.substring(4);
            appendLog("[SERVER] " + msg);
        }
    }

    @FXML
    void handlePlaceBid(ActionEvent event) {
        String bidText = txtBidAmount.getText().trim();
        if (bidText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Bid Error", "Empty Bid Field", "Please enter a bid amount before submitting.");
            return;
        }

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(bidText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Bid Error", "Invalid Numeric Format", "Please enter a valid numeric value (e.g. 5200 or 5200.50).");
            return;
        }

        if (bidAmount <= 0) {
            showAlert(Alert.AlertType.WARNING, "Bid Error", "Negative or Zero Bid", "Bid amount must be greater than zero.");
            return;
        }

        connection.sendBid(bidAmount);
        txtBidAmount.clear();
        txtBidAmount.requestFocus();
    }

    @FXML
    public void handleDisconnect(ActionEvent event) {
        isUserInitiatedDisconnect = true;
        if (connection != null) {
            connection.disconnect();
        }

        if (event != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/lk/ijse/realtimeactionprojects/login.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) btnDisconnect.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setOnCloseRequest(null);
                
                stage.setScene(scene);
                stage.setTitle("E-Auction - Connect");
                stage.centerOnScreen();
                stage.show();
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Application Error", "Failed to load Login UI", e.getMessage());
            }
        }
    }

    @FXML
    void handleOpenNewClientWindow(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/lk/ijse/realtimeactionprojects/login.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("E-Auction - Connect");
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Application Error", "Failed to open new client window", e.getMessage());
        }
    }

    private void appendLog(String text) {
        System.out.println(text);
        txtAreaLogs.appendText(text + "\n");
    }

    private void disableBiddingControls(String status) {
        txtBidAmount.setDisable(true);
        btnPlaceBid.setDisable(true);
        appendLog("[SYSTEM] Bidding controls disabled (" + status + ").");
    }

    private String formatCurrency(String value) {
        try {
            double num = Double.parseDouble(value);
            return String.format("%.2f", num);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        alert.showAndWait();
    }
}
