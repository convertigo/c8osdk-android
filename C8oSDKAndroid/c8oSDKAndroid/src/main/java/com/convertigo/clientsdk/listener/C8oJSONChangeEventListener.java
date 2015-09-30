package com.convertigo.clientsdk.listener;

import java.util.List;

import org.apache.http.NameValuePair;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.fullsync.FullSyncTranslator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.Replication.ChangeEvent;
import com.couchbase.lite.replicator.Replication.ChangeListener;

public class C8oJSONChangeEventListener implements ChangeListener {

	private C8oJSONResponseListener c8oJSONResponseListener;
	private List<NameValuePair> parameters;
	
	public C8oJSONChangeEventListener(C8oJSONResponseListener c8oJSONResponseListener, Replication replication, List<NameValuePair> parameters) {
		this.c8oJSONResponseListener = c8oJSONResponseListener;
		this.parameters = parameters;
	}
	
	@Override
	public void changed(ChangeEvent event) {
		try {
			c8oJSONResponseListener.onJSONResponse(this.parameters, FullSyncTranslator.changeEventToJSON(event));
		} catch (C8oException e) {
			e.printStackTrace();
		}
	}

}
