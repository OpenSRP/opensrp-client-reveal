package org.smartregister.reveal.view;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.viewpager.widget.ViewPager;

import org.smartregister.domain.Task;
import org.smartregister.family.activity.BaseFamilyProfileActivity;
import org.smartregister.family.adapter.ViewPagerAdapter;
import org.smartregister.family.util.Constants.INTENT_KEY;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.FamilyProfileContract;
import org.smartregister.reveal.fragment.FamilyProfileMemberFragment;
import org.smartregister.reveal.fragment.StructureTasksFragment;
import org.smartregister.reveal.model.FamilyProfileModel;
import org.smartregister.reveal.presenter.FamilyProfilePresenter;

import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.TASK_ID;
import static org.smartregister.reveal.util.Constants.RequestCode.REQUEST_CODE_GET_JSON_FRAGMENT;

/**
 * Created by samuelgithengi on 2/8/19.
 */
public class FamilyProfileActivity extends BaseFamilyProfileActivity implements FamilyProfileContract.View {

    private StructureTasksFragment structureTasksFragment;

    private FamilyProfileMemberFragment profileMemberFragment;

    private String familyBaseEntityId;


    @Override
    protected void initializePresenter() {
        familyBaseEntityId = getIntent().getStringExtra(INTENT_KEY.FAMILY_BASE_ENTITY_ID);
        String familyHead = getIntent().getStringExtra(INTENT_KEY.FAMILY_HEAD);
        String primaryCaregiver = getIntent().getStringExtra(INTENT_KEY.PRIMARY_CAREGIVER);
        String familyName = getIntent().getStringExtra(INTENT_KEY.FAMILY_NAME);

        presenter = new FamilyProfilePresenter(this,
                new FamilyProfileModel(familyName), familyBaseEntityId, familyHead, primaryCaregiver, familyName);
    }

    @Override
    protected ViewPager setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());

        profileMemberFragment = FamilyProfileMemberFragment.newInstance(this.getIntent().getExtras());
        adapter.addFragment(profileMemberFragment, this.getString(R.string.residents).toUpperCase());

        structureTasksFragment = StructureTasksFragment.newInstance(this.getIntent().getExtras(), this);
        adapter.addFragment(structureTasksFragment, this.getString(R.string.tasks, 0).toUpperCase());

        viewPager.setAdapter(adapter);

        return viewPager;
    }

    @Override
    public void setProfileImage(String baseEntityId) {
        //do nothing
    }

    @Override
    public void setStructureId(String structureId) {
        structureTasksFragment.setStructure(structureId);
        profileMemberFragment.setStructure(structureId);

    }

    @Override
    public void refreshTasks(String structureId) {
        structureTasksFragment.refreshTasks(structureId);
    }

    @Override
    public void updateFamilyName(String firstName) {
        if (profileMemberFragment.getArguments() != null) {
            profileMemberFragment.getArguments().putString(INTENT_KEY.FAMILY_NAME, firstName);
        }
    }

    @Override
    public Activity getContext() {
        return this;
    }

    @Override
    public void returnToMapView(String structureId, Task task) {
        Intent result = new Intent();
        result.putExtra(STRUCTURE_ID, structureId);
        result.putExtra(TASK_ID, task);
        setResult(RESULT_OK, result);
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == io.ona.kujaku.utils.Constants.RequestCode.LOCATION_SETTINGS ||
                requestCode == REQUEST_CODE_GET_JSON_FRAGMENT) {
            structureTasksFragment.onActivityResult(requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.profile_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.edit_family == item.getItemId()) {
            startFormForEdit();
            return true;
        } else if (R.id.add_member == item.getItemId()) {
            presenter().onAddFamilyMember();
            return true;
        } else if (R.id.archive_family == item.getItemId()) {
            presenter().onArchiveFamilyClicked();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void startFormForEdit() {
        if (familyBaseEntityId != null) {
            presenter().fetchProfileData();
        }
    }

    @Override
    public FamilyProfileContract.Presenter presenter() {
        return (FamilyProfileContract.Presenter) super.presenter();
    }
}
