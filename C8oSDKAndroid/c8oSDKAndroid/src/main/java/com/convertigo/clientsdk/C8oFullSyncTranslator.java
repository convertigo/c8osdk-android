package com.convertigo.clientsdk;

import com.convertigo.clientsdk.FullSyncResponse.FullSyncDefaultResponse;
import com.convertigo.clientsdk.FullSyncResponse.FullSyncDocumentOperationResponse;
import com.convertigo.clientsdk.exception.C8oException;
import com.couchbase.lite.Document;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.replicator.Replication.ChangeEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;

/**
 * Provides static functions to translate fullSync responses to JSON or XML.
 */
class C8oFullSyncTranslator {
	
	private static final String FULL_SYNC_RESPONSE_KEY_COUNT = "count";
	private static final String FULL_SYNC_RESPONSE_KEY_ROWS = "rows";
	private static final String FULL_SYNC_RESPONSE_KEY_CURRENT = "current";
	private static final String FULL_SYNC_RESPONSE_KEY_DIRECTION = "direction";
	private static final String FULL_SYNC_RESPONSE_KEY_TOTAL = "total";
	private static final String FULL_SYNC_RESPONSE_KEY_OK = "ok";
	private static final String FULL_SYNC_RESPONSE_KEY_STATUS = "status";

	static final String FULL_SYNC_RESPONSE_VALUE_DIRECTION_PUSH = "push";
	static final String FULL_SYNC_RESPONSE_VALUE_DIRECTION_PULL = "pull";
	
	private static final String XML_KEY_DOCUMENT = "document";
	private static final String XML_KEY_COUCHDB_OUTPUT = "couchdb_output";
	
	/**
	 * Translates a fullSync JSON document to an XML document.
	 * 
	 * @param builder
	 * @param json
	 * @return
	 * @throws C8oException
	 */
	private static org.w3c.dom.Document fullSyncJsonToXml(JSONObject json, DocumentBuilder builder) throws C8oException {
		org.w3c.dom.Document xmlDocument;
		// Lock on the document builder
		synchronized (builder) {
			xmlDocument = builder.newDocument();
		}
      
		// Create the root element node
		Element rootElement = xmlDocument.createElement(XML_KEY_DOCUMENT);
		xmlDocument.appendChild(rootElement);
		Element couchdb_output = xmlDocument.createElement(XML_KEY_COUCHDB_OUTPUT);
		
		// Translates the JSON document
		try {
			C8oTranslator.jsonValueToXml(json, xmlDocument, couchdb_output);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.jsonValueToXML(), e);
		}
		
