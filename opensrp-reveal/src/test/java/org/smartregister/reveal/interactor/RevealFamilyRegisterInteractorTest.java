package org.smartregister.reveal.interactor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.smartregister.domain.db.EventClient;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.contract.FamilyRegisterContract;
import org.smartregister.reveal.sync.RevealClientProcessor;
import org.smartregister.reveal.util.AppExecutors;
import org.smartregister.sync.ClientProcessorForJava;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * Created by samuelgithengi on 4/25/19.
 */
public class RevealFamilyRegisterInteractorTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private FamilyRegisterContract.Presenter presenter;


    @Mock
    private RevealClientProcessor clientProcessor;

    private RevealFamilyRegisterInteractor interactor;

    @Before
    public void setUp() {
        interactor = new RevealFamilyRegisterInteractor(presenter);
        AppExecutors appExecutors = new AppExecutors(Executors.newSingleThreadExecutor(),
                Executors.newSingleThreadExecutor(), Executors.newSingleThreadExecutor());
        Whitebox.setInternalState(interactor, "appExecutors", appExecutors);
        Whitebox.setInternalState(interactor, "clientProcessor", clientProcessor);
    }

    @Test
    public void testGetClientProcessorForJava() {
        ClientProcessorForJava clientProcessor = interactor.getClientProcessorForJava();
        assertNotNull(clientProcessor);
        assertTrue(clientProcessor instanceof RevealClientProcessor);
    }

    @Test
    public void testProcessClient() {
        List<EventClient> eventClientList = Collections.singletonList(new EventClient(new org.smartregister.domain.Event()));
        interactor.processClient(eventClientList);
        verify(clientProcessor).processClient(eventClientList, true);
    }


}
