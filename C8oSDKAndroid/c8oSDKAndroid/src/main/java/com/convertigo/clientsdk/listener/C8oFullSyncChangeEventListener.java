package com.convertigo.clientsdk.listener;

import java.util.List;

import org.apache.http.NameValuePair;

import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.Replication.ChangeEvent;
import com.couchbase.lite.replicator.Replication.ChangeListener;

public class C8oFullSyncChangeEventListener implements ChangeListener {

	private C8oFullSyncResponseListener c8oFullSyncResponseListener;
	private List<NameValuePair> parameters;
	
	public C8oFullSyncChangeEventListener(C8oFullSyncResponseListener c8oFullSyncResponseListener, Replication replication, List<NameValuePair> parameters) {
		this.c8oFullSyncResponseListener = c8oFullSyncResponseListener;
		this.parameters = parameters;
	}
	
	@Override
	public void changed(ChangeEvent event) {
		this.c8oFullSyncResponseListener.onReplicationChangeEventResponse(this.parameters, event);
	}

}