		rootElement.appendChild(couchdb_output);
		return xmlDocument;
	}
	
	//*** TAG Document ***//
	//*** GetDocument ***//
	
	/**
	 * Translates a Document instance to a JSON document.
	 * 
	 * @param document
	 * @return
	 */
	static JSONObject documentToJson(Document document) {
		JSONObject json = new JSONObject(document.getProperties());
		return json;
	}
	
	/**
	 * Translates a Document instance to a XML document.
	 * 
	 * @param document
	 * @param builder
	 * @return
	 * @throws C8oException 
	 */
	static org.w3c.dom.Document documentToXML(Document document, DocumentBuilder builder) throws C8oException {
		JSONObject json = documentToJson(document);
		try {
			return fullSyncJsonToXml(json, builder);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.fullSyncJsonToXML(), e);
		}
	}
	
	//*** TAG FullSyncDocumentOperationResponse ***//
	//*** DeleteDocument, PostDocument ***//
	
	static JSONObject fullSyncDocumentOperationResponseToJson(FullSyncDocumentOperationResponse fullSyncDocumentOperationResponse) {
		JSONObject json = new JSONObject(fullSyncDocumentOperationResponse.getProperties());
		return json;
	}
	
	static org.w3c.dom.Document fullSyncDocumentOperationResponseToXML(FullSyncDocumentOperationResponse fullSyncDocumentOperationResponse, DocumentBuilder builder) throws C8oException {
		try {
			return fullSyncJsonToXml(fullSyncDocumentOperationResponseToJson(fullSyncDocumentOperationResponse), builder);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.fullSyncJsonToXML(), e);
		}
	}
	
	//*** TAG QueryEnumerator ***//
	//*** GetAllDocuments ***//

	/**
	 * Translates a QueryEnumerator instance to a JSON document.
	 * 
	 * @param queryEnumerator
	 * @return
	 * @throws C8oException
	 */
	static JSONObject queryEnumeratorToJson(QueryEnumerator queryEnumerator) throws C8oException {
		JSONObject json = new JSONObject();
		JSONArray rowsArray = new JSONArray();
		
		while (queryEnumerator.hasNext()) {
			QueryRow queryRow = queryEnumerator.next();
		    JSONObject jsonQueryRow = new JSONObject(queryRow.asJSONDictionary());
		    rowsArray.put(jsonQueryRow);
		}
		
		try {
			json.put(FULL_SYNC_RESPONSE_KEY_COUNT, queryEnumerator.getCount());
			json.put(FULL_SYNC_RESPONSE_KEY_ROWS, rowsArray);
		} catch (JSONException e) {
			throw new C8oException(C8oExceptionMessage.putJson(), e);
		}
		
		return json;
	}
	
	/**
	 * Translates a QueryEnumerator instance to a XML document.
	 * 
	 * @param builder
	 * @param queryEnumerator
	 * @return
	 * @throws C8oException
	 */
	static org.w3c.dom.Document queryEnumeratorToXml(QueryEnumerator queryEnumerator, DocumentBuilder builder) throws C8oException {
		JSONObject json;
		try {
			json = queryEnumeratorToJson(queryEnumerator);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.queryEnumeratorToJSON(), e);
		}
		return fullSyncJsonToXml(json, builder);
	}
	
	//*** TAG DefaultResponse ***//
	//*** Sync, ReplicatePull, ReplicatePush, Reset ***//
	
	static JSONObject fullSyncDefaultResponseToJson(FullSyncDefaultResponse fullSyncDefaultResponse) {
		JSONObject json = new JSONObject(fullSyncDefaultResponse.getProperties());
		return json;
	}
	
	static org.w3c.dom.Document fullSyncDefaultResponseToXml(FullSyncDefaultResponse fullSyncDefaultResponse, DocumentBuilder builder) throws C8oException {
		try {
			return fullSyncJsonToXml(fullSyncDefaultResponseToJson(fullSyncDefaultResponse), builder);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.fullSyncJsonToXML(), e);
		}
	}
	
	//*** TAG ChangeEvent ***//
	//*** Sync, ReplicatePull, ReplicatePush ***//
	
	public static JSONObject changeEventToJSON(ChangeEvent changeEvent) throws C8oException {
		JSONObject json = new JSONObject();
		try {
			// Change count (total)
			json.put(FULL_SYNC_RESPONSE_KEY_TOTAL, changeEvent.getChangeCount());
			// Completed change count (current)
			json.put(FULL_SYNC_RESPONSE_KEY_CURRENT, changeEvent.getCompletedChangeCount());
			// Progress 
			// ???
			// Direction
			if (changeEvent.getSource().isPull()) {
				json.put(FULL_SYNC_RESPONSE_KEY_DIRECTION, FULL_SYNC_RESPONSE_VALUE_DIRECTION_PULL);
			} else {
				json.put(FULL_SYNC_RESPONSE_KEY_DIRECTION, FULL_SYNC_RESPONSE_VALUE_DIRECTION_PUSH);
			}
			// Status (ok)
			if (changeEvent.getError() == null) {
				json.put(FULL_SYNC_RESPONSE_KEY_OK, true);
			} else {
				json.put(FULL_SYNC_RESPONSE_KEY_OK, false);
			}
			
			// Status
			json.put(FULL_SYNC_RESPONSE_KEY_STATUS, changeEvent.getSource().getStatus().name());
			
		} catch (JSONException e) {
			throw new C8oException(C8oExceptionMessage.putJson(), e);
		}
		return json;
	}
	
	public static org.w3c.dom.Document changeEventToXML(ChangeEvent changeEvent, DocumentBuilder builder) throws C8oException {
		JSONObject json;
		try {
			json = changeEventToJSON(changeEvent);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.changeEventToJson(), e);
		}
		try {
			return fullSyncJsonToXml(json, builder);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.fullSyncJsonToXML(), e);
		}
	}
}
