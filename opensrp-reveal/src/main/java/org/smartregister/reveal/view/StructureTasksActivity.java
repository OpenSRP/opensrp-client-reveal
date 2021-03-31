package org.smartregister.reveal.view;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import org.smartregister.reveal.R;
import org.smartregister.reveal.fragment.StructureTasksFragment;
import org.smartregister.reveal.util.Constants;
import org.smartregister.view.activity.MultiLanguageActivity;

public class StructureTasksActivity extends MultiLanguageActivity {

    private  StructureTasksFragment structureTasksFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_structure_tasks);
        setupToolbar();
        initializeStructureFragment();
    }

    private void initializeStructureFragment() {
        structureTasksFragment = StructureTasksFragment.newInstance(this.getIntent().getExtras(), this);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, structureTasksFragment).commit();
        getSupportFragmentManager().executePendingTransactions();
        structureTasksFragment.setStructure(getIntent().getStringExtra(Constants.Properties.LOCATION_UUID));
    }

    private void setupToolbar() {
        Toolbar toolbar = this.findViewById(R.id.summary_toolbar);
        toolbar.setTitle(R.string.return_to_register);
        this.setSupportActionBar(toolbar);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

}