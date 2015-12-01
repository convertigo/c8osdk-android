package com.convertigo.clientsdk.listener;

import java.util.Map;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.C8oFullSyncTranslator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.Replication.ChangeEvent;
import com.couchbase.lite.replicator.Replication.ChangeListener;

public class C8oJSONChangeEventListener implements ChangeListener {

	private C8oResponseJsonListener C8oResponseJsonListener;
	private Map<String, Object> parameters;
	
	public C8oJSONChangeEventListener(C8oResponseJsonListener C8oResponseJsonListener, Replication replication, Map<String, Object> parameters) {
		this.C8oResponseJsonListener = C8oResponseJsonListener;
		this.parameters = parameters;
	}
	
	@Override
	public void changed(ChangeEvent event) {
		try {
			C8oResponseJsonListener.onJsonResponse(C8oFullSyncTranslator.changeEventToJSON(event), this.parameters);
		} catch (C8oException e) {
			e.printStackTrace();
		}
	}

}
