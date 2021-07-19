package org.smartregister.reveal.contract;

import android.app.DatePickerDialog;
import android.content.Intent;

import androidx.annotation.StringRes;

import com.google.android.flexbox.FlexboxLayout;

import org.smartregister.reveal.model.TaskFilterParams;

import java.util.Date;
import java.util.List;

/**
 * Created by samuelgithengi on 12/18/19.
 */
public interface FilterTasksContract {

    interface View {

        void onFiltedSelected(int size);

        void applyFilters(Intent intent);

        FlexboxLayout getBusinessStatusLayout();

        FlexboxLayout getTaskCodeLayout();

        FlexboxLayout getInterventionTypeLayout();

        FlexboxLayout getFormNameLayout();

        Date getFromDateFilter();

        boolean viewAllEvents();

        void setViewAllEvents(boolean viewAllEvents);

        void setFilterFromDate(Date dateFrom);

        void setSortBySelection(int sortBySpinner);
    }

    interface Presenter extends  DatePickerDialog.OnDateSetListener  {

        @StringRes
        Integer getStringResource(String intervention);

        void onToggleChanged(boolean isChecked, Object filterCategory, Object filterKey);

        List<String> getIntentionTypes();

        List<String> getBusinessStatusOptions();

        void onApplyFilters(String selectedItem);

        void restoreCheckedFilters(TaskFilterParams taskFilterParams);

        void onClearSelections();
    }
}
