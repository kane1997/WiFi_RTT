package com.example.rtttest1;

public class AccessPoints {
    private final String BSSID;
    private final double x;
    private final double y;

    //Constructor
    public AccessPoints(String BSSID, double x, double y) {
        this.BSSID = BSSID;
        this.x = x;
        this.y = y;
    }

    public String getBSSID() {
        return BSSID;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
