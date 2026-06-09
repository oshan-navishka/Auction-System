package lk.ijse.realtimeactionprojects;

import lk.ijse.realtimeactionprojects.controller.ClientHandler;
import lk.ijse.realtimeactionprojects.model.Auction;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private static final int PORT = 6000;
    private final Auction auction;
    private final List<ClientHandler> clients;
    private ServerSocket serverSocket;
    private boolean isRunning = true;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Server() {
        this.auction = new Auction();
        this.clients = new CopyOnWriteArrayList<>();
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

    public void start() {
        log("==========================================");
        log("   REAL-TIME AUCTION SERVER STARTING      ");
        log("==========================================");
        log("Item: " + auction.getItemName());
        log("Starting Price: LKR " + auction.getStartingPrice());
        log("Port: " + PORT);
        log("------------------------------------------");

        try {
            serverSocket = new ServerSocket(PORT);
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                if (!isRunning) {
                    break;
                }
                
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (SocketException e) {
            if (isRunning) {
                log("Server socket exception: " + e.getMessage());
            }
        } catch (IOException e) {
            log("Server socket error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }


    private synchronized void endAuction() {
        if (auction.isEnded()) {
            return;
        }

        auction.endAuction();
        log("Auction ended! Announcing winner...");

        String winner = auction.getHighestBidderName();
        double winningBid = auction.getCurrentHighestBid();

        String announcementMessage;
        if (winner.equals("No bids yet")) {
            announcementMessage = "END:No bids were placed:0.0";
            log("Auction finished. No bids were placed.");
        } else {
            announcementMessage = String.format("END:%s:%.2f", winner, winningBid);
            log(String.format("Auction finished. Winner: %s with a bid of LKR %.2f", winner, winningBid));
        }

        broadcast(announcementMessage);

        isRunning = false;
        shutdown();
    }

    private void shutdown() {
        isRunning = false;
        
        log("Closing all client connections...");
        for (ClientHandler client : clients) {
            client.sendMessage("MSG:The server is shutting down.");
        }
        clients.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log("Error closing server socket: " + e.getMessage());
        }
        log("Server stopped successfully.");
    }

    public void addClient(ClientHandler client) {
        clients.add(client);
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public boolean isUsernameTaken(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername() != null && client.getUsername().equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    public void log(String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        System.out.println("[" + timestamp + "] [SERVER] " + message);
    }

    public Auction getAuction() {
        return auction;
    }
}
