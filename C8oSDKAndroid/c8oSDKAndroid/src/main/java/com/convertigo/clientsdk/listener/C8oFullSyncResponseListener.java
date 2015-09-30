package com.convertigo.clientsdk.listener;

import java.util.List;

import org.apache.http.NameValuePair;

import com.couchbase.lite.Document;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.replicator.Replication.ChangeEvent;

/**
 * Listens fullSync c8o call responses.
 */
public interface C8oFullSyncResponseListener extends C8oResponseListener {
	/**
	 * Called on fullSync Document responses.<br/>
	 * It occurs in case the fullSync requestable is 'GetDocument'.
	 * 
	 * @param requestParameters
	 * @param document
	 */
	public void onDocumentResponse(List<NameValuePair> requestParameters, Document document);
	/**
	 * Called on fullSync QueryEnumerator responses.<br/>
	 * It occurs in case the fullSync requestable is 'GetAllDocuments' and 'GetView'.
	 * 
	 * @param requestParameters
	 * @param queryEnumerator
	 */
	public void onQueryEnumeratorResponse(List<NameValuePair> requestParameters, QueryEnumerator queryEnumerator);
	/**
	 * Called on fullSync ChangeEvent responses.<br/>
	 * It occurs in case the fullSync requestable is 'Sync', 'ReplicatePull' and 'ReplicatePush'.
	 * 
	 * @param requestParameters
	 * @param changeEvent
	 */
	public void onReplicationChangeEventResponse(List<NameValuePair> requestParameters, ChangeEvent changeEvent);
}
