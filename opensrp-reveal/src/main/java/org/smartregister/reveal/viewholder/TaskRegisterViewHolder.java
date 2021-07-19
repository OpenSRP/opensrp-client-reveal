package org.smartregister.reveal.viewholder;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.domain.Task;
import org.smartregister.reveal.R;
import org.smartregister.reveal.model.CardDetails;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.Utils;

import static org.smartregister.reveal.util.Constants.Intervention.BEDNET_DISTRIBUTION;
import static org.smartregister.reveal.util.Constants.Intervention.BLOOD_SCREENING;
import static org.smartregister.reveal.util.Constants.Intervention.REGISTER_FAMILY;


/**
 * Created by samuelgithengi on 3/12/19.
 */
public class TaskRegisterViewHolder extends RecyclerView.ViewHolder {

    private Context context;

    private ImageView iconView;

    private TextView nameView;

    private TextView distanceView;

    private TextView actionView;

    private TextView taskDetailsView;

    private TextView houseNumberView;

    private PreferencesUtil prefsUtil = PreferencesUtil.getInstance();

    public TaskRegisterViewHolder(@NonNull View itemView) {
        super(itemView);
        context = itemView.getContext();
        iconView = itemView.findViewById(R.id.task_icon);
        nameView = itemView.findViewById(R.id.task_name);
        distanceView = itemView.findViewById(R.id.distance_from_structure);
        taskDetailsView = itemView.findViewById(R.id.task_details);
        actionView = itemView.findViewById(R.id.task_action);
        houseNumberView = itemView.findViewById(R.id.house_number);
    }


    public void setIcon(@DrawableRes int iconResource) {
        iconView.setVisibility(View.VISIBLE);
        iconView.setImageDrawable(context.getResources().getDrawable(iconResource));
    }

    public void setTaskName(String taskName) {
        nameView.setText(taskName);
    }

    public void setDistanceFromStructure(float distance, boolean distanceFromCenter) {
        if (distanceFromCenter) {
            distanceView.setText(context.getString(
                    R.string.distance_from_center, distance, prefsUtil.getCurrentOperationalArea()));
        } else {
            distanceView.setText(context.getString(R.string.distance_from_structure, distance));
        }
    }

    public void hideDistanceFromStructure() {
        distanceView.setVisibility(View.GONE);
    }


    /**
     * Method that handles the populating of action view information on each row of the
     * task register.
     * <p>
     * It handles the text and color displayed on an Action View. Also attaches a clickListener
     * which handles clicks on the action view.
     *
     * @param actionLabel     Text that shows what action to take when action view is clicked
     * @param task            TaskDetails object used to populate info in a particular row
     * @param cardDetails     Object that contains status, status message and status color
     * @param onClickListener Click listener that handles clicks events on the Task Action
     */
    public void setTaskAction(String actionLabel, TaskDetails task, CardDetails cardDetails, View.OnClickListener onClickListener) {
        actionView.setText(actionLabel);

        boolean hasSingleGroupedTask = (BEDNET_DISTRIBUTION.equals(task.getTaskCode()) || BLOOD_SCREENING.equals(task.getTaskCode())) && task.getTaskCount() == 1;
        // registered family with multiple tasks
        if (cardDetails != null && task.hasRegisteredFamily() && task.getTaskCount() != null // task grouping only for FI
                && !(REGISTER_FAMILY.equals(task.getTaskCode()) && Task.TaskStatus.READY.name().equals(task.getTaskStatus()))) { //skip if we have a READY family reg task
            if (task.getTaskCount() > 1 || hasSingleGroupedTask) {
                if (task.getTaskCount() != task.getCompleteTaskCount()) {


                    Pair<Drawable, String > actionViewPair = getActionDrawable(task);
                    actionView.setTextColor(context.getResources().getColor(R.color.text_black));
                    actionView.setBackground(actionViewPair.first);
                    actionView.setText(actionViewPair.second);
                } else if (task.getTaskCount() == task.getCompleteTaskCount()) {
                    showTasksCompleteActionView();
                }
            }

        } else if (cardDetails != null && cardDetails.getStatusColor() != null) {
            if (task.getTaskCount() != null && task.getPendingTasksCount() > 1) {
                actionView.setText(R.string.view_tasks);
                actionView.setTextColor(context.getResources().getColor(R.color.text_black));
                actionView.setBackground(context.getResources().getDrawable(R.drawable.family_registered_bg));
            } else {
                actionView.setBackground(null);
                actionView.setTextColor(context.getResources().getColor(cardDetails.getStatusColor()));
            }
        } else {
            actionView.setBackground(context.getResources().getDrawable(R.drawable.task_action_bg));
            actionView.setTextColor(context.getResources().getColor(R.color.text_black));
        }
        actionView.setOnClickListener(onClickListener);
        actionView.setTag(R.id.task_details, task);
    }

