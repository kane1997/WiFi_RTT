package com.example.rtttest1;

import android.net.wifi.ScanResult;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<ViewHolder> {

    private static final String TAG = "MyAdapter";
    private ArrayList<ScanResult> AP_list_support_RTT;

    // A constructor
    public MyAdapter(ArrayList<ScanResult> list){
        AP_list_support_RTT = list;
    }

    public class ViewHolderItems extends ViewHolder{

        public TextView mySSIDTextView;
        public TextView myBSSIDTextView;

        public ViewHolderItems(View view){
            super(view);
            mySSIDTextView = view.findViewById(R.id.SSID_textView);
            myBSSIDTextView = view.findViewById(R.id.BSSID_textView);
        }
    }

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
        ViewHolder viewHolder = new ViewHolderItems(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_item, parent, false));

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Log.d(TAG, "onBindViewHolder()");
        ViewHolderItems viewHolderItems = (ViewHolderItems) viewHolder;
        ScanResult currentScanResult = AP_list_support_RTT.get(position);

        viewHolderItems.mySSIDTextView.setText(currentScanResult.SSID);
        viewHolderItems.myBSSIDTextView.setText(currentScanResult.BSSID);
    }

    @Override
    public int getItemCount() {
        return AP_list_support_RTT.size();
    }
}
