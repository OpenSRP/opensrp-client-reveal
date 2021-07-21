package org.smartregister.reveal.util;

import org.smartregister.util.AppExecutors;
import org.smartregister.util.GenericInteractor;

import java.util.concurrent.Executor;

/**
 * Provides an override to execute all calls on main thread
 */
public class TestGenericInteractor extends GenericInteractor implements Executor {

    public TestGenericInteractor() {
        this.appExecutors = new AppExecutors(this, this, this);
    }

    @Override
    public void execute(Runnable command) {
        command.run();
    }
}
