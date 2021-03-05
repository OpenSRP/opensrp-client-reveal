package org.smartregister.reveal.model;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import net.sqlcipher.MatrixCursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;
import org.smartregister.domain.UniqueId;
import org.smartregister.repository.Repository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.repository.UniqueIdRepository;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.RevealJsonFormUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ChildModelTest extends ChildModel {

    @Mock
    private Repository repository;

    @Mock
    private SQLiteDatabase database;

    private ChildModel model;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        setRepository(repository);
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        model = new ChildModel();
    }

    @Test
    public void testSearchAndFilter() {

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"base_entity_id", "first_name", "last_name", "dob", "middle_name", "gender", "grade", "unique_id", "status"});
        matrixCursor.addRow(new Object[]{"bid_001", "Linda", "Nakitare", "2008-11-16", "", "Female", "Grade 1", "uid_123", "Visited"});
        matrixCursor.addRow(new Object[]{"bid_002", "Richard", "Kerako", "20017-11-16", "", "Male", "Grade 1", "uid_124", "Not Visited"});

        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());

        HashMap<String, List<String>> sortAndFilter = new HashMap<>();
        sortAndFilter.put(Constants.ChildFilter.SORT, Arrays.asList(new String[]{"ec_child.firstname", "ec_child.middle_name"}));
        sortAndFilter.put(Constants.ChildFilter.FILTER_GRADE, Arrays.asList(new String[]{"Grade 1", "Grade 2"}));
        sortAndFilter.put(Constants.ChildFilter.FILTER_AGE, Arrays.asList(new String[]{"6:10", "Adult"}));

        model = Mockito.spy(model);
        Mockito.doReturn("12345").when(model).getCurrentLocationID();
        List<Child> children = model.searchAndFilter(sortAndFilter, "Maria Lorenzo");

        Assert.assertEquals(children.size(), 2);

        Child child_one = children.get(0);
        Assert.assertEquals(child_one.getFirstName(), "Linda");
        Assert.assertEquals(child_one.getLastName(), "Nakitare");
        Assert.assertEquals(child_one.getGender(), "Female");
        Assert.assertEquals(child_one.getMiddleName(), "");
        Assert.assertEquals(child_one.getGrade(), "Grade 1");
        Assert.assertEquals(child_one.getUniqueID(), "uid_123");
        Assert.assertEquals(child_one.getTaskStatus(), "Visited");

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(database).rawQuery(stringArgumentCaptor.capture(), Mockito.any());

        String expected = "SELECT ec_child.base_entity_id , ec_child.first_name , ec_child.last_name , ec_child.dob , ec_child.middle_name , ec_child.gender , ec_child.grade , ec_child.unique_id , (select business_status from task where for = ec_child.base_entity_id and code = 'MDA Dispense' order by authored_on desc limit 1) as status FROM ec_child WHERE ( ( ec_child.first_name LIKE '%Maria%' ) OR ( ec_child.last_name LIKE '%Maria%' ) OR ( ec_child.unique_id LIKE '%Maria%' ) OR ( ec_child.middle_name LIKE '%Maria%' ) ) AND ( ( ec_child.first_name LIKE '%Lorenzo%' ) OR ( ec_child.last_name LIKE '%Lorenzo%' ) OR ( ec_child.unique_id LIKE '%Lorenzo%' ) OR ( ec_child.middle_name LIKE '%Lorenzo%' ) ) AND ( location = '12345' ) AND ( ec_child.grade in ( 'Grade 2' , 'Grade 1') ) AND ( (  cast(strftime('%Y.%m%d', 'now') - strftime('%Y.%m%d', ec_child.dob) as int)  between 6 and 10 ) OR (  cast(strftime('%Y.%m%d', 'now') - strftime('%Y.%m%d', ec_child.dob) as int)  > 18  ) ) ORDER BY ec_child.firstname, ec_child.middle_name";
        Assert.assertEquals(expected, stringArgumentCaptor.getValue());
    }

    @Test
    public void testGetReportCounts() {
        model = Mockito.spy(model);
        Mockito.doReturn("12345").when(model).getCurrentLocationID();

        model.getReportCounts();

        Mockito.verify(database, Mockito.times(4)).rawQuery(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testGetMDAForm() throws JSONException {
        model = Mockito.spy(model);
        Mockito.doReturn("{}").when(model).readAssetContents(Mockito.any(), Mockito.anyString());

        JSONObject jsonObject = model.getMDAForm(null, "12345");
        Assert.assertTrue(jsonObject.has(Constants.Properties.BASE_ENTITY_ID));
        Assert.assertEquals(jsonObject.get(Constants.Properties.BASE_ENTITY_ID), "12345");
    }

    @Test
    public void testGetRegistrationForm() throws JSONException {
        model = Mockito.spy(model);
        Mockito.doReturn("{}").when(model).readAssetContents(Mockito.any(), Mockito.anyString());


        UniqueId uniqueId = new UniqueId();
        uniqueId.setOpenmrsId("34567");

        UniqueIdRepository uniqueIdRepository = Mockito.mock(UniqueIdRepository.class);
        Mockito.doReturn(uniqueId).when(uniqueIdRepository).getNextUniqueId();
        Mockito.doReturn(uniqueIdRepository).when(model).getUniqueIdRepository();

        RevealJsonFormUtils revealJsonFormUtils = Mockito.mock(RevealJsonFormUtils.class);
        Whitebox.setInternalState(model, "revealJsonFormUtils", revealJsonFormUtils);

        JSONObject jsonObject = model.getRegistrationForm(null);
        Mockito.verify(revealJsonFormUtils).populateField(jsonObject, Constants.DatabaseKeys.UNIQUE_ID, "34567", JsonFormConstants.VALUE);
        Assert.assertNotNull(jsonObject);
    }

    @Test
    public void testGetCurrentTask() {
        TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
        model = Mockito.spy(model);
        Mockito.doReturn(taskRepository).when(model).getTaskRepository();

        model.getCurrentTask(null, "taskid");
        Mockito.verify(taskRepository).getTaskByIdentifier(Mockito.any());
    }
}
