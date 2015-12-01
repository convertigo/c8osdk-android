package com.convertigo.clientsdk.listener;

import java.util.Map;

import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.Replication.ChangeEvent;
import com.couchbase.lite.replicator.Replication.ChangeListener;

public class C8oFullSyncChangeEventListener implements ChangeListener {

	private C8oResponseCblListener c8oResponseCblListener;
	private Map<String, Object> parameters;
	
	public C8oFullSyncChangeEventListener(C8oResponseCblListener c8oResponseCblListener, Replication replication, Map<String, Object> parameters) {
		this.c8oResponseCblListener = c8oResponseCblListener;
		this.parameters = parameters;
	}
	
	@Override
	public void changed(ChangeEvent event) {
		c8oResponseCblListener.onReplicationChangeEventResponse(event, parameters);
	}

}
