package lk.ijse.realtimeactionprojects.controller;

import lk.ijse.realtimeactionprojects.model.Auction;
import lk.ijse.realtimeactionprojects.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Server server;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private boolean isJoined = false;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            server.log("Error initializing I/O streams for client " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
        }
    }


    @Override
    public void run() {
        try {
            String message;

            while ((message = reader.readLine()) != null) {
                if (message.startsWith("JOIN:")) {
                    handleJoin(message.substring(5).trim());
                    if (isJoined) {
                        break;
                    }
                } else {
                    sendMessage("JOIN_FAILED:Must send a join request first.");
                    closeConnection();
                    return;
                }
            }

            while (isJoined && (message = reader.readLine()) != null) {
                if (message.equals("QUIT")) {
                    server.log("Client " + username + " sent QUIT command.");
                    break;
                } else if (message.startsWith("BID:")) {
                    handleBid(message.substring(4).trim());
                } else {
                    sendMessage("REJECT:Unknown command syntax.");
                }
            }
        } catch (IOException e) {
            server.log("Connection lost with " + (username != null ? username : socket.getRemoteSocketAddress()) + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void handleJoin(String requestedUsername) {
        if (requestedUsername.isEmpty()) {
            sendMessage("JOIN_FAILED:Username cannot be empty.");
            closeConnection();
            return;
        }

        if (requestedUsername.contains(":")) {
            sendMessage("JOIN_FAILED:Username cannot contain colons.");
            closeConnection();
            return;
        }

        synchronized (server) {
            if (server.isUsernameTaken(requestedUsername)) {
                sendMessage("JOIN_FAILED:Username is already taken.");
                closeConnection();
                return;
            }

            if (server.getAuction().isEnded()) {
                sendMessage("JOIN_FAILED:The auction has already ended.");
                closeConnection();
                return;
            }

            this.username = requestedUsername;
            this.isJoined = true;
            server.addClient(this);
        }

        server.log("Client connected and registered username: " + username + " from " + socket.getRemoteSocketAddress());
        
        Auction auction = server.getAuction();
        sendMessage(String.format("JOIN_SUCCESS:%s:%.2f:%.2f:%s",
                auction.getItemName(),
                auction.getStartingPrice(),
                auction.getCurrentHighestBid(),
                auction.getHighestBidderName()));

        server.broadcast("INFO:" + username + " has joined the auction.");
    }

    private void handleBid(String bidAmountStr) {
        double bidAmount;
        try {
            bidAmount = Double.parseDouble(bidAmountStr);
        } catch (NumberFormatException e) {
            sendMessage("REJECT:Invalid bid amount format.");
            server.log("Rejected bid from " + username + ": invalid numeric format \"" + bidAmountStr + "\"");
            return;
        }

        if (bidAmount <= 0) {
            sendMessage("REJECT:Bid amount must be positive.");
            server.log("Rejected bid from " + username + ": " + bidAmountStr + " (non-positive)");
            return;
        }

        Auction auction = server.getAuction();
        server.log("Bid submission received: LKR " + bidAmount + " from " + username);

        synchronized (auction) {
            if (auction.isEnded()) {
                sendMessage("REJECT:The auction is already closed.");
                server.log("Rejected bid from " + username + ": LKR " + bidAmount + " (auction ended)");
                return;
            }

            double currentHighest = auction.getCurrentHighestBid();
            if (auction.placeBid(username, bidAmount)) {
                server.log("Accepted bid: LKR " + bidAmount + " by " + username);
                server.broadcast(String.format("BID_ACCEPTED:%s:%.2f", username, bidAmount));
            } else {
                String rejectionMsg = String.format("REJECT:Your bid of LKR %.2f was rejected. It must be higher than the current highest bid of LKR %.2f.",
                        bidAmount, currentHighest);
                sendMessage(rejectionMsg);
                server.log("Rejected bid from " + username + ": LKR " + bidAmount + " (not higher than LKR " + currentHighest + ")");
            }
        }
    }

    public synchronized void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }


    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            server.log("Error closing socket: " + e.getMessage());
        }
    }


    private void cleanup() {
        isJoined = false;
        closeConnection();
        if (username != null) {
            server.removeClient(this);
            server.log("Client disconnected: " + username);
            server.broadcast("INFO:" + username + " has left the auction.");
        }
    }


    public String getUsername() {
        return username;
    }
}
