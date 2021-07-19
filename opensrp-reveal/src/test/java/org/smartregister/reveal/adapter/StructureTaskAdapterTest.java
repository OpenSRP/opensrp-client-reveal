package org.smartregister.reveal.adapter;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.smartregister.domain.Task;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.R;
import org.smartregister.reveal.model.StructureTaskDetails;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.BusinessStatus;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.reveal.viewholder.StructureTaskViewHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created by samuelgithengi on 4/18/19.
 */
public class StructureTaskAdapterTest extends BaseUnitTest {


    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private View.OnClickListener registerActionHandler;

    private StructureTaskAdapter adapter;

    private Context context = RuntimeEnvironment.application;

    private List<StructureTaskDetails> taskDetailsList;

    @Before
    public void setUp() {
        adapter = new StructureTaskAdapter(registerActionHandler);
        taskDetailsList = new ArrayList<>();
        taskDetailsList.add(TestingUtils.getStructureTaskDetails());
    }

    @Test
    public void testOnCreateViewHolder() {
        LinearLayout vg = new LinearLayout(context);
        StructureTaskViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        assertNotNull(holder);
        assertNotNull(Whitebox.getInternalState(holder, "nameTextView"));
        assertNotNull(Whitebox.getInternalState(holder, "actionTextView"));

    }


