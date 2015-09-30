package com.convertigo.clientsdk.fullsync;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes many FullSync response types.
 */
public class FullSyncResponse {
	/**
	 * Used to create sub-class instances.
	 */
	private static FullSyncResponse fullSyncResponsesInstance = new FullSyncResponse();
	/**
	 * The response key indicating the operation status.
	 */
	public static final String RESPONSE_KEY_OK = "ok";
	/**
	 * The response key indicating the document ID.
	 */
	public static final String RESPONSE_KEY_DOCUMENT_ID = "id";
	/**
	 * The response key indicating the document revision.
	 */
	public static final String RESPONSE_KEY_DOCUMENT_REVISION = "rev";
	
	//*** TAG Response classes ***//
	
	/**
	 * Returned by a fullSync operation without return data.
	 */
	private class FullSyncAbstractResponse {
		public boolean operationStatus;
		
		private FullSyncAbstractResponse(boolean operationStatus) {
			this.operationStatus = operationStatus;
		}
		
		Map<String, Object> getProperties() {
			LinkedHashMap<String, Object> properties = new LinkedHashMap<String, Object>(); 
			properties.put(RESPONSE_KEY_OK, this.operationStatus);
			return properties;
		}
	}
	
	/**
	 * Represents a default fullSync response.
	 */
	public class FullSyncDefaultResponse extends FullSyncAbstractResponse {
		private FullSyncDefaultResponse(boolean operationStatus) {
			super(operationStatus);
		}
	}
	
	/**
	 * Returned by a fullSync document operation without return data.
	 */
	public class FullSyncDocumentOperationResponse extends FullSyncAbstractResponse {
		public String documentId;
		public String documentRevision;
		
		private FullSyncDocumentOperationResponse(String documentId, String documentRevision, boolean operationStatus) {
			super(operationStatus);
			this.documentId = documentId;
			this.documentRevision = documentRevision;
		}

		@Override
		Map<String, Object> getProperties() {
			Map<String, Object> properties = super.getProperties();
			properties.put(RESPONSE_KEY_DOCUMENT_ID, this.documentId);
			properties.put(RESPONSE_KEY_DOCUMENT_REVISION, this.documentRevision);
			return properties;
		}
	}
	
	//*** TAG Creator methods ***//
	
	/**
	 * Returns a new instance of FullSyncDefaultResponse.
	 * 
	 * @param operationStatus
	 * @return
	 */
	static FullSyncDefaultResponse createFullSyncDefaultResponse(boolean operationStatus) {
		return fullSyncResponsesInstance.new FullSyncDefaultResponse(operationStatus);
	}
	
	/**
	 * Returns a new instance of FullSyncDocumentOperationResponse.
	 * 
	 * @param documentId
	 * @param documentRevision
	 * @param operationStatus
	 * @return
	 */
	static FullSyncDocumentOperationResponse createFullSyncDocumentOperationResponse(String documentId, String documentRevision, boolean operationStatus) {
		return fullSyncResponsesInstance.new FullSyncDocumentOperationResponse(documentId, documentRevision, operationStatus);
	}
}
