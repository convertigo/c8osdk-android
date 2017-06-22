package com.convertigo.clientsdk.listener;

import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;

import com.couchbase.lite.Document;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.replicator.Replication.ChangeEvent;

/**
 * Listens fullSync c8o call responses.
 */
public interface C8oResponseCblListener extends C8oResponseListener {
	/**
	 * Called on fullSync Document responses.<br/>
	 * It occurs in case the fullSync requestable is 'GetDocument'.
	 * 
	 * @param requestParameters
	 * @param document
	 */
	public void onDocumentResponse(Document document, Map<String, Object> requestParameters);
	/**
	 * Called on fullSync QueryEnumerator responses.<br/>
	 * It occurs in case the fullSync requestable is 'GetAllDocuments' and 'GetView'.
	 * 
	 * @param requestParameters
	 * @param queryEnumerator
	 */
	public void onQueryEnumeratorResponse(QueryEnumerator queryEnumerator, Map<String, Object> requestParameters);
	/**
	 * Called on fullSync ChangeEvent responses.<br/>
	 * It occurs in case the fullSync requestable is 'Sync', 'ReplicatePull' and 'ReplicatePush'.
	 * 
	 * @param requestParameters
	 * @param changeEvent
	 */
	public void onReplicationChangeEventResponse(ChangeEvent changeEvent, Map<String, Object> requestParameters);
}
