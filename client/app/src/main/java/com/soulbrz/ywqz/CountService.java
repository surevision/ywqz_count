package com.soulbrz.ywqz;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

public class CountService extends JobIntentService {

    private static final int JOB_ID = 1001;

    public static void enqueue(Context context) {
        enqueueWork(context, CountService.class, JOB_ID, new Intent(context, CountService.class));
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (Api.getBaseUrl(this).length() == 0) {
            return;
        }
        String url = Api.buildUrl(this, Api.KEY_PATH_POST, Api.DEFAULT_PATH_POST);
        if (url == null) {
            return;
        }
        String result;
        try {
            long timestamp = System.currentTimeMillis();
            String body = "{\"timestamp\":" + timestamp + "}";
            result = Api.request(url, "POST", body);
        } catch (Exception e) {
            result = Api.errorMessage(e);
        }
        Api.setLastResponse(this, result);
        CountWidgetProvider.refresh(this);
    }
}
