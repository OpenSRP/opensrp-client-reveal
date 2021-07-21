package org.smartregister.reveal.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.reveal.R;
import org.smartregister.reveal.model.Child;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.viewholder.GroupedListableViewHolder;
import org.smartregister.view.ListContract;

import java.util.List;

public class ChildRegisterAdapter extends GroupedListableAdapter<Child, GroupedListableViewHolder<Child>> {

    public ChildRegisterAdapter(List<Child> items, ListContract.View<Child> view) {
        super(items, view);
    }

    @Override
    public void reloadData(@Nullable List<Child> items) {
        super.reloadData(items);
    }

    @NonNull
    @Override
    public GroupedListableViewHolder<Child> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.child_register_list_row, parent, false);
        return new ChildViewHolder(view);
    }

    public static class ChildViewHolder extends GroupedListableViewHolder<Child> {

        private TextView tvName;
        private TextView tvDetails;
        private View currentView;
        private View linearLayoutAction;
        private TextView tvHeader;
        private Button btnDue;
        private Context context;

        private ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            currentView = itemView;
            tvHeader = itemView.findViewById(R.id.tvHeader);
            tvName = itemView.findViewById(R.id.patient_name);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            linearLayoutAction = itemView.findViewById(R.id.linearLayoutAction);
            btnDue = itemView.findViewById(R.id.btnDue);
            context = itemView.getContext();
        }

        @Override
        public void bindView(Child child, ListContract.View<Child> view) {
            tvName.setText(child.getFullName());
            String details = "#" + child.getUniqueID() + " \u00B7 " + child.getGender() + " \u00B7 Age " + child.getAge();
            tvDetails.setText(details);
            currentView.setOnClickListener(v -> view.onListItemClicked(child, v.getId()));

            if (StringUtils.isBlank(child.getTaskStatus()))
                return;

            switch (child.getTaskStatus()) {
                case Constants.BusinessStatus.NOT_VISITED:
                    btnDue.setOnClickListener(v -> view.onListItemClicked(child, v.getId()));
                    linearLayoutAction.setOnClickListener(v -> view.onListItemClicked(child, v.getId()));
                    linearLayoutAction.setVisibility(View.VISIBLE);

                    btnDue.setText(context.getResources().getString(R.string.record_dose));
                    btnDue.setTextColor(context.getResources().getColor(R.color.mda_partially_received));
                    btnDue.setBackground(context.getResources().getDrawable(R.drawable.due_dose_bg));
                    break;
                case Constants.BusinessStatus.VISITED_DRUG_ADMINISTERED:
                    btnDue.setOnClickListener(null);
                    linearLayoutAction.setOnClickListener(null);
                    linearLayoutAction.setVisibility(View.VISIBLE);

                    btnDue.setText(context.getResources().getString(R.string.dose_given));
                    btnDue.setTextColor(context.getResources().getColor(R.color.alert_complete_green));
                    btnDue.setBackground(null);
                    break;
                case Constants.BusinessStatus.VISITED_DRUG_NOT_ADMINISTERED:
                    btnDue.setOnClickListener(null);
                    linearLayoutAction.setOnClickListener(null);
                    linearLayoutAction.setVisibility(View.VISIBLE);

                    btnDue.setText(context.getResources().getString(R.string.not_given));
                    btnDue.setTextColor(context.getResources().getColor(R.color.dark_grey));
                    btnDue.setBackground(null);
                    break;
                default:
                    btnDue.setOnClickListener(null);
                    linearLayoutAction.setOnClickListener(null);
                    linearLayoutAction.setVisibility(View.GONE);
                    break;
            }

        }

        @Override
        public void resetView() {
            tvHeader.setText("");
            tvHeader.setVisibility(View.GONE);

            tvName.setText("");
            tvDetails.setText("");
            linearLayoutAction.setVisibility(View.GONE);
            linearLayoutAction.setOnClickListener(null);
            btnDue.setBackground(null);
        }

        @Override
        public void bindHeader(Child currentObject, @Nullable Child previousObject, ListContract.View<Child> view) {
            if (previousObject == null || !currentObject.getGrade().equals(previousObject.getGrade())) {
                tvHeader.setVisibility(View.VISIBLE);
                tvHeader.setText(currentObject.getGrade());
            }
        }
    }
}
