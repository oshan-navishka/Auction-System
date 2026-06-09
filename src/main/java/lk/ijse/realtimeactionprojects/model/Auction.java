package lk.ijse.realtimeactionprojects.model;

public class Auction {
    private final String itemName;
    private final double startingPrice;
    private double currentHighestBid;
    private String highestBidderName;
    private boolean isEnded;


    public Auction() {
        this.itemName = "Vintage Watch";
        this.startingPrice = 5000.0;
        this.currentHighestBid = 5000.0;
        this.highestBidderName = "No bids yet";
        this.isEnded = false;
    }

    public synchronized boolean placeBid(String bidderName, double amount) {
        if (isEnded) {
            return false;
        }
        if (amount > currentHighestBid) {
            currentHighestBid = amount;
            highestBidderName = bidderName;
            return true;
        }
        return false;
    }

    public synchronized void endAuction() {
        this.isEnded = true;
    }

    public synchronized String getItemName() {
        return itemName;
    }

    public synchronized double getStartingPrice() {
        return startingPrice;
    }

    public synchronized double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public synchronized String getHighestBidderName() {
        return highestBidderName;
    }

    public synchronized boolean isEnded() {
        return isEnded;
    }
}