    public void setItemViewListener(TaskDetails task, View.OnClickListener onClickListener) {
        itemView.setOnClickListener(onClickListener);
        itemView.setTag(R.id.task_details, task);
    }

    public void setTaskDetails(String businessStatus, String taskDetails) {
        if (Constants.BusinessStatus.NOT_SPRAYED.equals(businessStatus)) {
            taskDetailsView.setVisibility(View.VISIBLE);
            taskDetailsView.setText(context.getString(R.string.task_reason, taskDetails));
        } else {
            taskDetailsView.setVisibility(View.GONE);
        }
    }

    public void setHouseNumber(String houseNumber) {
        houseNumberView.setText(houseNumber);
    }

    public void showHouseNumber() {
        houseNumberView.setVisibility(View.VISIBLE);
    }
    public void hideHouseNumber() {
        houseNumberView.setVisibility(View.GONE);
    }

    public void hideIcon() {
        iconView.setVisibility(View.GONE);
    }

    private void showTasksCompleteActionView() {
        if (Utils.isFocusInvestigation()) {
            actionView.setBackground(context.getResources().getDrawable(R.drawable.tasks_complete_bg));
        } else if (Utils.isMDA()){
            actionView.setBackground(context.getResources().getDrawable(R.drawable.mda_adhered_bg));
        }
        actionView.setTextColor(context.getResources().getColor(R.color.text_black));
        actionView.setText(context.getText(R.string.tasks_complete));
    }

    private Pair<Drawable, String > getActionDrawable(TaskDetails task) {
        // The assumption is that a register structure task always exists if the structure has
        // atleast one bednet distribution or blood screening task
        boolean familyRegTaskMissingOrFamilyRegComplete = task.isFamilyRegistered() || !task.isFamilyRegTaskExists();
        Drawable actionBg = null;
        String actionText = context.getText(R.string.view_tasks).toString();

        if (Utils.isFocusInvestigation()) {
            if (familyRegTaskMissingOrFamilyRegComplete && task.isBednetDistributed() && task.isBloodScreeningDone()) {
                actionBg = context.getResources().getDrawable(R.drawable.tasks_complete_bg);
                actionText = context.getText(R.string.tasks_complete).toString();
            } else if (familyRegTaskMissingOrFamilyRegComplete && !task.isBednetDistributed() && !task.isBloodScreeningDone()) {
                actionBg = context.getResources().getDrawable(R.drawable.family_registered_bg);
            } else if (familyRegTaskMissingOrFamilyRegComplete && task.isBednetDistributed()) {
                actionBg = context.getResources().getDrawable(R.drawable.bednet_distributed_bg);
            } else if (task.isBloodScreeningDone()) {
                actionBg = context.getResources().getDrawable(R.drawable.blood_screening_complete_bg);
            } else {
                actionBg = context.getResources().getDrawable(R.drawable.no_task_complete_bg);
            }
        } else if (Utils.isMDA()) {
            if (familyRegTaskMissingOrFamilyRegComplete && task.isMdaAdhered()) {
                actionBg = context.getResources().getDrawable(R.drawable.mda_adhered_bg);
                actionText = context.getText(R.string.tasks_complete).toString();
            } else if (familyRegTaskMissingOrFamilyRegComplete && task.isFullyReceived()) {
                actionBg = context.getResources().getDrawable(R.drawable.mda_dispensed_bg);
            } else if (familyRegTaskMissingOrFamilyRegComplete && task.isPartiallyReceived()) {
                actionBg = context.getResources().getDrawable(R.drawable.mda_partially_received_bg);
            } else if (familyRegTaskMissingOrFamilyRegComplete && task.isNoneReceived()) {
                actionBg = context.getResources().getDrawable(R.drawable.mda_none_received_bg);
            } else if (familyRegTaskMissingOrFamilyRegComplete && task.isNotEligible()) {
                actionBg = context.getResources().getDrawable(R.drawable.mda_not_eligible_bg);
            } else if (familyRegTaskMissingOrFamilyRegComplete) {
                actionBg = context.getResources().getDrawable(R.drawable.family_registered_bg);
            } else {
                actionBg = context.getResources().getDrawable(R.drawable.no_task_complete_bg);
            }
        }

        return new Pair<>(actionBg, actionText);
    }
}
