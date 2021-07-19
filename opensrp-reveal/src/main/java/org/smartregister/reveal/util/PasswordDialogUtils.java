package org.smartregister.reveal.util;

import android.content.Context;
import android.content.DialogInterface;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.PasswordRequestCallback;

/**
 * Created by samuelgithengi on 2/6/19.
 */
public class PasswordDialogUtils {

    public static AlertDialog initPasswordDialog(Context context, PasswordRequestCallback callback) {
        if (context == null) {
            return null;
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_request_password, null);

        AlertDialog passwordDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.request_password_title)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(false)
                .create();

        final EditText adminPassEditText = dialogView.findViewById(R.id.admin_pass);
        ((CheckBox) dialogView.findViewById(R.id.show_password_checkbox)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    adminPassEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                else
                    adminPassEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });

        passwordDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

                passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!adminPassEditText.getText().toString().equals(Utils.getAdminPasswordNotNearStructures())) {
                            Toast.makeText(context, R.string.wrong_admin_password, Toast.LENGTH_LONG).show();
                            adminPassEditText.setError(context.getString(R.string.wrong_admin_password));
                        } else {
                            adminPassEditText.setError(null);
                            adminPassEditText.setText(null);
                            passwordDialog.dismiss();
                            callback.onPasswordVerified();
                        }
                    }
                });

                passwordDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        adminPassEditText.setText(null);
                        adminPassEditText.setError(null);
                        passwordDialog.dismiss();
                    }
                });
            }
        });
        return passwordDialog;
    }
}
