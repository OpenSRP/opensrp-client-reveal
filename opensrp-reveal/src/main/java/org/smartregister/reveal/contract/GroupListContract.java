package org.smartregister.reveal.contract;

import androidx.annotation.Nullable;

import org.smartregister.view.ListContract;

public interface GroupListContract extends ListContract {

    interface GroupedAdapterViewHolder<T extends ListContract.Identifiable> extends AdapterViewHolder<T> {

        void bindHeader(T currentObject, @Nullable T previousObject, ListContract.View<T> view);

    }
}
