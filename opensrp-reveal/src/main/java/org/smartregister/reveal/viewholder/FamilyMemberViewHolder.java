package org.smartregister.reveal.viewholder;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.cursoradapter.RecyclerViewProvider;
import org.smartregister.family.fragment.BaseFamilyProfileMemberFragment;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;
import org.smartregister.reveal.R;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.contract.SmartRegisterClients;
import org.smartregister.view.customcontrols.CustomFontTextView;
import org.smartregister.view.customcontrols.FontVariant;
import org.smartregister.view.dialog.FilterOption;
import org.smartregister.view.dialog.ServiceModeOption;
import org.smartregister.view.dialog.SortOption;
import org.smartregister.view.viewholder.OnClickFormLauncher;

import java.text.MessageFormat;

import static org.smartregister.family.util.Utils.getName;

/**
 * Created by samuelgithengi on 4/8/19.
 */
public class FamilyMemberViewHolder implements RecyclerViewProvider<FamilyMemberViewHolder.RegisterViewHolder> {

    private Context context;
    private final View.OnClickListener registerActionHandler;
    private final View.OnClickListener paginationClickListener;

    public FamilyMemberViewHolder(Context context, View.OnClickListener registerActionHandler, View.OnClickListener paginationClickListener) {
        this.context = context;
        this.registerActionHandler = registerActionHandler;
        this.paginationClickListener = paginationClickListener;
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient smartRegisterClient, FamilyMemberViewHolder.RegisterViewHolder registerViewHolder) {
        CommonPersonObjectClient pc = (CommonPersonObjectClient) smartRegisterClient;
        String firstName = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true);
        String middleName = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true);
        String lastName = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.LAST_NAME, true);

        String patientName = getName(firstName, middleName, lastName);

        String dob = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.DOB, false);
        String dobString = org.smartregister.reveal.util.Utils.getAge(dob);

        String dod = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.DOD, false);
        if (StringUtils.isNotBlank(dod)) {

            dobString = Utils.getDuration(dod, dob);
            dobString = dobString.contains("y") ? dobString.substring(0, dobString.indexOf("y")) : dobString;

            patientName = patientName + ", " + dobString + " " + context.getString(R.string.deceased_brackets);
            registerViewHolder.patientNameAge.setFontVariant(FontVariant.REGULAR);
            registerViewHolder.patientNameAge.setTextColor(Color.GRAY);
            registerViewHolder.patientNameAge.setTypeface(registerViewHolder.patientNameAge.getTypeface(), Typeface.ITALIC);
        } else {
            patientName = patientName + ", " + dobString;
            registerViewHolder.patientNameAge.setFontVariant(FontVariant.REGULAR);
            registerViewHolder.patientNameAge.setTextColor(Color.BLACK);
            registerViewHolder.patientNameAge.setTypeface(registerViewHolder.patientNameAge.getTypeface(), Typeface.NORMAL);
        }


        fillValue(registerViewHolder.patientNameAge, patientName);

        String gender = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.GENDER, true);
        fillValue(registerViewHolder.gender, gender);

        View patient = registerViewHolder.patientColumn;
        attachPatientOnclickListener(patient, smartRegisterClient);

    }

    private void attachPatientOnclickListener(View view, SmartRegisterClient client) {
        view.setOnClickListener(registerActionHandler);
        view.setTag(client);
        view.setTag(R.id.VIEW_ID, BaseFamilyProfileMemberFragment.CLICK_VIEW_NORMAL);
    }

    private void fillValue(TextView v, String value) {
        if (v != null)
            v.setText(value);

    }

    @Override
    public void getFooterView(RecyclerView.ViewHolder viewHolder, int currentPageCount, int totalPageCount, boolean hasNext, boolean hasPrevious) {
        FooterViewHolder footerViewHolder = (FooterViewHolder) viewHolder;
        footerViewHolder.pageInfoView.setText(
                MessageFormat.format(context.getString(R.string.str_page_info), currentPageCount,
                        totalPageCount));

        footerViewHolder.nextPageView.setVisibility(hasNext ? View.VISIBLE : View.INVISIBLE);
        footerViewHolder.previousPageView.setVisibility(hasPrevious ? View.VISIBLE : View.INVISIBLE);

        footerViewHolder.nextPageView.setOnClickListener(paginationClickListener);
        footerViewHolder.previousPageView.setOnClickListener(paginationClickListener);
    }

    @Override
    public SmartRegisterClients updateClients(FilterOption filterOption, ServiceModeOption serviceModeOption, FilterOption filterOption1, SortOption sortOption) {
        return null;
    }

    @Override
    public void onServiceModeSelected(ServiceModeOption serviceModeOption) {//Implement Abstract Method
    }

    @Override
    public OnClickFormLauncher newFormLauncher(String s, String s1, String s2) {
        return null;
    }

    @Override
    public LayoutInflater inflater() {
        return (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public FamilyMemberViewHolder.RegisterViewHolder createViewHolder(ViewGroup viewGroup) {
        View view = inflater().inflate(R.layout.family_member_register_list_row, viewGroup, false);

        return new RegisterViewHolder(view);
    }

    @Override
    public RecyclerView.ViewHolder createFooterHolder(ViewGroup viewGroup) {
        View view = inflater().inflate(R.layout.smart_register_pagination, viewGroup, false);
        return new FooterViewHolder(view);
    }

    @Override
    public boolean isFooterViewHolder(RecyclerView.ViewHolder viewHolder) {
        return FooterViewHolder.class.isInstance(viewHolder);
    }


    public class RegisterViewHolder extends RecyclerView.ViewHolder {
        public ImageView status;
        public ImageView profile;
        public CustomFontTextView patientNameAge;
        public TextView gender;
        public View patientColumn;

        public RegisterViewHolder(View itemView) {
            super(itemView);
            this.status = itemView.findViewById(R.id.status);
            this.profile = itemView.findViewById(R.id.profile);
            this.patientNameAge = itemView.findViewById(R.id.patient_name_age);
            this.gender = itemView.findViewById(R.id.gender);
            this.patientColumn = itemView.findViewById(R.id.patient_column);
        }
    }

}