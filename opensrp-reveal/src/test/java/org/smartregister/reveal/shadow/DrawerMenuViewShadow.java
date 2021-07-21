package org.smartregister.reveal.shadow;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.smartregister.reveal.view.DrawerMenuView;

/**
 * Created by samuelgithengi on 1/27/20.
 */
@Implements(DrawerMenuView.class)
public class DrawerMenuViewShadow {

    @Implementation
    public void initializeDrawerLayout() {//Do nothing
    }

    @Implementation
    public void onResume() {//Do nothing
    }

    @Implementation
    public void openDrawerLayout(){
        //Do nothing
    }
}
