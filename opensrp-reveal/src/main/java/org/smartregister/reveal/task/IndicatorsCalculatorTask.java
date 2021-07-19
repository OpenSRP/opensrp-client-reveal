package org.smartregister.reveal.task;

import android.app.Activity;
import android.os.AsyncTask;
import android.view.View;
import android.widget.LinearLayout;

import net.sqlcipher.database.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.domain.Location;
import org.smartregister.domain.Setting;
import org.smartregister.reporting.view.ProgressIndicatorView;
import org.smartregister.reporting.view.TableView;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.model.IndicatorDetails;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.util.Constants.CONFIGURATION;
import org.smartregister.reveal.util.Country;
import org.smartregister.reveal.util.IndicatorUtils;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.Utils;
import org.smartregister.reveal.view.ListTasksActivity;

import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

/**
 * Created by ndegwamartin on 2019-09-23.
 */
public class IndicatorsCalculatorTask extends AsyncTask<Void, Void, IndicatorDetails> {

    private final PreferencesUtil prefsUtil;
    private final SQLiteDatabase sqLiteDatabase;
    private LinearLayout indicatorParentView;
    private ProgressIndicatorView progressIndicator;
    private ProgressIndicatorView progressIndicator2;
    private ProgressIndicatorView progressIndicator3;
    protected Activity activity;
    private List<TaskDetails> tasks;
    private TableView tableView;

    public IndicatorsCalculatorTask(Activity context, List<TaskDetails> tasks) {
        this.activity = context;
        this.tasks = tasks;
        prefsUtil = PreferencesUtil.getInstance();
        sqLiteDatabase = RevealApplication.getInstance().getRepository().getReadableDatabase();

    }

    @Override
    protected void onPreExecute() {
        indicatorParentView = activity.findViewById(R.id.progressIndicatorViewsParent);
        progressIndicator = activity.findViewById(R.id.progressIndicatorView);
        progressIndicator2 = activity.findViewById(R.id.progressIndicatorView2);
        progressIndicator3 = activity.findViewById(R.id.progressIndicatorView3);
        tableView = activity.findViewById(R.id.tableView);

    }

    @Override
    protected IndicatorDetails doInBackground(Void... params) {
        IndicatorDetails indicatorDetails = null;

        if ((BuildConfig.BUILD_COUNTRY == Country.ZAMBIA && !Utils.isZambiaIRSLite()) || BuildConfig.BUILD_COUNTRY == Country.SENEGAL) {
            indicatorDetails = IndicatorUtils.processIndicators(this.tasks);
            indicatorDetails.setSprayIndicatorList(IndicatorUtils.populateSprayIndicators(this.activity, indicatorDetails));
        } else if (BuildConfig.BUILD_COUNTRY == Country.NAMIBIA || BuildConfig.BUILD_COUNTRY == Country.REFAPP) {
            Location operationalArea = Utils.getOperationalAreaLocation(prefsUtil.getCurrentOperationalArea());
            indicatorDetails = IndicatorUtils.getNamibiaIndicators(operationalArea.getId(), prefsUtil.getCurrentPlanId(), sqLiteDatabase);
            indicatorDetails.setTarget(calculateTarget());
            indicatorDetails.setSprayIndicatorList(IndicatorUtils.populateNamibiaSprayIndicators(this.activity, indicatorDetails));
        }
        return indicatorDetails;

    }

    public int calculateTarget() {
        int target = 0;
        String operationalId = Utils.getCurrentLocationId();
        Setting metadata = RevealApplication.getInstance().getSettingsRepository().getSetting(CONFIGURATION.JURISDICTION_METADATA);
        if (metadata != null) {
            try {
                JSONArray settings = new JSONObject(metadata.getValue()).getJSONArray(CONFIGURATION.SETTINGS);
                for (int i = 0; i < settings.length(); i++) {
                    JSONObject setting = settings.getJSONObject(i);
                    if (setting.getString(CONFIGURATION.KEY).equalsIgnoreCase(operationalId)) {
                        target = setting.getInt(CONFIGURATION.VALUE);
                        break;
                    }
                }
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
        return target;
    }


    @Override
    protected void onPostExecute(IndicatorDetails indicatorDetails) {
        if (progressIndicator == null || indicatorDetails == null) {
            Timber.w("progress indicator or indicators is null");
            return;
        }

        if(Utils.isZambiaIRSLite()) {
            indicatorParentView.setVisibility(View.GONE);
        }

        if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA || BuildConfig.BUILD_COUNTRY == Country.SENEGAL) {
            progressIndicator.setProgress(indicatorDetails.getProgress());
            progressIndicator.setTitle(this.activity.getString(R.string.n_percent, indicatorDetails.getProgress()));

            int totalFound = indicatorDetails.getSprayed() + indicatorDetails.getNotSprayed();
            int progress2 = indicatorDetails.getTotalStructures() > 0 ? Math.round(totalFound * 100 / indicatorDetails.getTotalStructures()) : 0;

            progressIndicator2.setProgress(progress2);
            progressIndicator2.setTitle(this.activity.getString(R.string.n_percent, progress2));

            int progress3 = totalFound > 0 ? Math.round((indicatorDetails.getSprayed() * 100 / totalFound)) : 0;

            progressIndicator3.setProgress(progress3);
            progressIndicator3.setTitle(this.activity.getString(R.string.n_percent, progress3));
        } else if (BuildConfig.BUILD_COUNTRY == Country.NAMIBIA || BuildConfig.BUILD_COUNTRY == Country.REFAPP) {

            progressIndicator3.setSubTitle(activity.getString(R.string.target_coverage));
            int targetCoverage = indicatorDetails.getTarget() == 0 ? 100 : (int) (indicatorDetails.getSprayed() * 100.0 / indicatorDetails.getTarget());
            progressIndicator3.setProgress(targetCoverage);
            progressIndicator3.setTitle(activity.getString(R.string.n_percent, targetCoverage));

            progressIndicator2.setSubTitle(activity.getString(R.string.found_coverage));
            int coverage = (int) (indicatorDetails.getSprayed() * 100.0 / indicatorDetails.getFoundStructures());
            progressIndicator2.setProgress(coverage);
            progressIndicator2.setTitle(this.activity.getString(R.string.n_percent, coverage));

            progressIndicator.setVisibility(View.GONE);
        }

        tableView.setTableData(Arrays.asList(new String[]{this.activity.getString(R.string.indicator), this.activity.getString(R.string.value)}), indicatorDetails.getSprayIndicatorList());

        //Show or hide depending on plan

        ((View) progressIndicator.getParent()).setVisibility(Utils.getInterventionLabel() == R.string.irs ? View.VISIBLE : View.GONE);

        if (activity instanceof ListTasksActivity)
            ((ListTasksActivity) activity).positionMyLocationAndLayerSwitcher();
    }


}
