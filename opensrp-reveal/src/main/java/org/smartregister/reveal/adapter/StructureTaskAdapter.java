package org.smartregister.reveal.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.domain.Task;
import org.smartregister.reveal.R;
import org.smartregister.reveal.model.StructureTaskDetails;
import org.smartregister.reveal.util.CardDetailsUtil;
import org.smartregister.reveal.util.Constants.Intervention;
import org.smartregister.reveal.viewholder.StructureTaskViewHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by samuelgithengi on 4/11/19.
 */
public class StructureTaskAdapter extends RecyclerView.Adapter<StructureTaskViewHolder> {

    private Context context;

    private List<StructureTaskDetails> taskDetailsList = new ArrayList<>();
    private View.OnClickListener onClickListener;

    public StructureTaskAdapter(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public StructureTaskViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        context = viewGroup.getContext();
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.structure_task_row, viewGroup, false);
        return new StructureTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StructureTaskViewHolder viewHolder, int position) {
        StructureTaskDetails taskDetails = taskDetailsList.get(position);
        if (Intervention.BEDNET_DISTRIBUTION.equals(taskDetails.getTaskCode())) {
            taskDetails.setTaskName(context.getString(R.string.distribute_llin));
            taskDetails.setTaskAction(context.getString(R.string.record_llin));
        } else if (Intervention.IRS.equals(taskDetails.getTaskCode())) {
            taskDetails.setTaskName(context.getString(R.string.irs));
            taskDetails.setTaskAction(context.getString(R.string.record_status));
        } else {

            String action = taskDetails.getTaskAction();
            String name = taskDetails.getTaskName();
            if (Intervention.BLOOD_SCREENING.equals(taskDetails.getTaskCode())) {
                action = context.getString(R.string.record_test);
            } else if (Intervention.CASE_CONFIRMATION.equals(taskDetails.getTaskCode())) {
                action = context.getString(R.string.detect_case);
            } else if (Intervention.REGISTER_FAMILY.equals(taskDetails.getTaskCode())) {
                action = context.getString(R.string.register_family);
                name = context.getString(R.string.add_fam);
            } else if (Intervention.MDA_ADHERENCE.equals(taskDetails.getTaskCode())) {
                action = context.getString(R.string.adhere_mda);
            } else if (Intervention.MDA_DISPENSE.equals(taskDetails.getTaskCode())) {
                action = context.getString(R.string.dispense_mda);
            } else if (Intervention.MOSQUITO_COLLECTION.equals(taskDetails.getTaskCode())) {
                name = context.getString(R.string.mosquito_collection_point);
                action = context.getString(R.string.record_mosquito_collection);
            } else if (Intervention.LARVAL_DIPPING.equals(taskDetails.getTaskCode())) {
                name = context.getString(R.string.larval_breeding_site);
                action = context.getString(R.string.record_larvacide);
            } else if (Intervention.BCC.equals(taskDetails.getTaskCode())) {
                name = context.getString(R.string.bcc);
                action = context.getString(R.string.record_bcc);
            }  else if (Intervention.PAOT.equals(taskDetails.getTaskCode())) {
                name = context.getString(R.string.card_view_paot);
                if (taskDetails.getBusinessStatus() != null) {
                    action = CardDetailsUtil.getTranslatedBusinessStatus(taskDetails.getBusinessStatus()).replaceAll(" ", "\n");
                }
            }
            taskDetails.setTaskName(name);
            taskDetails.setTaskAction(action);

        }
        if (Intervention.MDA_DISPENSE.equals(taskDetails.getTaskCode()) ||
                Intervention.MDA_ADHERENCE.equals(taskDetails.getTaskCode())) {
            viewHolder.setTaskName(taskDetails.getTaskName(), taskDetails.getTaskCode());
        } else {
            viewHolder.setTaskName(taskDetails.getTaskName());
        }
        viewHolder.setTaskAction(taskDetails, onClickListener);

    }

    @Override
    public int getItemCount() {
        return taskDetailsList.size();
    }

    public void setTaskDetailsList(List<StructureTaskDetails> taskDetailsList) {
        this.taskDetailsList = taskDetailsList;
        notifyDataSetChanged();
    }

    private int updateTaskStatus(String taskID, Task.TaskStatus taskStatus, String businessStatus) {
        int position = taskDetailsList.indexOf(new StructureTaskDetails(taskID));
        if (position != -1) {
            StructureTaskDetails taskDetails = taskDetailsList.get(position);
            taskDetails.setBusinessStatus(businessStatus);
            taskDetails.setTaskStatus(taskStatus.name());
        }
        return position;
    }

    public void updateTask(String taskID, Task.TaskStatus taskStatus, String businessStatus) {
        int position = updateTaskStatus(taskID, taskStatus, businessStatus);
        if (position != -1) {
            notifyItemChanged(position);
        }
    }

    public void updateTasks(String taskID, Task.TaskStatus taskStatus, String businessStatus, Set<Task> removedTasks) {
        updateTaskStatus(taskID, taskStatus, businessStatus);
        for (Task task : removedTasks) {
            taskDetailsList.remove(new StructureTaskDetails(task.getIdentifier()));
        }
        notifyDataSetChanged();
    }
}
