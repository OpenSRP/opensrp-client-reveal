package org.smartregister.reveal.shadow;

import org.mockito.Mockito;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.smartregister.Context;
import org.smartregister.repository.FormDataRepository;

/**
 * @author rkodev
 */

@Implements(Context.class)
public class ContextShadowHelper {

    @Implementation
    public FormDataRepository formDataRepository() {
        return Mockito.mock(FormDataRepository.class);
    }
}
