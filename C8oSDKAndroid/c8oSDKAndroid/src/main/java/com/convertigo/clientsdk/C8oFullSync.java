package com.convertigo.clientsdk;

import android.content.Context;

import com.convertigo.clientsdk.FullSyncEnum.FullSyncPolicy;
import com.convertigo.clientsdk.FullSyncEnum.FullSyncRequestable;
import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oRessourceNotFoundException;
import com.convertigo.clientsdk.exception.C8oUnavailableLocalCacheException;
import com.convertigo.clientsdk.listener.C8oResponseListener;
import com.couchbase.lite.View;
import com.couchbase.lite.javascript.JavaScriptViewCompiler;

import java.util.Map;

abstract class C8oFullSync {
	private final static String FULL_SYNC_URL_PATH = "/fullsync/";
	/**
	 * The project requestable value to execute a fullSync request.
	 */
	public final static String FULL_SYNC_PROJECT = "fs://";
	public final static String FULL_SYNC__ID = "_id";
	public final static String FULL_SYNC__REV = "_rev";
	public final static String FULL_SYNC__ATTACHMENTS = "_attachments";

	protected C8o c8o;
	protected String fullSyncDatabaseUrlBase;
	protected String localSuffix;
	
	static {
		// Setup a Javascript view compiler
		View.setCompiler(new JavaScriptViewCompiler());
	}

	/**
	 * Creates an interface allowing to run fullSync requests.
	 *
	 * @param c8o
	 * @throws C8oException 
	 */
	public void Init(C8o c8o, Context context) throws C8oException {
		this.c8o = c8o;
		fullSyncDatabaseUrlBase = c8o.getEndpointConvertigo() + C8oFullSync.FULL_SYNC_URL_PATH;
		localSuffix = (c8o.getFullSyncLocalSuffix() != null) ? c8o.getFullSyncLocalSuffix() : "_device";
	}
	
	//*** TAG Request handlers ***//
	
