package org.smartregister.reveal.viewholder;

import android.view.View;

import androidx.annotation.NonNull;

import org.smartregister.reveal.contract.GroupListContract;
import org.smartregister.view.ListContract;
import org.smartregister.view.viewholder.ListableViewHolder;

public abstract class GroupedListableViewHolder<T extends ListContract.Identifiable> extends ListableViewHolder<T> implements GroupListContract.GroupedAdapterViewHolder<T> {
    public GroupedListableViewHolder(@NonNull View itemView) {
        super(itemView);
    }
}
