package org.smartregister.reveal.viewholder;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.reveal.R;
import org.smartregister.reveal.model.OfflineMapModel;

/**
 * Created by Richard Kareko on 2/10/20.
 */

public class BaseOfflineMapViewHolder extends RecyclerView.ViewHolder {

    protected TextView tvOfflineMapNameLabel;

    protected TextView tvDownloadingLabel;

    protected CheckBox offlineMapCheckBox;

    public BaseOfflineMapViewHolder(@NonNull View itemView) {
        super(itemView);
        tvOfflineMapNameLabel = itemView.findViewById(R.id.offline_map_label);
        tvDownloadingLabel = itemView.findViewById(R.id.downloading_label);
        offlineMapCheckBox = itemView.findViewById(R.id.offline_map_checkbox);
    }

    public void setOfflineMapLabel(String offlineMapLabel) {
        this.tvOfflineMapNameLabel.setText(offlineMapLabel);
    }

    public void setItemViewListener(OfflineMapModel offlineMapModel, View.OnClickListener onClickListener) {
        offlineMapCheckBox.setOnClickListener(onClickListener);
        offlineMapCheckBox.setTag(R.id.offline_map_checkbox, offlineMapModel);
    }

    public void enableCheckBox(boolean enable) {
        offlineMapCheckBox.setEnabled(enable);
    }

    public void checkCheckBox(boolean check) {
        offlineMapCheckBox.setChecked(check);
    }

    public void displayDownloadingLabel(boolean displayDownloadingLabel) {
        if (displayDownloadingLabel) {
            tvDownloadingLabel.setVisibility(View.VISIBLE);
        } else {
            tvDownloadingLabel.setVisibility(View.GONE);
        }
    }
}
