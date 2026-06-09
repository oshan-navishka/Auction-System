package lk.ijse.realtimeactionprojects.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class AuctionConnection {

    private final String serverIp;
    private final int port;
    private final String username;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean isConnected = false;

    public AuctionConnection(String serverIp, int port, String username) {
        this.serverIp = serverIp;
        this.port = port;
        this.username = username;
    }

    public String connectAndJoin() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(serverIp, port), 5000);
        
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        isConnected = true;

        send("JOIN:" + username);

        String response = reader.readLine();
        if (response == null) {
            disconnect();
            throw new IOException("Server closed the connection during join handshake.");
        }
        return response;
    }

    public synchronized void send(String msg) {
        if (writer != null && isConnected) {
            writer.println(msg);
        }
    }

    public void sendBid(double amount) {
        send("BID:" + amount);
    }

    public String readMessage() throws IOException {
        if (reader != null && isConnected) {
            return reader.readLine();
        }
        return null;
    }

    public synchronized void disconnect() {
        if (!isConnected) {
            return;
        }
        isConnected = false;
        try {
            send("QUIT");
        } catch (Exception ignored) {
        }

        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error while disconnecting socket: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }

    public String getUsername() {
        return username;
    }
}
