# WiFi_RTT
Use WiFi RTT to locate device.

1. Scan surrounding WiFi APs using WifiManager.startScan()
2. Get a list of APs using getScanResults
3. Check whether each AP supports 802.11mc by scanResult.is802.11mcResponder
4. WifiManager.startRanging()
