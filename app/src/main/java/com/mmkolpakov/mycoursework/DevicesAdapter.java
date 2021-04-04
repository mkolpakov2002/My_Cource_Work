package com.mmkolpakov.mycoursework;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DevicesAdapterVh> implements Filterable {

    private List<DeviceModel> userModelList;
    private final List<DeviceModel> getUserModelListFiltered;
    private final SelectedDevice selectedDevice;

    public DevicesAdapter(List<DeviceModel> deviceModelList, SelectedDevice selectedDevice) {
        this.userModelList = deviceModelList;
        this.getUserModelListFiltered = deviceModelList;
        this.selectedDevice = selectedDevice;
    }

    @NonNull
    @Override
    public DevicesAdapter.DevicesAdapterVh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        return new DevicesAdapterVh(LayoutInflater.from(context).inflate(R.layout.row_devices,null));
    }

    @Override
    public void onBindViewHolder(@NonNull DevicesAdapter.DevicesAdapterVh holder, int position) {

        DeviceModel userModel = userModelList.get(position);

        String devicename = userModel.getDeviceName();

        holder.tvDevicename.setText(devicename);

    }

    @Override
    public int getItemCount() {
        return userModelList.size();
    }

    @Override
    public Filter getFilter() {

        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults filterResults = new FilterResults();

                if(charSequence == null | charSequence.length() == 0){
                    filterResults.count = getUserModelListFiltered.size();
                    filterResults.values = getUserModelListFiltered;

                }else{
                    String searchChr = charSequence.toString().toLowerCase();

                    List<DeviceModel> resultData = new ArrayList<>();

                    for(DeviceModel userModel: getUserModelListFiltered){
                        if(userModel.getDeviceName().toLowerCase().contains(searchChr)){
                            resultData.add(userModel);
                        }
                    }
                    filterResults.count = resultData.size();
                    filterResults.values = resultData;

                }

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {

                userModelList = (List<DeviceModel>) filterResults.values;
                notifyDataSetChanged();

            }
        };
    }


    public interface SelectedDevice {

        void selectedDevice(DeviceModel deviceModel);

    }

    public class DevicesAdapterVh extends RecyclerView.ViewHolder {

        TextView tvDevicename;
        ImageView imIcon;
        public DevicesAdapterVh(@NonNull View itemView) {
            super(itemView);
            tvDevicename = itemView.findViewById(R.id.devicename);
            imIcon = itemView.findViewById(R.id.imageView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedDevice.selectedDevice(userModelList.get(getAdapterPosition()));
                }
            });


        }
    }
}