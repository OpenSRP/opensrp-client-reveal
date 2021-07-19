package org.smartregister.reveal.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
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
import org.robolectric.Shadows;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.R;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.Intervention;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.reveal.viewholder.TaskRegisterViewHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by samuelgithengi on 3/26/19.
 */
public class TaskRegisterAdapterTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private View.OnClickListener registerActionHandler;

    private Context context = RuntimeEnvironment.application;

    private TaskRegisterAdapter adapter;

    private List<TaskDetails> taskDetailsList;

    @Before
    public void setUp() {
        adapter = new TaskRegisterAdapter(context, registerActionHandler);
        taskDetailsList = new ArrayList<>();
        taskDetailsList.add(TestingUtils.getTaskDetails());
    }

    @Test
    public void testOnCreateViewHolder() {
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        assertNotNull(holder);
        assertNotNull(Whitebox.getInternalState(holder, "iconView"));
        assertNotNull(Whitebox.getInternalState(holder, "nameView"));
        assertNotNull(Whitebox.getInternalState(holder, "distanceView"));
        assertNotNull(Whitebox.getInternalState(holder, "actionView"));
        assertNotNull(Whitebox.getInternalState(holder, "taskDetailsView"));
    }

    @Test
    public void testonBindViewHolder() {
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals("Kenny House", ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
        assertEquals("26 m away", ((TextView) holder.itemView.findViewById(R.id.distance_from_structure)).getText());
        assertEquals("Not\nSprayable", ((TextView) holder.itemView.findViewById(R.id.task_action)).getText());
        assertEquals(View.GONE, holder.itemView.findViewById(R.id.task_details).getVisibility());
        assertEquals(View.GONE, holder.itemView.findViewById(R.id.task_icon).getVisibility());
    }

    @Test
    public void testonBindViewHolderWithoutName() {
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        TaskDetails taskDetails = new TaskDetails(UUID.randomUUID().toString());
        taskDetails.setBusinessStatus(Constants.BusinessStatus.IN_PROGRESS);
        taskDetailsList.add(taskDetails);
        adapter.onBindViewHolder(holder, 1);
        assertEquals(context.getString(R.string.unenumerated_structure), ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
        assertEquals("In\nProgress", ((TextView) holder.itemView.findViewById(R.id.task_action)).getText());
        assertEquals(View.GONE, holder.itemView.findViewById(R.id.task_details).getVisibility());
        assertEquals(View.GONE, holder.itemView.findViewById(R.id.task_icon).getVisibility());
    }


    @Test
    public void testonBindViewHolderMosquitoPoint() {
        TaskDetails task = taskDetailsList.get(0);
        task.setTaskCode(Intervention.MOSQUITO_COLLECTION);
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals(context.getString(R.string.mosquito_collection_point), ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
        assertEquals("26 m away", ((TextView) holder.itemView.findViewById(R.id.distance_from_structure)).getText());
        assertEquals("Not\nSprayable", ((TextView) holder.itemView.findViewById(R.id.task_action)).getText());
        assertEquals(View.GONE, holder.itemView.findViewById(R.id.task_details).getVisibility());
        assertEquals(View.GONE, holder.itemView.findViewById(R.id.task_icon).getVisibility());
    }

    @Test
    public void testonBindViewHolderLarvacide() {
        TaskDetails task = taskDetailsList.get(0);
        task.setTaskCode(Intervention.LARVAL_DIPPING);
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals(context.getString(R.string.larval_breeding_site), ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
        assertEquals("26 m away", ((TextView) holder.itemView.findViewById(R.id.distance_from_structure)).getText());
        assertEquals("Not\nSprayable", ((TextView) holder.itemView.findViewById(R.id.task_action)).getText());
        assertEquals(View.GONE, holder.itemView.findViewById(R.id.task_details).getVisibility());
        assertEquals(View.GONE, holder.itemView.findViewById(R.id.task_icon).getVisibility());
    }

    @Test
    public void testonBindViewHolderForBCCShouldDisplayIconAndHideDistance() {
        TaskDetails bccTask = new TaskDetails(UUID.randomUUID().toString());
        taskDetailsList.add(bccTask);
        bccTask.setTaskCode(Intervention.BCC);
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 1);

        assertEquals(context.getString(R.string.bcc), ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
        assertEquals(View.GONE, holder.itemView.findViewById(R.id.distance_from_structure).getVisibility());

        ImageView imageView = holder.itemView.findViewById(R.id.task_icon);
        assertEquals(View.VISIBLE, imageView.getVisibility());
        assertEquals(R.drawable.ic_bcc, Shadows.shadowOf(imageView.getDrawable()).getCreatedFromResId());
    }

    @Test
    public void testonBindViewHolderForUnsprayedShouldDisplayReason() {

        TaskDetails task = taskDetailsList.get(0);
        task.setBusinessStatus(Constants.BusinessStatus.NOT_SPRAYED);
        task.setStructureName(null);
        task.setTaskDetails("Locked");
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 0);

        assertEquals(context.getString(R.string.unenumerated_structure), ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
        assertEquals("26 m away", ((TextView) holder.itemView.findViewById(R.id.distance_from_structure)).getText());
        assertEquals("Not\nSprayed", ((TextView) holder.itemView.findViewById(R.id.task_action)).getText());
        assertEquals("Reason: Locked", ((TextView) holder.itemView.findViewById(R.id.task_details)).getText());
        assertEquals(View.VISIBLE, holder.itemView.findViewById(R.id.task_details).getVisibility());
        assertEquals(View.GONE, holder.itemView.findViewById(R.id.task_icon).getVisibility());

    }


    @Test
    public void testonBindViewHolderForIndexCase() {
        TaskDetails indexCaseTask = new TaskDetails(UUID.randomUUID().toString());
        taskDetailsList.add(indexCaseTask);
        indexCaseTask.setTaskCode(Intervention.CASE_CONFIRMATION);
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 1);

        assertEquals(context.getString(R.string.classification_details), ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
        assertEquals(View.GONE, holder.itemView.findViewById(R.id.distance_from_structure).getVisibility());


        ImageView imageView = holder.itemView.findViewById(R.id.task_icon);
        assertEquals(View.VISIBLE, imageView.getVisibility());
        assertEquals(R.drawable.ic_classification_details, Shadows.shadowOf(imageView.getDrawable()).getCreatedFromResId());
    }

    @Test
    public void testonBindViewHolderForPAOT() {
        TaskDetails task = taskDetailsList.get(0);
        task.setTaskCode(Intervention.PAOT);
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 0);

        assertEquals(context.getString(R.string.card_view_paot), ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
        assertEquals(View.VISIBLE, holder.itemView.findViewById(R.id.distance_from_structure).getVisibility());
        assertEquals("26 m away", ((TextView) holder.itemView.findViewById(R.id.distance_from_structure)).getText());
        assertEquals(View.GONE, holder.itemView.findViewById(R.id.task_icon).getVisibility());
    }

    @Test
    public void testGetItemCount() {
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        assertEquals(1, adapter.getItemCount());

        TaskDetails bccTask = new TaskDetails(UUID.randomUUID().toString());
        taskDetailsList.add(bccTask);

        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void testSetTaskDetails() {

        adapter = spy(adapter);
        assertEquals(0, adapter.getItemCount());
        adapter.setTaskDetails(taskDetailsList);
        assertEquals(1, adapter.getItemCount());

        TaskDetails bccTask = new TaskDetails(UUID.randomUUID().toString());
        taskDetailsList.add(bccTask);

        assertEquals(2, adapter.getItemCount());

        verify(adapter).notifyDataSetChanged();


        adapter.setTaskDetails(taskDetailsList);
        assertEquals(2, adapter.getItemCount());
        verify(adapter, times(2)).notifyDataSetChanged();
    }

    @Test
    public void testOnBindViewHolderUsesFamilyNameNotStructureNameIfBothExist() {
        String structureName = "William residence";
        String familyName = "John";
        TaskDetails task = taskDetailsList.get(0);
        task.setFamilyName(familyName);
        task.setStructureName(structureName);
        task.setTaskCode(Constants.Intervention.REGISTER_FAMILY);
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals(familyName, ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
    }

    @Test
    public void testOnBindViewHolderUsesStructureNameIfFamilyNameIsMissing() {
        String structureName = "William residence";
        TaskDetails task = taskDetailsList.get(0);
        task.setStructureName(structureName);
        task.setTaskCode(Constants.Intervention.REGISTER_FAMILY);
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals(structureName, ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
    }

    @Test
    public void testOnBindViewHolderMDAAdherence() {
        String structureName = "William residence";
        TaskDetails task = taskDetailsList.get(0);
        task.setTaskCode(Constants.Intervention.MDA_ADHERENCE);
        task.setStructureName(structureName);
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals(structureName, ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
    }

    @Test
    public void testOnBindViewHolderMDADispense() {
        String structureName = "William residence";
        TaskDetails task = taskDetailsList.get(0);
        task.setStructureName("Dispense MDA");
        task.setTaskCode(Constants.Intervention.MDA_DISPENSE);
        task.setStructureName(structureName);
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals(structureName, ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
    }

    @Test
    public void testOnBindViewHolderMosquitoCollection() {
        TaskDetails task = taskDetailsList.get(0);
        task.setTaskCode(Constants.Intervention.MOSQUITO_COLLECTION);
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals("Mosquito collection point", ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
    }

    @Test
    public void testOnBindViewHolderLaravalDipping() {
        TaskDetails task = taskDetailsList.get(0);
        task.setTaskCode(Constants.Intervention.LARVAL_DIPPING);
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals("Larval breeding site", ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
    }

    @Test
    public void testOnBindViewHolderBCC() {
        TaskDetails task = taskDetailsList.get(0);
        task.setTaskCode(Constants.Intervention.BCC);
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals("Behaviour Change communication", ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
    }


    @Test
    public void testOnBindViewHolderPAOT() {
        TaskDetails task = taskDetailsList.get(0);
        task.setTaskCode(Intervention.PAOT);
        LinearLayout vg = new LinearLayout(context);
        TaskRegisterViewHolder holder = adapter.onCreateViewHolder(vg, 0);
        Whitebox.setInternalState(adapter, "taskDetails", taskDetailsList);
        adapter.onBindViewHolder(holder, 0);
        assertEquals("Potential Area of Transmission", ((TextView) holder.itemView.findViewById(R.id.task_name)).getText());
    }
}
