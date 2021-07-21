package org.smartregister.reveal.presenter;

import android.content.Context;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RuntimeEnvironment;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.ChildFilterFragmentContract;
import org.smartregister.reveal.util.TestGenericInteractor;
import org.smartregister.util.QueryComposer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ronald
 */
public class ChildFilterFragmentPresenterTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private ChildFilterFragmentPresenter presenter;

    @Mock
    private ChildFilterFragmentContract.View view;

    @Mock
    private ChildFilterFragmentContract.Model model;

    @Before
    public void setUp() {
        presenter = new ChildFilterFragmentPresenter();
        presenter.usingView(view)
                .usingInteractor(new TestGenericInteractor())
                .usingModel(model);
    }

    @Test
    public void testFetchUniqueGrades() throws QueryComposer.InvalidQueryException {
        String schoolID = "schoolID";

        List<String> result = new ArrayList<>();
        Mockito.doReturn(result).when(model).fetchUniqueGrades(schoolID);

        presenter.fetchUniqueGrades(schoolID);
        Mockito.verify(view).onGradesFetched(result);
        Mockito.verify(view).setLoadingState(false);
    }

    @Test
    public void testFetchUniqueGradesWithError() throws QueryComposer.InvalidQueryException {
        String schoolID = "schoolID";

        Mockito.doThrow(new QueryComposer.InvalidQueryException()).when(model).fetchUniqueGrades(schoolID);

        presenter.fetchUniqueGrades(schoolID);
        Mockito.verify(view).onError(Mockito.any());
        Mockito.verify(view).setLoadingState(false);
    }

    @Test
    public void testGetSelectedAges() {

        Context context = RuntimeEnvironment.application;

        List<String> selectedRanges = new ArrayList<>();
        selectedRanges.add(context.getString(R.string.range_6_10));
        selectedRanges.add(context.getString(R.string.range_11_15));
        selectedRanges.add(context.getString(R.string.range_16_18));
        selectedRanges.add(context.getString(R.string.adult));

        List<String> result = presenter.getSelectedAges(selectedRanges, context);
        Assert.assertTrue(result.contains("6:10"));
        Assert.assertTrue(result.contains("11:15"));
        Assert.assertTrue(result.contains("16:18"));
        Assert.assertTrue(result.contains("Adult"));
    }
}
