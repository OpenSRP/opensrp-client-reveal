package org.smartregister.reveal.model;

import net.sqlcipher.database.SQLiteDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.smartregister.repository.Repository;
import org.smartregister.util.QueryComposer;

public class ChildFilterFragmentModelTest extends ChildFilterFragmentModel {

    @Mock
    private Repository repository;

    @Mock
    private SQLiteDatabase database;

    private ChildFilterFragmentModel model;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        setRepository(repository);
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        model = new ChildFilterFragmentModel();
    }

    @Test
    public void testFetchUniqueGradesExecutesCorrectQuery() throws QueryComposer.InvalidQueryException {
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        model.fetchUniqueGrades("12345");
        Mockito.verify(database).rawQuery(stringArgumentCaptor.capture(), Mockito.any());

        String expected = "SELECT distinct ec_child.grade FROM ec_child WHERE ( ec_child.grade is not null ) AND ( ec_child.location = '12345' ) ORDER BY ec_child.grade";
        Assert.assertEquals(expected, stringArgumentCaptor.getValue());
    }

    @Test
    public void testFetchUniqueGradesExecutesCorrectQueryWithoutParam() throws QueryComposer.InvalidQueryException {
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        model.fetchUniqueGrades(null);
        Mockito.verify(database).rawQuery(stringArgumentCaptor.capture(), Mockito.any());

        String expected = "SELECT distinct ec_child.grade FROM ec_child WHERE ( ec_child.grade is not null ) ORDER BY ec_child.grade";
        Assert.assertEquals(expected, stringArgumentCaptor.getValue());
    }
}
