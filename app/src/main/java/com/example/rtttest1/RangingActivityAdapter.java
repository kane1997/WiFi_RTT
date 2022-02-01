package com.example.rtttest1;

import android.net.wifi.rtt.RangingResult;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RangingActivityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "RangingActivityAdapter";
    private final List<RangingResult> Results;

    public RangingActivityAdapter(List<RangingResult> list){
        Results = list;
    }

    public static class ViewHolderItems extends RecyclerView.ViewHolder{

        public TextView myBSSIDTextView_Ranging;
        public TextView myRangeTextView_Ranging;
        public TextView myRangeSDTextView_Ranging;
        public TextView myRSSTextView_Ranging;

        public ViewHolderItems(View view){
            super(view);
            myBSSIDTextView_Ranging = view.findViewById(R.id.textViewBSSID_Recycler);
            myRangeTextView_Ranging = view.findViewById(R.id.textViewRange_Recycler);
            myRangeSDTextView_Ranging = view.findViewById((R.id.textViewRangeSD_Recycler));
            myRSSTextView_Ranging = view.findViewById((R.id.textViewRSS_Recycler));
        }
    }

    public void swapData(List<RangingResult> list) {
        Results.clear();

        if ((list != null) && (list.size() > 0)){
            Results.addAll(list);
        }
        notifyDataSetChanged();
        Log.d(TAG,"Updating");
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = new ViewHolderItems(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.ranging_activity_recycler_item, parent, false));

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolderItems viewHolderItems = (ViewHolderItems) holder;
        RangingResult currentRangingResult = Results.get(position);

        viewHolderItems.myBSSIDTextView_Ranging.
                setText((String.valueOf(currentRangingResult.getMacAddress())));
        viewHolderItems.myRangeTextView_Ranging.
                setText(String.valueOf(currentRangingResult.getDistanceMm()));
        viewHolderItems.myRangeSDTextView_Ranging.
                setText(String.valueOf(currentRangingResult.getDistanceStdDevMm()));
        viewHolderItems.myRSSTextView_Ranging.
                setText(String.valueOf(currentRangingResult.getRssi()));
    }

    @Override
    public int getItemCount() {
        return Results.size();
    }
}
