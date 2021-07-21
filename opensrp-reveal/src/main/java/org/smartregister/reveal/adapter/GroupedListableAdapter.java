package org.smartregister.reveal.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smartregister.reveal.viewholder.GroupedListableViewHolder;
import org.smartregister.view.ListContract;
import org.smartregister.view.adapter.ListableAdapter;

import java.util.List;

public abstract class GroupedListableAdapter<T extends ListContract.Identifiable, H extends GroupedListableViewHolder<T>>
        extends ListableAdapter<T, H> {

    public GroupedListableAdapter(List<T> items, ListContract.View<T> view) {
        super(items, view);
    }

    @Override
    public void reloadData(@Nullable List<T> items) {
        this.items.clear();
        if (items != null)
            this.items.addAll(items);
    }

    @Override
    public void onBindViewHolder(@NonNull H holder, int position) {
        holder.resetView();

        T previous = position > 0 ? items.get(position - 1) : null;
        T current = items.get(position);
        if (current != null) {
            holder.bindHeader(current, previous, view);
            holder.bindView(current, view);
        }
    }

}
