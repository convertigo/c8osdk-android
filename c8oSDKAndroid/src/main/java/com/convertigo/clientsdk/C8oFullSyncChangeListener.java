package com.convertigo.clientsdk;

import org.json.JSONObject;

/**
 * Created by Nicolas on 04/11/2016.
 */

public interface C8oFullSyncChangeListener {
    void onChange(JSONObject changes);
}
