package com.example.rtttest1;

public class AccessPoints {
    private String BSSID;
    private double x;
    private double y;

    //Constructor
    public AccessPoints(String BSSID, double x, double y) {
        this.BSSID = BSSID;
        this.x = x;
        this.y = y;
    }

    public String getBSSID() {
        return BSSID;
    }

    public double[] getPosition() {
        return new double[] {this.x,this.y};
    }

    public void setBSSID(String BSSID) {
        this.BSSID = BSSID;
    }
}
