package com.convertigo.clientsdk;

import com.convertigo.clientsdk.listener.C8oResponseListener;

import java.util.Map;

/**
 * Created by nicolasa on 04/12/2015.
 */
interface C8oResponseProgressListener extends C8oResponseListener {
    void onProgressResponse(C8oProgress progress, Map<String, Object> parameters);
}
