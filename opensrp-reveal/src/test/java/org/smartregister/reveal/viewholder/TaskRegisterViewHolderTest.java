package org.smartregister.reveal.viewholder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
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
import org.smartregister.reveal.model.CardDetails;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.PreferencesUtil;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.reveal.util.Constants.Intervention.BEDNET_DISTRIBUTION;
import static org.smartregister.reveal.util.Constants.Intervention.BLOOD_SCREENING;
import static org.smartregister.reveal.util.Constants.Intervention.FI;
import static org.smartregister.reveal.util.Constants.Intervention.MDA;

/**
 * Created by samuelgithengi on 3/26/19.
 */
public class TaskRegisterViewHolderTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private PreferencesUtil prefsUtil;

    @Mock
    private TaskDetails taskDetails;

    @Mock
    private CardDetails cardDetails;

    @Mock
    private View.OnClickListener onClickListener;

    private Context context = RuntimeEnvironment.application;

    private TaskRegisterViewHolder viewHolder;

    @Before
    public void setUp() {
        View view = LayoutInflater.from(context).inflate(R.layout.task_register_row, null);
        viewHolder = new TaskRegisterViewHolder(view);
    }

    @Test
    public void testSetIcon() {
        viewHolder.setIcon(R.drawable.ic_bcc);
        ImageView iconView = viewHolder.itemView.findViewById(R.id.task_icon);
        assertEquals(View.VISIBLE, iconView.getVisibility());
        assertEquals(R.drawable.ic_bcc, Shadows.shadowOf(iconView.getDrawable()).getCreatedFromResId());

        viewHolder.setIcon(R.drawable.ic_icon_danger);
        assertEquals(R.drawable.ic_icon_danger, Shadows.shadowOf(iconView.getDrawable()).getCreatedFromResId());
    }

    @Test
    public void testSetTaskName() {
        viewHolder.setTaskName("Structure 19");
        assertEquals("Structure 19", ((TextView) viewHolder.itemView.findViewById(R.id.task_name)).getText());
    }


    @Test
    public void testSetDistanceFromStructure() {
        viewHolder.setDistanceFromStructure(34, false);
        assertEquals("34 m away", ((TextView) viewHolder.itemView.findViewById(R.id.distance_from_structure)).getText());
    }

    @Test
    public void testSetDistanceFromCenterOfOperationalArea() {
        Whitebox.setInternalState(viewHolder, "prefsUtil", prefsUtil);
        when(prefsUtil.getCurrentOperationalArea()).thenReturn("MTI_13");
        viewHolder.setDistanceFromStructure(34, true);
        assertEquals("34 m from MTI_13 center", ((TextView) viewHolder.itemView.findViewById(R.id.distance_from_structure)).getText());
    }


    @Test
    public void testHideDistanceFromStructure() {
        viewHolder.hideDistanceFromStructure();
        assertEquals(View.GONE, viewHolder.itemView.findViewById(R.id.distance_from_structure).getVisibility());
    }

    @Test
    public void testHideIcon() {
        viewHolder.hideIcon();
        assertEquals(View.GONE, viewHolder.itemView.findViewById(R.id.task_icon).getVisibility());
    }


    @Test
    public void testSetTaskDetailsForNotSprayed() {
        viewHolder.setTaskDetails(Constants.BusinessStatus.NOT_SPRAYED, "No one Home");
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_details);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals("Reason: No one Home", taskDetails.getText());
    }

    @Test
    public void testSetTaskDetails() {
        viewHolder.setTaskDetails(Constants.BusinessStatus.SPRAYED, "No one Home");
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_details);
        assertEquals(View.GONE, taskDetails.getVisibility());
        assertEquals("", taskDetails.getText());
    }

    @Test
    public void testSetTaskActionWithoutCardDetails() {
        viewHolder.setTaskAction("Record\n Index", taskDetails, null, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals("Record\n Index", taskDetails.getText());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);
    }

    @Test
    public void testSetTaskActionWithCardDetails() {
        when(cardDetails.getStatusColor()).thenReturn(R.color.sprayed);
        when(taskDetails.getTaskCount()).thenReturn(null);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals("Record\n Status", taskDetails.getText());
        assertEquals(context.getColor(R.color.sprayed), taskDetails.getCurrentTextColor());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testSetTaskActionForFITaskGroupingComplete() {
        setFIAsCurrentPlan();
        PreferencesUtil.getInstance().setCurrentPlan("Focus 1");
        PreferencesUtil.getInstance().setInterventionTypeForPlan("Focus 1", FI);
        when(taskDetails.getTaskCount()).thenReturn(2);
        when(taskDetails.isFamilyRegistered()).thenReturn(true);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        when(taskDetails.isBednetDistributed()).thenReturn(true);
        when(taskDetails.isBloodScreeningDone()).thenReturn(true);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.tasks_complete), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.tasks_complete_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testSetTaskActionForFITaskGroupingFamRegistered() {
        setFIAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(2);
        when(taskDetails.isFamilyRegistered()).thenReturn(true);
        when(taskDetails.isBednetDistributed()).thenReturn(false);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        when(taskDetails.isBloodScreeningDone()).thenReturn(false);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.view_tasks), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.family_registered_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testSetTaskActionForFITaskGroupingBednetDistributed() {
        setFIAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(2);
        when(taskDetails.isFamilyRegistered()).thenReturn(true);
        when(taskDetails.isBednetDistributed()).thenReturn(true);
        when(taskDetails.isBloodScreeningDone()).thenReturn(false);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.view_tasks), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.bednet_distributed_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testSetTaskActionForFITaskGroupingBloodScreening() {
        setFIAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(2);
        when(taskDetails.isFamilyRegistered()).thenReturn(true);
        when(taskDetails.isBednetDistributed()).thenReturn(false);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        when(taskDetails.isBloodScreeningDone()).thenReturn(true);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.view_tasks), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.blood_screening_complete_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testSetTaskActionForFITaskGroupingNoTaskDone() {
        setFIAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(2);
        when(taskDetails.isFamilyRegTaskExists()).thenReturn(true);
        when(taskDetails.isFamilyRegistered()).thenReturn(false);
        when(taskDetails.isBednetDistributed()).thenReturn(false);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        when(taskDetails.isBloodScreeningDone()).thenReturn(false);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.view_tasks), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.no_task_complete_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testSetTaskActionForMDATaskGroupingMDAAdhered() {
        setMDAAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(2);
        when(taskDetails.isFamilyRegistered()).thenReturn(true);
        when(taskDetails.isMdaAdhered()).thenReturn(true);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.tasks_complete), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.mda_adhered_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testSetTaskActionForMDATaskGroupingMdaDispenced() {
        setMDAAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(2);
        when(taskDetails.isFamilyRegistered()).thenReturn(true);
        when(taskDetails.isMdaAdhered()).thenReturn(false);
        when(taskDetails.isFullyReceived()).thenReturn(true);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.view_tasks), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.mda_dispensed_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testSetTaskActionForMDATaskGroupingPartiallyReceived() {
        setMDAAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(2);
        when(taskDetails.isFamilyRegistered()).thenReturn(true);
        when(taskDetails.isMdaAdhered()).thenReturn(false);
        when(taskDetails.isFullyReceived()).thenReturn(false);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        when(taskDetails.isPartiallyReceived()).thenReturn(true);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.view_tasks), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.mda_partially_received_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testSetTaskActionForMDATaskGroupingNoneReceived() {
        setMDAAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(2);
        when(taskDetails.isFamilyRegistered()).thenReturn(true);
        when(taskDetails.isMdaAdhered()).thenReturn(false);
        when(taskDetails.isFullyReceived()).thenReturn(false);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        when(taskDetails.isPartiallyReceived()).thenReturn(false);
        when(taskDetails.isNoneReceived()).thenReturn(true);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.view_tasks), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.mda_none_received_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testSetTaskActionForMDATaskGroupingNotEligible() {
        setMDAAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(2);
        when(taskDetails.isFamilyRegistered()).thenReturn(true);
        when(taskDetails.isMdaAdhered()).thenReturn(false);
        when(taskDetails.isFullyReceived()).thenReturn(false);
        when(taskDetails.isPartiallyReceived()).thenReturn(false);
        when(taskDetails.isNoneReceived()).thenReturn(false);
        when(taskDetails.isNotEligible()).thenReturn(true);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.view_tasks), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.mda_not_eligible_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testSetTaskActionForMDATaskGroupingFamRegistered() {
        setMDAAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(2);
        when(taskDetails.isFamilyRegistered()).thenReturn(true);
        when(taskDetails.isMdaAdhered()).thenReturn(false);
        when(taskDetails.isFullyReceived()).thenReturn(false);
        when(taskDetails.isPartiallyReceived()).thenReturn(false);
        when(taskDetails.isNoneReceived()).thenReturn(false);
        when(taskDetails.isNotEligible()).thenReturn(false);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.view_tasks), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.family_registered_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testSetTaskActionForMDATaskGroupingNoneComplete() {
        setMDAAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(2);
        when(taskDetails.isFamilyRegTaskExists()).thenReturn(true);
        when(taskDetails.isFamilyRegistered()).thenReturn(false);
        when(taskDetails.isMdaAdhered()).thenReturn(false);
        when(taskDetails.isFullyReceived()).thenReturn(false);
        when(taskDetails.isPartiallyReceived()).thenReturn(false);
        when(taskDetails.isNoneReceived()).thenReturn(false);
        when(taskDetails.isNotEligible()).thenReturn(false);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.view_tasks), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.no_task_complete_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testShowFITasksCompleteActionView() {
        setFIAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(2);
        when(taskDetails.getCompleteTaskCount()).thenReturn(2);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.tasks_complete), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.tasks_complete_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testShowMDATasksCompleteActionView() {
        setMDAAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(2);
        when(taskDetails.getCompleteTaskCount()).thenReturn(2);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.tasks_complete), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.mda_adhered_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testSetHouseNumber() {
        viewHolder.setHouseNumber("House 2");
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.house_number);
        assertEquals("House 2", taskDetails.getText());
    }


    @Test
    public void testShowHouseNumber() {
        viewHolder.showHouseNumber();
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.house_number);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
    }

    @Test
    public void testSetTaskActionForFISingleTaskGroupingWithSingleBednetDistributed() {
        setFIAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(1);
        when(taskDetails.isFamilyRegistered()).thenReturn(false);
        when(taskDetails.isBednetDistributed()).thenReturn(true);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        when(taskDetails.isBloodScreeningDone()).thenReturn(false);
        when(taskDetails.getTaskCode()).thenReturn(BEDNET_DISTRIBUTION);
        when(cardDetails.getStatusColor()).thenReturn(null);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.view_tasks), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.bednet_distributed_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testSetTaskActionForFISingleTaskGroupingWithSingleBloodScreening() {
        setFIAsCurrentPlan();
        when(taskDetails.getTaskCount()).thenReturn(1);
        when(taskDetails.isFamilyRegistered()).thenReturn(false);
        when(taskDetails.isBednetDistributed()).thenReturn(false);
        when(taskDetails.hasRegisteredFamily()).thenReturn(true);
        when(taskDetails.isBloodScreeningDone()).thenReturn(true);
        when(taskDetails.getTaskCode()).thenReturn(BLOOD_SCREENING);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.view_tasks), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        assertEquals(context.getResources().getDrawable(R.drawable.blood_screening_complete_bg).getConstantState(), taskDetails.getBackground().getConstantState());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }

    @Test
    public void testMultipleTasksForNonFamilyRegistration() {
        setFIAsCurrentPlan();
        when(taskDetails.getPendingTasksCount()).thenReturn(2);
        when(taskDetails.isFamilyRegistered()).thenReturn(false);
        when(taskDetails.isBednetDistributed()).thenReturn(false);
        when(taskDetails.hasRegisteredFamily()).thenReturn(false);
        when(taskDetails.isBloodScreeningDone()).thenReturn(true);
        when(taskDetails.getTaskCode()).thenReturn(BLOOD_SCREENING);
        viewHolder.setTaskAction("Record\n Status", taskDetails, cardDetails, onClickListener);
        TextView taskDetails = viewHolder.itemView.findViewById(R.id.task_action);
        assertEquals(View.VISIBLE, taskDetails.getVisibility());
        assertEquals(context.getString(R.string.view_tasks), taskDetails.getText());
        assertEquals(context.getColor(R.color.text_black), taskDetails.getCurrentTextColor());
        taskDetails.performClick();
        verify(onClickListener).onClick(taskDetails);

    }



    private void setFIAsCurrentPlan() {
        PreferencesUtil.getInstance().setCurrentPlan("Focus 1");
        PreferencesUtil.getInstance().setInterventionTypeForPlan("Focus 1", FI);
    }

    private void setMDAAsCurrentPlan() {
        PreferencesUtil.getInstance().setCurrentPlan("MDA 1");
        PreferencesUtil.getInstance().setInterventionTypeForPlan("MDA 1", MDA);
    }

}
