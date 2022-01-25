package com.example.rtttest1;

import android.annotation.SuppressLint;
import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.ArrayList;

public class MainActivityAdapter extends RecyclerView.Adapter<ViewHolder> {

    private final ArrayList<ScanResult> AP_list_support_RTT;

    // A constructor
    public MainActivityAdapter(ArrayList<ScanResult> list){
        AP_list_support_RTT = list;
    }

    public static class ViewHolderItems extends ViewHolder{

        public TextView mySSIDTextView_Main;
        public TextView myBSSIDTextView_Main;

        public ViewHolderItems(View view){
            super(view);
            mySSIDTextView_Main = view.findViewById(R.id.textViewSSID_Recycler);
            myBSSIDTextView_Main = view.findViewById(R.id.textViewBSSID_Recycler);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void swapData(ArrayList<ScanResult> list) {
        AP_list_support_RTT.clear();

        if ((list != null) && (list.size() > 0)){
            AP_list_support_RTT.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolder viewHolder = new ViewHolderItems(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.main_activity_recycler_item, parent, false));

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        //Log.d(TAG, "onBindViewHolder()");
        ViewHolderItems viewHolderItems = (ViewHolderItems) viewHolder;
        ScanResult currentScanResult = AP_list_support_RTT.get(position);

        viewHolderItems.mySSIDTextView_Main.setText(currentScanResult.SSID);
        viewHolderItems.myBSSIDTextView_Main.setText(currentScanResult.BSSID);
    }

    @Override
    public int getItemCount() {
        return AP_list_support_RTT.size();
    }
}
