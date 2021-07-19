package org.smartregister.reveal.util;

import android.content.Context;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.R;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by samuelgithengi on 5/22/19.
 */
public class AlertDialogUtilsTest extends BaseUnitTest {

    private Context context = RuntimeEnvironment.application;

    @Test
    public void testDisplayNotification() {
        AlertDialog alertDialog = AlertDialogUtils.displayNotification(context, R.string.opening_form_title, R.string.opening_form_message);
        assertNotNull(alertDialog);
        assertTrue(alertDialog.isShowing());
        assertEquals(context.getString(R.string.opening_form_title), ((TextView) alertDialog.findViewById(R.id.alertTitle)).getText());
        assertEquals(context.getString(R.string.opening_form_message), ((TextView) alertDialog.findViewById(android.R.id.message)).getText());
    }

    @Test
    public void testDisplayNotificationWithFormatArgs() {
        AlertDialog alertDialog = AlertDialogUtils.displayNotification(context, R.string.opening_form_title, R.string.distance_from_structure, 20f);
        assertNotNull(alertDialog);
        assertTrue(alertDialog.isShowing());
        assertEquals(context.getString(R.string.opening_form_title), ((TextView) alertDialog.findViewById(R.id.alertTitle)).getText());
        assertEquals("20 m away", ((TextView) alertDialog.findViewById(android.R.id.message)).getText());
    }

    @Test
    public void testDisplayNotificationWithMessage() {
        AlertDialog alertDialog = AlertDialogUtils.displayNotification(context, "Custom messsage");
        assertNotNull(alertDialog);
        assertTrue(alertDialog.isShowing());
        assertEquals("Custom messsage", ((TextView) alertDialog.findViewById(android.R.id.message)).getText());
    }

    @Test
    public void testdisplayNotificationWithCallback() {
        AlertDialog alertDialog = AlertDialogUtils.displayNotificationWithCallback(context, R.string.register_outside_boundary_title,
                R.string.register_outside_boundary_warning, R.string.register, R.string.cancel, null);
        assertNotNull(alertDialog);
        assertEquals(context.getString(R.string.register_outside_boundary_title), ((TextView) alertDialog.findViewById(R.id.alertTitle)).getText());
        assertEquals(context.getString(R.string.register_outside_boundary_warning), ((TextView) alertDialog.findViewById(android.R.id.message)).getText());
        assertEquals(context.getString(R.string.register), alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).getText());
        assertEquals(context.getString(R.string.cancel), alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).getText());
    }

    @Test
    public void testdisplayNotificationWithCallbackWithFormatArgs() {
        AlertDialog alertDialog = AlertDialogUtils.displayNotificationWithCallback(context, R.string.register_outside_boundary_title,
                R.string.distance_from_structure, R.string.register, R.string.cancel, null, 20f);
        assertNotNull(alertDialog);
        assertEquals(context.getString(R.string.register_outside_boundary_title), ((TextView) alertDialog.findViewById(R.id.alertTitle)).getText());
        assertEquals("20 m away", ((TextView) alertDialog.findViewById(android.R.id.message)).getText());
        assertEquals(context.getString(R.string.register), alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).getText());
        assertEquals(context.getString(R.string.cancel), alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).getText());
    }
}
