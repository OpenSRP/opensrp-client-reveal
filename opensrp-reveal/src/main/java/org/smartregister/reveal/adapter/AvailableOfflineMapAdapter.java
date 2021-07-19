package org.smartregister.reveal.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.reveal.R;
import org.smartregister.reveal.model.OfflineMapModel;
import org.smartregister.reveal.viewholder.AvailableOfflineMapViewHolder;

import java.util.ArrayList;
import java.util.List;

public class AvailableOfflineMapAdapter extends RecyclerView.Adapter<AvailableOfflineMapViewHolder> {

    private Context context;

    private View.OnClickListener offlineMapClickHandler;

    private List<OfflineMapModel> offlineMapModels;

    public AvailableOfflineMapAdapter(Context context, View.OnClickListener offlineMapClickHandler) {
        this.context = context;
        this.offlineMapClickHandler = offlineMapClickHandler;
        this.offlineMapModels = new ArrayList<>();
    }


    @NonNull
    @Override
    public AvailableOfflineMapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.offline_map_row, parent, false);
        return new AvailableOfflineMapViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvailableOfflineMapViewHolder viewHolder, int position) {
        OfflineMapModel offlineMapModel = offlineMapModels.get(position);
        viewHolder.setOfflineMapLabel(offlineMapModel.getDownloadAreaLabel());
        viewHolder.setItemViewListener(offlineMapModel, offlineMapClickHandler);

        switch (offlineMapModel.getOfflineMapStatus()) {
            case READY:
                viewHolder.enableCheckBox(true);
                viewHolder.displayDownloadingLabel(false);
                viewHolder.checkCheckBox(false);
                break;
            case DOWNLOAD_STARTED:
                viewHolder.enableCheckBox(false);
                viewHolder.displayDownloadingLabel(true);
                viewHolder.checkCheckBox(true);
                break;
            case SELECTED_FOR_DOWNLOAD:
                viewHolder.enableCheckBox(true);
                viewHolder.checkCheckBox(true);
                viewHolder.displayDownloadingLabel(false);
                break;
            default:
                break;

        }

    }

    @Override
    public int getItemCount() {
        return offlineMapModels.size();
    }

    public void setOfflineMapModels(List<OfflineMapModel> offlineMapModels) {
        this.offlineMapModels = offlineMapModels;
        notifyDataSetChanged();
    }
}