    @Test
    public void testOnBindViewHolderForBedNetTask() {
        StructureTaskDetails task = TestingUtils.getStructureTaskDetails();
        task.setTaskCode(Constants.Intervention.BEDNET_DISTRIBUTION);
        adapter.setTaskDetailsList(Collections.singletonList(task));
        LinearLayout vg = new LinearLayout(context);
        StructureTaskViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals(context.getString(R.string.distribute_llin), ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
        assertEquals(context.getString(R.string.record_llin), ((TextView) holder.itemView.findViewById(R.id.task_action)).getText());
    }


    @Test
    public void testOnBindViewHolderForIRSTask() {
        StructureTaskDetails task = TestingUtils.getStructureTaskDetails();
        task.setTaskCode(Constants.Intervention.IRS);
        adapter.setTaskDetailsList(Collections.singletonList(task));
        LinearLayout vg = new LinearLayout(context);
        StructureTaskViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals("IRS", ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
        assertEquals("Record\nStatus", ((TextView) holder.itemView.findViewById(R.id.task_action)).getText());
    }


    @Test
    public void testOnBindViewHolderForMBSTask() {
        StructureTaskDetails task = TestingUtils.getStructureTaskDetails();
        task.setTaskCode(Constants.Intervention.BLOOD_SCREENING);
        task.setTaskName("Ali Otala");
        adapter.setTaskDetailsList(Collections.singletonList(task));
        LinearLayout vg = new LinearLayout(context);
        StructureTaskViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals("Ali Otala", ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
        assertEquals(context.getString(R.string.record_test), ((TextView) holder.itemView.findViewById(R.id.task_action)).getText());
    }


    @Test
    public void testOnBindViewHolderForCaseConfirmationTask() {
        StructureTaskDetails task = TestingUtils.getStructureTaskDetails();
        task.setTaskCode(Constants.Intervention.CASE_CONFIRMATION);
        task.setTaskName("Ali Otala");
        adapter.setTaskDetailsList(Collections.singletonList(task));
        LinearLayout vg = new LinearLayout(context);
        StructureTaskViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals("Ali Otala", ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
        assertEquals("Detect\nCase", ((TextView) holder.itemView.findViewById(R.id.task_action)).getText());
    }

    @Test
    public void testOnBindViewHolderForRegisterFamilyTask() {
        StructureTaskDetails task = TestingUtils.getStructureTaskDetails();
        adapter.setTaskDetailsList(Collections.singletonList(task));
        LinearLayout vg = new LinearLayout(context);
        StructureTaskViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals("Household Registration", ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
        assertEquals("Register Family", ((TextView) holder.itemView.findViewById(R.id.task_action)).getText());
    }


    @Test
    public void testGetItemCount() {
        assertEquals(0, adapter.getItemCount());
        adapter.setTaskDetailsList(taskDetailsList);
        assertEquals(1, adapter.getItemCount());

    }

    @Test
    public void testUpdateTask() {
        adapter = spy(adapter);
        adapter.setTaskDetailsList(taskDetailsList);
        adapter.updateTask(taskDetailsList.get(0).getTaskId(), Task.TaskStatus.FAILED, BusinessStatus.NOT_ELIGIBLE);
        assertEquals(Task.TaskStatus.FAILED.name(), taskDetailsList.get(0).getTaskStatus());
        assertEquals(BusinessStatus.NOT_ELIGIBLE, taskDetailsList.get(0).getBusinessStatus());
        verify(adapter).notifyItemChanged(0);


    }

    @Test
    public void testOnBindViewHolderMDAAdherence() {
        String structureName = "William residence";
        StructureTaskDetails task = taskDetailsList.get(0);
        task.setTaskCode(Constants.Intervention.MDA_ADHERENCE);
        task.setTaskName(structureName);
        LinearLayout vg = new LinearLayout(context);
        StructureTaskViewHolder holder = adapter.onCreateViewHolder(vg, 0);
                adapter.setTaskDetailsList(taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals(structureName, ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
    }

    @Test
    public void testOnBindViewHolderMDADispense() {
        String structureName = "William residence";
        StructureTaskDetails task = taskDetailsList.get(0);
        task.setTaskCode(Constants.Intervention.MDA_DISPENSE);
        task.setTaskName(structureName);
        LinearLayout vg = new LinearLayout(context);
        StructureTaskViewHolder holder = adapter.onCreateViewHolder(vg, 0);
                adapter.setTaskDetailsList(taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals(structureName, ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
    }

    @Test
    public void testOnBindViewHolderMosquitoCollection() {
        StructureTaskDetails task = taskDetailsList.get(0);
        task.setTaskCode(Constants.Intervention.MOSQUITO_COLLECTION);
        LinearLayout vg = new LinearLayout(context);
        StructureTaskViewHolder holder = adapter.onCreateViewHolder(vg, 0);
                adapter.setTaskDetailsList(taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals("Mosquito collection point", ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
    }

    @Test
    public void testOnBindViewHolderLaravalDipping() {
        StructureTaskDetails task = taskDetailsList.get(0);
        task.setTaskCode(Constants.Intervention.LARVAL_DIPPING);
        LinearLayout vg = new LinearLayout(context);
        StructureTaskViewHolder holder = adapter.onCreateViewHolder(vg, 0);
                adapter.setTaskDetailsList(taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals("Larval breeding site", ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
    }

    @Test
    public void testOnBindViewHolderBCC() {
        StructureTaskDetails task = taskDetailsList.get(0);
        task.setTaskCode(Constants.Intervention.BCC);
        LinearLayout vg = new LinearLayout(context);
        StructureTaskViewHolder holder = adapter.onCreateViewHolder(vg, 0);
                adapter.setTaskDetailsList(taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals("Behaviour Change communication", ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
    }


    @Test
    public void testOnBindViewHolderPAOT() {
        StructureTaskDetails task = taskDetailsList.get(0);
        task.setTaskCode(Constants.Intervention.PAOT);
        LinearLayout vg = new LinearLayout(context);
        StructureTaskViewHolder holder = adapter.onCreateViewHolder(vg, 0);
                adapter.setTaskDetailsList(taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals("Potential Area of Transmission", ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
    }

    @Test
    public void testUpdateTasks() {

        adapter.setTaskDetailsList(taskDetailsList);
        adapter = spy(adapter);
        Set<Task> tasks = new HashSet<>();
        Task bloodScreeningTask = new Task();
        bloodScreeningTask.setIdentifier(UUID.randomUUID().toString());
        bloodScreeningTask.setCode(Constants.Intervention.BLOOD_SCREENING);
        bloodScreeningTask.setForEntity(taskDetailsList.get(0).getTaskEntity());
        tasks.add(bloodScreeningTask);
        taskDetailsList.add(getStructureTaskDetails(bloodScreeningTask));
        assertEquals(2, adapter.getItemCount());
        adapter.updateTasks(taskDetailsList.get(0).getTaskId(), Task.TaskStatus.FAILED, BusinessStatus.NOT_ELIGIBLE, tasks);
        assertEquals(1, adapter.getItemCount());
        assertEquals(Task.TaskStatus.FAILED.name(), taskDetailsList.get(0).getTaskStatus());
        assertEquals(BusinessStatus.NOT_ELIGIBLE, taskDetailsList.get(0).getBusinessStatus());
        verify(adapter).notifyDataSetChanged();


    }

    private StructureTaskDetails getStructureTaskDetails(Task task) {
        StructureTaskDetails taskDetails = new StructureTaskDetails(task.getIdentifier());
        taskDetails.setTaskCode(task.getCode());
        taskDetails.setTaskEntity(task.getForEntity());
        return taskDetails;

    }
}
