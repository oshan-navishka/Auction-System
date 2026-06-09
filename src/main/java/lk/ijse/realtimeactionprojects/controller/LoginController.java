package lk.ijse.realtimeactionprojects.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lk.ijse.realtimeactionprojects.util.AuctionConnection;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField txtUsername;

    @FXML
    private Button btnConnect;

    @FXML
    private Button btnNewClient;

    @FXML
    void handleConnect(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String serverIp = "localhost";

        if (username.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Username is required", "Please enter a valid username before connecting.");
            return;
        }

        btnConnect.setDisable(true);
        btnConnect.setText("CONNECTING...");

        new Thread(() -> {
            try {
                AuctionConnection connection = new AuctionConnection(serverIp, 6000, username);
                
                String response = connection.connectAndJoin();

                Platform.runLater(() -> {
                    btnConnect.setDisable(false);
                    btnConnect.setText("CONNECT TO SERVER");
                    
                    if (response.startsWith("JOIN_SUCCESS:")) {
                        navigateToAuction(connection, response);
                    } else if (response.startsWith("JOIN_FAILED:")) {
                        String reason = response.substring(12);
                        showAlert(Alert.AlertType.ERROR, "Join Failed", "Server rejected connection", reason);
                        connection.disconnect();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Invalid Response", "Received invalid message from the server: " + response);
                        connection.disconnect();
                    }
                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    btnConnect.setDisable(false);
                    btnConnect.setText("CONNECT TO SERVER");
                    showAlert(Alert.AlertType.ERROR, "Connection Error", "Failed to connect", 
                            "Could not establish connection to server at " + serverIp + " on port 6000.\nDetails: " + e.getMessage());
                });
            }
        }, "ClientConnectThread").start();
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

    private void navigateToAuction(AuctionConnection connection, String joinSuccessResponse) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/lk/ijse/realtimeactionprojects/auction.fxml"));
            Parent root = loader.load();

            AuctionController controller = loader.getController();
            controller.initData(connection, joinSuccessResponse);

            Stage stage = (Stage) btnConnect.getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setOnCloseRequest(event -> {
                controller.handleDisconnect(null);
            });

            stage.setScene(scene);
            stage.setTitle("Live Auction - " + connection.getUsername());
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Application Error", "Failed to load auction UI", e.getMessage());
            connection.disconnect();
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
