package org.smartregister.reveal.contract;

import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;

public interface FormProcessor {
    interface Host {
        void startForm(JSONObject jsonObject, Form form, Requester requester);
    }

    interface Requester {
        void onFormProcessingResult(String jsonForm);

        FormProcessor.Host getHostFormProcessor();
    }
}