	/**
	 * Handles a fullSync request.<br/>
	 * It determines the type of the request thanks to parameters.
	 * 
	 * @param parameters
	 * @param listener
	 * @return
	 * @throws C8oException 
	 */
	public Object handleFullSyncRequest(Map<String, Object> parameters, C8oResponseListener listener) throws C8oException {
		// Checks if this is really a fullSync request (even if this is normally already checked)
		String projectParameterValue = C8oUtils.peekParameterStringValue(parameters, C8o.ENGINE_PARAMETER_PROJECT, true);

		if (!projectParameterValue.startsWith(FULL_SYNC_PROJECT)) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterProjectRequestableFullSync(projectParameterValue));
		}

		// Gets the sequence parameter to know which fullSync requestable to use
		String fullSyncRequestableValue = C8oUtils.peekParameterStringValue(parameters, C8o.ENGINE_PARAMETER_SEQUENCE, true);
		FullSyncRequestable fullSyncRequestable = FullSyncRequestable.getFullSyncRequestable(fullSyncRequestableValue);
		if (fullSyncRequestable == null) {
			throw new IllegalArgumentException(C8oExceptionMessage.unhandledFullSyncRequestable(fullSyncRequestableValue));
		}
		
		// Gets the database name if this is not specified then if takes the default database name
		String databaseName = projectParameterValue.substring(FULL_SYNC_PROJECT.length());
		if (databaseName.length() < 1) {
			databaseName = c8o.getDefaultDatabaseName();
			if (databaseName == null) {
				throw new IllegalArgumentException(C8oExceptionMessage.invalidParamterValue(C8o.ENGINE_PARAMETER_PROJECT, null));
			}
		}
		
		Object response;
		try {
			response = fullSyncRequestable.handleFullSyncRequest(this, databaseName, parameters, listener);
		} catch (C8oRessourceNotFoundException e) {
			throw new C8oException(C8oExceptionMessage.fullSyncHandleRequest(fullSyncRequestableValue, databaseName, parameters), e);
		} catch (Exception e) {
			throw new C8oException(C8oExceptionMessage.fullSyncHandleRequest(fullSyncRequestableValue, databaseName, parameters), e);
		}
		
		if (response == null) {
			throw new C8oException(C8oExceptionMessage.couchNullResult());
		}
		
		// Handles the result depending to the C8oresponseListener
		try {
			response = handleFullSyncResponse(response, listener);
			return response;
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.fullSyncHandleResponse(), e);
		}
	}

    /**
     *
     * @param response
     * @param listener
     * @return
     * @throws C8oException Failed to parse response to JSON or XML.
     */
    protected Object handleFullSyncResponse(Object response, C8oResponseListener listener) throws C8oException {
        return response;
    }
	
	/**
	 * Returns the requested document.
	 * 
	 * @param databaseName
	 * @param parameters
	 * @return
	 * @throws C8oException 
	 */
	abstract Object handleGetDocumentRequest(String databaseName, String docid, Map<String, Object> parameters) throws C8oException;

	//*** TAG DeleteDocument ***//

	/**
	 * Deletes an existing document from the local database.
	 *
	 * @param databaseName
	 * @param parameters
	 * @return
	 * @throws C8oException
	 * @throws C8oRessourceNotFoundException
	 */
	abstract Object handleDeleteDocumentRequest(String databaseName, String docid, Map<String, Object> parameters) throws C8oException, C8oRessourceNotFoundException;

	//*** TAG PostDocument ***//

	/**
	 * Creates a new document or a new revision of the existing document in the local database.
	 *
	 * @param databaseName
	 * @param parameters
	 * @return
	 * @throws C8oException
	 */
	abstract Object handlePostDocumentRequest(String databaseName, FullSyncPolicy fullSyncPolicy, Map<String, Object> parameters) throws C8oException;

	//*** TAG GetAllDocuments ***//

	/**
	 * Returns all the documents in a given database.
	 *
	 * @param databaseName
	 * @param parameters
	 * @return
	 * @throws C8oException
	 */
	abstract Object handleAllDocumentsRequest(String databaseName, Map<String, Object> parameters) throws C8oException;

	//*** TAG GetView ***//

	/**
	 * Executes the specified view function from the local database.
	 *
	 * @param databaseName
	 * @param parameters
	 * @return
	 * @throws C8oException
	 * @throws C8oRessourceNotFoundException
	 */
	abstract Object handleGetViewRequest(String databaseName, String ddoc, String view, Map<String, Object> parameters) throws C8oException, C8oRessourceNotFoundException;

	//*** TAG Sync, ReplicatePull, ReplicatePush ***//

	/**
	 * Synchronizes the local database with the remote one and synchronizes the remote database with the local one.
	 *
	 * @param databaseName
	 * @param parameters
	 * @param c8oResponseListener
	 * @return
	 */
	abstract Object handleSyncRequest(String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException;

	/**
	 * Synchronizes the local database with the remote one.
	 *
	 * @param databaseName
	 * @param parameters
	 * @param c8oResponseListener
	 * @return
	 */
	abstract Object handleReplicatePullRequest(String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException;

	/**
	 * Synchronizes the remote database with the local one.
	 *
	 * @param databaseName
	 * @param parameters
	 * @param c8oResponseListener
	 * @return
	 */
	abstract Object handleReplicatePushRequest(String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException;

	//*** TAG Reset ***//

	/**
	 * Deletes all documents of the local database.
	 *
	 * @param databaseName
	 * @return
	 * @throws C8oException
	 */
	abstract Object handleResetDatabaseRequest(String databaseName) throws C8oException;

	abstract Object handleCreateDatabaseRequest(String databaseName) throws C8oException;

	abstract Object handleDestroyDatabaseRequest(String databaseName) throws C8oException;

    abstract C8oLocalCacheResponse getResponseFromLocalCache(String c8oCalRequestIdentifier) throws C8oException, C8oUnavailableLocalCacheException;

    abstract void saveResponseToLocalCache(String c8oCalRequestIdentifier, C8oLocalCacheResponse localCacheResponse) throws C8oException;

	/**
	 * Checks if request parameters correspond to a fullSync request.
	 */
	static boolean isFullSyncRequest(Map<String, Object> requestParameters) {
		// Check if there is one parameter named "__project" and if its value begin with "fs://"
		String parameterValue = C8oUtils.getParameterStringValue(requestParameters, C8o.ENGINE_PARAMETER_PROJECT, false);
		if (parameterValue != null) {
			return parameterValue.startsWith(FULL_SYNC_PROJECT);
		}
		return false;
	}
}
