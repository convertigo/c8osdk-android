package com.convertigo.clientsdk.fullsync;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;

import android.content.Context;

import com.convertigo.clientsdk.C8o;
import com.convertigo.clientsdk.C8oSettings;
import com.convertigo.clientsdk.VoidResponse;
import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oExceptionMessage;
import com.convertigo.clientsdk.exception.C8oRessourceNotFoundException;
import com.convertigo.clientsdk.fullsync.FullSyncEnum.FullSyncDeleteDocumentParameter;
import com.convertigo.clientsdk.fullsync.FullSyncEnum.FullSyncGetDocumentParameter;
import com.convertigo.clientsdk.fullsync.FullSyncEnum.FullSyncGetViewParameter;
import com.convertigo.clientsdk.fullsync.FullSyncEnum.FullSyncPolicy;
import com.convertigo.clientsdk.fullsync.FullSyncEnum.FullSyncPostDocumentParameter;
import com.convertigo.clientsdk.fullsync.FullSyncEnum.FullSyncRequestParameter;
import com.convertigo.clientsdk.fullsync.FullSyncEnum.FullSyncRequestable;
import com.convertigo.clientsdk.fullsync.FullSyncResponse.FullSyncDefaultResponse;
import com.convertigo.clientsdk.fullsync.FullSyncResponse.FullSyncDocumentOperationResponse;
import com.convertigo.clientsdk.listener.C8oFullSyncResponseListener;
import com.convertigo.clientsdk.listener.C8oJSONResponseListener;
import com.convertigo.clientsdk.listener.C8oResponseListener;
import com.convertigo.clientsdk.listener.C8oXMLResponseListener;
import com.convertigo.clientsdk.util.C8oUtils;
import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Database.TDContentOptions;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.Reducer;
import com.couchbase.lite.Revision;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.View;
import com.couchbase.lite.View.TDViewCollation;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.internal.RevisionInternal;
import com.couchbase.lite.javascript.JavaScriptViewCompiler;

public class FullSyncInterface {
	
	//*** TAG Constants ***//
	
	/**
	 * 
	 */
	private final static String FULL_SYNC_URL_PATH = "/fullsync/";
	/**
	 * The project requestable value to execute a fullSync request.
	 */
	public final static String FULL_SYNC_PROJECT = "fs://";
	/**
	 * 
	 */
	public final static String FULL_SYNC__ID = "_id";
	/**
	 * 
	 */
	public final static String FULL_SYNC__REV = "_rev";
	public final static String FULL_SYNC__ATTACHMENTS = "_attachments";
	
	private final static String ATTACHMENT_PROPERTY_KEY_CONTENT_URL = "content_url";
	
	//*** TAG Attributes ***//

	/**
	 * Used to log.
	 */
	private C8o c8o;
	/**
	 * Manages a collection of CBL Database instances.
	 */
	private Manager manager; 
	/**
	 * List of couch databases and their replications.
	 */
	private List<FullSyncDatabase> fullSyncDatabases;
	/**
	 * The base of fullSync database URL.
	 */
	private String fullSyncDatabaseUrlBase;
	/**
	 * The default fullSync database name.
	 */
	private String defaultFullSyncDatabaseName;
	
	//*** TAG Constructors ***//

	/**
	 * Creates an interface allowing to run fullSync requests.
	 * 
	 * @param context
	 * @param c8o
	 * @param endpointFirstPart - The first part of the endpoint (before '/projects/...').
	 * @param settings
	 * @throws C8oException 
	 */
	public FullSyncInterface(Context context, C8o c8o, String endpointFirstPart, C8oSettings settings) throws C8oException {
    	// Setup a Javascript view compiler
		View.setCompiler(new JavaScriptViewCompiler());
		
    	this.c8o = c8o;
		this.fullSyncDatabases = new ArrayList<FullSyncDatabase>();
		this.fullSyncDatabaseUrlBase = endpointFirstPart + FullSyncInterface.FULL_SYNC_URL_PATH;
		this.defaultFullSyncDatabaseName = settings.getDefaultDatabaseName();
        try {
			this.manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
		} catch (IOException e) {
			throw new C8oException(C8oExceptionMessage.initCouchManager(), e);
		}
		
		// If the default fullSync database name is specified then adds the related database to the list
		if (this.defaultFullSyncDatabaseName != null) {
	        try {
				this.fullSyncDatabases.add(new FullSyncDatabase(this.c8o, this.manager, this.defaultFullSyncDatabaseName, this.fullSyncDatabaseUrlBase));
			} catch (C8oException e) {
				// ???
				throw e;
			}
		}
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
	public Object handleFullSyncRequest(List<NameValuePair> parameters, C8oResponseListener listener) throws C8oException {
		
		//*** Checks parameters validity ***//
		
		// Checks if this is really a fullSync request (even if this is normally already checked)
		String projectParameterValue = C8oUtils.getParameterStringValue(parameters, C8o.ENGINE_PARAMETER_PROJECT, false);
		if (projectParameterValue == null) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentMissParameter(C8o.ENGINE_PARAMETER_PROJECT));
		}
		if (!projectParameterValue.startsWith(FULL_SYNC_PROJECT)) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterProjectRequestableFullSync(projectParameterValue));
		}
		// Gets the sequence parameter to know which fullSync requestable to use
		String fullSyncRequestableValue = C8oUtils.getParameterStringValue(parameters, C8o.ENGINE_PARAMETER_SEQUENCE, false);
		if (fullSyncRequestableValue == null) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentMissParameter(C8o.ENGINE_PARAMETER_SEQUENCE));
		}
		
		// Gets the database name if this is not specified then if takes the default database name
		String databaseName = projectParameterValue.substring(FULL_SYNC_PROJECT.length());
		if (databaseName.length() < 1) {
			if (this.defaultFullSyncDatabaseName == null) {
				throw new IllegalArgumentException(C8oExceptionMessage.invalidParamterValue(C8o.ENGINE_PARAMETER_PROJECT, null));
			}
			databaseName = this.defaultFullSyncDatabaseName;
		}
		// Gets the database in the list, if it does not exist then creates it
		FullSyncDatabase fullSyncDatabase;
		try {
			fullSyncDatabase = this.getOrCreateFullSyncDatabase(databaseName);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.fullSyncGetOrCreateDatabase(databaseName), e);
		}
		// If the database is closed the re-open it
		if (!fullSyncDatabase.database.isOpen()) {
			fullSyncDatabase.database.open();
		}

		// Handles the fullSync request depending to the requestable
		FullSyncRequestable fullSyncRequestable = FullSyncRequestable.getFullSyncRequestable(fullSyncRequestableValue);
		if (fullSyncRequestable == null) {
			throw new IllegalArgumentException(C8oExceptionMessage.unhandledFullSyncRequestable(fullSyncRequestableValue));
		}
		
		Object response;
		try {
			response = fullSyncRequestable.handleFullSyncRequest(this, fullSyncDatabase, parameters, listener);
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
			return FullSyncInterface.handleFullSyncResponseDependingToListener(response, listener, c8o, fullSyncRequestableValue);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.fullSyncHandleResponse(), e);
		}
	}
	
	//*** TAG GetDocument ***//
	
	/**
	 * Returns the requested document.
	 * 
	 * @param fullsyncDatabase
	 * @param parameters
	 * @return
	 * @throws C8oException 
	 */
	Document handleGetDocumentRequest(FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters) {
		Database database = fullsyncDatabase.getDatabase();
		
		// Gets the docid parameter value
		String docidParameterValue = C8oUtils.getParameterStringValue(parameters, FullSyncGetDocumentParameter.DOCID.name, false);
		if (docidParameterValue == null) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentMissParameter(FullSyncGetDocumentParameter.DOCID.name));
		}
		
		// Gets the document from the local database
		Document document = database.getExistingDocument(docidParameterValue);
		
		// If there are attachments, compute for each one the url to local storage and add it to the attachment descriptor
		if (document != null) {
			@SuppressWarnings("unchecked")
			LinkedHashMap<String, Object> attachments = (LinkedHashMap<String, Object>)document.getProperty(FULL_SYNC__ATTACHMENTS);
			if (attachments != null) {
				Revision rev = document.getCurrentRevision();
				for (String attachmentName: attachments.keySet()) {
					//this.c8o.c8oLogger.log(4, "Attachment name is : " + key);
					Attachment attachment  = rev.getAttachment(attachmentName);
					URL url  = attachment.getContentURL();
					//this.c8o.c8oLogger.log(4, "Attachment name URL is : " + url.toString());
					@SuppressWarnings("unchecked")
					LinkedHashMap<String, Object> attachmentDesc = (LinkedHashMap<String, Object>)attachments.get(attachmentName);
					attachmentDesc.put(ATTACHMENT_PROPERTY_KEY_CONTENT_URL, URLDecoder.decode(url.toString()));
				}
			}
		}		
		return document;
	}
	
	//*** TAG DeleteDocument ***//
	
	/**
	 * Deletes an existing document from the local database.
	 * 
	 * @param fullsyncDatabase
	 * @param parameters
	 * @return
	 * @throws C8oException
	 * @throws C8oRessourceNotFoundException 
	 */
	FullSyncDocumentOperationResponse handleDeleteDocumentRequest(FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters) throws C8oException, C8oRessourceNotFoundException {
		// Gets the docid parameter value
		String docidParameterValue = C8oUtils.getParameterStringValue(parameters, FullSyncDeleteDocumentParameter.DOCID.name, false);
		if (docidParameterValue == null) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentMissParameter(FullSyncDeleteDocumentParameter.DOCID.name));
		}
		String revParameterValue = C8oUtils.getParameterStringValue(parameters, FullSyncDeleteDocumentParameter.REV.name, false);
		
		Document document = fullsyncDatabase.database.getExistingDocument(docidParameterValue);
		if (document == null) {
			throw new C8oRessourceNotFoundException(C8oExceptionMessage.toDo());
		}
		
		String documentRevision = document.getCurrentRevisionId();
		
		// If the revision is specified then checks if this is the right revision
		if (revParameterValue != null && !revParameterValue.equals(documentRevision)) {
			throw new C8oRessourceNotFoundException(C8oExceptionMessage.couchRequestInvalidRevision());
		}
		boolean deleted;
		try {
			deleted = document.delete();
		} catch (CouchbaseLiteException e) {
			throw new C8oException(C8oExceptionMessage.couchRequestDeleteDocument(), e);
		}
		
		return FullSyncResponse.createFullSyncDocumentOperationResponse(docidParameterValue, documentRevision, deleted);
	}
	
	//*** TAG PostDocument ***//
	
	/**
	 * Creates a new document or a new revision of the existing document in the local database.
	 * 
	 * @param fullsyncDatabase
	 * @param parameters
	 * @return
	 * @throws C8oException 
	 */
	FullSyncDocumentOperationResponse handlePostDocumentRequest(FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters) throws C8oException {
		// Gets the policy parameter
		FullSyncPolicy fullSyncPolicy = null; 
		String policyParameterValue = C8oUtils.getParameterStringValue(parameters, FullSyncPostDocumentParameter.POLICY.name, false);
		// Found the policy corresponding to the parameter value if it exists
		if (policyParameterValue != null) {
			int i = 0;
			FullSyncPolicy[] fullSyncPolicyValues = FullSyncPolicy.values();
			while (i < fullSyncPolicyValues.length && fullSyncPolicy == null) {
				if (policyParameterValue.equals(fullSyncPolicyValues[i].value)) {
					fullSyncPolicy = FullSyncPolicy.values()[i];
				}
				i++;
			}
		}
		if (fullSyncPolicy == null) {
			fullSyncPolicy = FullSyncPolicy.NONE;
		}
		// Gets the subkey separator parameter
		String subkeySeparatorParameterValue = C8oUtils.getParameterStringValue(parameters, FullSyncPostDocumentParameter.SUBKEY_SEPARATOR.name, false);
		if (subkeySeparatorParameterValue == null) {
			subkeySeparatorParameterValue = ".";
		}
		
		// Filters and modifies wrong properties
		LinkedHashMap<String, Object> newProperties = new LinkedHashMap<String, Object>();
		for (NameValuePair parameter : parameters) {
			String parameterName = parameter.getName();
			
            // Ignores parameters beginning with "__" or "_use_" 
			if (parameterName.startsWith("__")) {
				continue;
			}
			if (parameterName.startsWith("_use_")) {
				continue;
			}
			
			Object objectParameterValue;
			try {
				objectParameterValue = C8oUtils.getParameterObjectValue(parameter);
			} catch (C8oException e) {
				throw new C8oException(C8oExceptionMessage.getNameValuePairObjectValue(parameter), e);
			}
			
			// Checks if the parameter name is splittable
			String[] paths = parameterName.split(Pattern.quote(subkeySeparatorParameterValue));
			if (paths.length > 1) {
                // The first substring becomes the key
				parameterName = paths[0];
                // Next substrings create a hierarchy which will becomes json subkeys  
				int count = paths.length - 1;
				while (count > 0) {
					LinkedHashMap<String, Object> tmpObject =  new LinkedHashMap<String, Object>();
					tmpObject.put(paths[count], objectParameterValue);
					objectParameterValue = tmpObject;
					count--;
				}
			}
			
			newProperties.put(parameterName, objectParameterValue);
		}
		
		// Execute the query depending to the policy
		Document createdDocument;
		try {
			createdDocument = fullSyncPolicy.postDocument(fullsyncDatabase, newProperties);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.postDocument(), e);
		}
		
		String documentId = createdDocument.getId();
		String currentRevision = createdDocument.getCurrentRevisionId();
		return FullSyncResponse.createFullSyncDocumentOperationResponse(documentId, currentRevision, true);
	}
	
	//*** TAG GetAllDocuments ***//
	
	/**
	 * Returns all the documents in a given database.
	 * 
	 * @param fullsyncDatabase
	 * @param parameters
	 * @return
	 * @throws C8oException 
	 */
	QueryEnumerator handleAllDocumentsRequest(FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters) throws C8oException {
		Database database = fullsyncDatabase.getDatabase();

		// Creates the fullSync query and add parameters to it
		Query query = database.createAllDocumentsQuery();
		try {
			FullSyncInterface.addParametersToQuery(query, parameters);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.addparametersToQuery(), e);
		}
		
		// Runs the query
		QueryEnumerator result;
		try {
			result = query.run();
		} catch (CouchbaseLiteException e) {
			throw new C8oException(C8oExceptionMessage.couchRequestAllDocuments(), e);
		}
		return result;
	}
	
	//*** TAG GetView ***//
	
	/**
	 * Executes the specified view function from the local database.
	 * 
	 * @param fullsyncDatabase
	 * @param parameters
	 * @return
	 * @throws C8oException 
	 * @throws C8oRessourceNotFoundException 
	 */
	QueryEnumerator handleGetViewRequest(FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters) throws C8oException, C8oRessourceNotFoundException {
		Database database = fullsyncDatabase.getDatabase();
		
		// Gets the view name parameter value
		String viewParameterValue = C8oUtils.getParameterStringValue(parameters, FullSyncGetViewParameter.VIEW.name, false);
		if (viewParameterValue == null) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentMissParameter(FullSyncGetViewParameter.VIEW.name));
		}
		// Gets the design doc parameter value
		String ddocParameterValue = C8oUtils.getParameterStringValue(parameters, FullSyncGetViewParameter.DDOC.name, false);
		
		// Gets the view depending to its programming language (Javascript / Java)
		View view;
		if (ddocParameterValue != null) {
			// Javascript view
			view = this.checkAndCreateJavaScriptView(database, ddocParameterValue, viewParameterValue);
		} else {
			// Java view
			view = database.getView(viewParameterValue);
		}
		if (view == null) {
			throw new C8oRessourceNotFoundException(C8oExceptionMessage.illegalArgumentNotFoundFullSyncView(viewParameterValue, database.getName()));
		}
		
		// Creates the fullSync query and add parameters to it
		Query query = view.createQuery();
		try {
			FullSyncInterface.addParametersToQuery(query, parameters);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.addparametersToQuery(), e);
		}
		
		// Runs the query
		QueryEnumerator result;
		try {
			result = query.run();
		} catch (CouchbaseLiteException e) {
			throw new C8oException(C8oExceptionMessage.couchRequestGetView(), e);
		}
		return result;
	}
	
	//*** TAG Sync, ReplicatePull, ReplicatePush ***//
	
	/**
	 * Synchronizes the local database with the remote one and synchronizes the remote database with the local one.
	 * 
	 * @param fullsyncDatabase
	 * @param parameters
	 * @param c8oResponseListener
	 * @return
	 */
	VoidResponse handleSyncRequest(FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) {
		fullsyncDatabase.startAllReplications(c8oResponseListener, parameters);
		return VoidResponse.getInstance();
	}
	
	/**
	 * Synchronizes the local database with the remote one.
	 * 
	 * @param fullsyncDatabase
	 * @param parameters
	 * @param c8oResponseListener
	 * @return
	 */
	VoidResponse handleReplicatePullRequest(FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) {
		fullsyncDatabase.startPullReplication(c8oResponseListener, parameters);
		return VoidResponse.getInstance();
	}
	
	/**
	 * Synchronizes the remote database with the local one.
	 * 
	 * @param fullsyncDatabase
	 * @param parameters
	 * @param c8oResponseListener
	 * @return
	 */
	VoidResponse handleReplicatePushRequest(FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) {
		fullsyncDatabase.startPushReplication(c8oResponseListener, parameters);
		return VoidResponse.getInstance();
	}
	
	//*** TAG Reset ***//
	
	/**
	 * Deletes all documents of the local database.
	 * 
	 * @param fullsyncDatabase
	 * @param parameters
	 * @return
	 * @throws C8oException
	 */
	FullSyncDefaultResponse handleResetDatabaseRequest(FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters) throws C8oException {
		try {
			fullsyncDatabase.database.delete();
		} catch (CouchbaseLiteException e) {
			throw new C8oException(C8oExceptionMessage.couchRequestResetDatabase(), e);
		}
		return FullSyncResponse.createFullSyncDefaultResponse(true);
	}

	//*** TAG Javascript view ***//
	
	/**
	 * Compiles a javascript view.<br/>
	 * <br/>
	 * Code derived from CBLite<br/>
	 * https://github.com/couchbase/couchbase-lite-java-core/blob/master/src/main/java/com/couchbase/lite/router/Router.java
	 * 
	 * @param db
	 * @param viewName
	 * @param viewProps
	 * @return
	 */
	private View compileView(Database db, String viewName, Map<String, Object> viewProps) {
		String language = (String) viewProps.get("language");
		if (language == null) {
		    language = "javascript";
		}
		
		String mapSource = (String) viewProps.get("map");
		if (mapSource == null) {
		    return null;
		}
		
		Mapper mapBlock = View.getCompiler().compileMap(mapSource, language);
		if (mapBlock == null) {
	    	// C8oLogger.log("View " +viewName + " has unknown map function:" +mapSource, Log.ERROR);
		    return null;
		}
		
		String reduceSource = (String) viewProps.get("reduce");
		Reducer reduceBlock = null;
		if (reduceSource != null) {
		    reduceBlock = View.getCompiler().compileReduce(reduceSource, language);
		    if (reduceBlock == null) {
		    	// C8oLogger.log("View " +viewName + " has unknown reduce function:" +reduceBlock, Log.ERROR);
		        return null;
		    }
		}
		
		View view = db.getView(viewName);
		view.setMapReduce(mapBlock, reduceBlock, "1");
		String collation = (String) viewProps.get("collation");
		if ("raw".equals(collation)) {
		    view.setCollation(TDViewCollation.TDViewCollationRaw);
		}
		return view;
	}

	/**
	 * Checks is a javascript view is not already compiled and if not, Gets it from design document and compile it.<br/>
	 * <br/>
	 * Code derived from CBLite<br/>
	 * https://github.com/couchbase/couchbase-lite-java-core/blob/master/src/main/java/com/couchbase/lite/router/Router.java
	 *  
	 * @param database
	 * @param designDoc
	 * @param viewName
	 */
	@SuppressWarnings("unchecked")
	private View checkAndCreateJavaScriptView(Database database, String designDoc, String viewName) {
		String tdViewName = String.format("%s/%s", designDoc, viewName);
		View view = database.getExistingView(tdViewName);
        if (view == null || view.getMap() == null) {
            // No TouchDB view is defined, or it hasn't had a map block assigned
            // Searches in the design document if there is a CouchDB view definition we can compile
            RevisionInternal rev = database.getDocumentWithIDAndRev(String.format("_design/%s", designDoc), null, EnumSet.noneOf(TDContentOptions.class));
            if (rev == null) {
			    // C8oLogger.log("Document : " + designDoc + " not found", Log.ERROR);
			    return null;
            }
            Map<String, Object> views = (Map<String, Object>) rev.getProperties().get("views");
            Map<String, Object> viewProps = (Map<String, Object>) views.get(viewName);
            if (viewProps == null) {
			    // C8oLogger.log("view : " + tdViewName + " not found", Log.ERROR);
			    return null;
            }
            // If there is a CouchDB view, see if it can be compiled from source:
            view = compileView(database, tdViewName, viewProps);
            if (view == null) {
			    // C8oLogger.log("Unable to compile view : " + tdViewName, Log.ERROR);
			    return null;
            }
            return view;
        }
        return view;
	}
	
	//*** TAG Other ***//
	
	/**
	 * Adds known parameters to the fullSync query.
	 * 
	 * @param query
	 * @param parameters
	 * @throws C8oException 
	 */
	private static void addParametersToQuery(Query query, List<NameValuePair> parameters) throws C8oException  {
		for (FullSyncRequestParameter fullSyncParameter : FullSyncRequestParameter.values()) {
			Object objectParameterValue;
			if (fullSyncParameter.isJson) {
				try {
					objectParameterValue = C8oUtils.getParameterObjectValue(parameters, fullSyncParameter.name, true);
				} catch (C8oException e) {
					throw new C8oException(C8oExceptionMessage.getNameValuePairObjectValue(fullSyncParameter.name), e);
				}
			} else {
				objectParameterValue = C8oUtils.getParameterStringValue(parameters, fullSyncParameter.name, true);
			}
			// If the parameter is specified
			if (objectParameterValue != null) {
				fullSyncParameter.addToQuery(query, objectParameterValue);
			}
		}
	}
	
	/**
	 * Checks if request parameters correspond to a fullSync request.
	 * 
	 * @param requestParameters
	 * @return
	 * @throws C8oException 
	 */
	public static boolean isFullSyncRequest(List<NameValuePair> requestParameters) {
		// Check if there is one parameter named "__project" and if its value begin with "fs://"
		String parameterValue = C8oUtils.getParameterStringValue(requestParameters, C8o.ENGINE_PARAMETER_PROJECT, false);
		if (parameterValue != null) {
			return parameterValue.startsWith(FULL_SYNC_PROJECT);
		}
		return false;
	}
	
	/**
	 * Returns the database with this name in the list.<br/>
	 * If it does not already exist yet then creates it and adds it to the list.
	 * 
	 * @param databaseName
	 * @return
	 * @throws C8oException Failed to create a new fullSync database.
	 */
	private FullSyncDatabase getOrCreateFullSyncDatabase(String databaseName) throws C8oException {
		for (FullSyncDatabase fullSyncDatabase : this.fullSyncDatabases) {
			if (fullSyncDatabase.getDatabaseName().equals(databaseName)) {
				return fullSyncDatabase;
			}
		}
		FullSyncDatabase fullSyncDatabase;
		try {
			fullSyncDatabase = new FullSyncDatabase(c8o, this.manager, databaseName, this.fullSyncDatabaseUrlBase);
		} catch (C8oException e) {
			// ???
			throw e;
		}
		this.fullSyncDatabases.add(fullSyncDatabase);
		return fullSyncDatabase;
	}
	
	@SuppressWarnings("unchecked")
	static void mergeProperties(Map<String, Object> newProperties, Map<String, Object> oldProperties) {
		// Iterates on old document keys
		Iterator<Entry<String, Object>> oldDocumentIterator = oldProperties.entrySet().iterator();
		while (oldDocumentIterator.hasNext()) {
			// Gets old document key and value
			Entry<String, Object> oldDocumentEntry = oldDocumentIterator.next();
			String oldDocumentKey = oldDocumentEntry.getKey();
			Object oldDocumentValue = oldDocumentEntry.getValue();
			
			// Checks if the new document contains the same key
			if (newProperties.containsKey(oldDocumentKey)) {
				// Get the new document value
				Object newDocumentValue = newProperties.get(oldDocumentKey);
				if (newDocumentValue instanceof Map<?, ?> && oldDocumentValue instanceof Map<?, ?>) {
					FullSyncInterface.mergeProperties((Map<String, Object>) newDocumentValue, (Map<String, Object>) oldDocumentValue);
				} else if (newDocumentValue instanceof ArrayList && oldDocumentValue instanceof ArrayList) {
					FullSyncInterface.mergeArrayProperties((ArrayList<Object>) newDocumentValue, (ArrayList<Object>) oldDocumentValue);
				} else {
					// If the new document has the same key but its value is not the same type than the old one or if their type are "simple"
					// Does nothing cause the right value is the new one
				}
			} else {
				// If the new document does not contain the key then adds it
				newProperties.put(oldDocumentKey, oldDocumentValue);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void mergeArrayProperties(ArrayList<Object> newArray, ArrayList<Object> oldArray) {
		int newArraySize = newArray.size();
		int oldArraySize = oldArray.size();
		
		// Iterates on old array values
		for (int i = 0; i < oldArraySize; i++) {
			// Gets new and old values
			Object newArrayValue = null;
			if (i < newArraySize) {
				newArrayValue = newArray.get(i);
			}
			Object oldArrayValue = oldArray.get(i);
			
			if (newArrayValue != null) {
				if (newArrayValue instanceof Map<?, ?> && oldArrayValue instanceof Map<?, ?>) {
					FullSyncInterface.mergeProperties((Map<String, Object>) newArrayValue, (Map<String, Object>) oldArrayValue);
				} else if (newArrayValue instanceof ArrayList && oldArrayValue instanceof ArrayList) {
					FullSyncInterface.mergeArrayProperties((ArrayList<Object>) newArrayValue, (ArrayList<Object>) oldArrayValue);
				} else {
					// If the new array value is not the same type than the old one or if their type are "simple"
					// Does nothing cause the right value is the new one
				}
			} else {
				// If the new array value is null then it means that it size is reach so we can add objects at its end
				newArray.add(oldArrayValue);
			}
		}
	}
	
	/**
	 * 
	 * @param response
	 * @param listener
	 * @param c8o
	 * @param fullSyncRequestableValue
	 * @return
	 * @throws C8oException Failed to parse response to JSON or XML.
	 */
	private static Object handleFullSyncResponseDependingToListener(Object response, C8oResponseListener listener, C8o c8o, String fullSyncRequestableValue) throws C8oException {
		if (response instanceof VoidResponse) {
			return response;
		}
		
		if (listener instanceof C8oJSONResponseListener) {
			//*** Document (GetDocument) ***//
			if (response instanceof Document) {
				return FullSyncTranslator.documentToJSON((Document) response);
			} 
			//*** FullSyncDocumentOperationResponse (DeleteDocument, PostDocument) ***//
			else if (response instanceof FullSyncDocumentOperationResponse) {
				return FullSyncTranslator.fullSyncDocumentOperationResponseToJSON((FullSyncDocumentOperationResponse) response);
			}
			//*** QueryEnumerator (GetAllDocuments, GetView) ***// 
			else if (response instanceof QueryEnumerator) {
				try {
					return FullSyncTranslator.queryEnumeratorToJSON((QueryEnumerator) response);
				} catch (C8oException e) {
					throw new C8oException(C8oExceptionMessage.queryEnumeratorToJSON(), e);
				}
			} 
			//*** FullSyncDefaultResponse (Sync, ReplicatePull, ReplicatePush, Reset) ***//
			else if (response instanceof FullSyncDefaultResponse) {
				return FullSyncTranslator.fullSyncDefaultResponseToJSON((FullSyncDefaultResponse) response);
			} else {
				throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentIncompatibleListener
						(listener.getClass().getName(), response.getClass().getName()));
			}
		} else if (listener instanceof C8oXMLResponseListener) {
			//*** Document (GetDocument) ***//
			if (response instanceof Document) {
				return FullSyncTranslator.documentToXML((Document) response, c8o.getDocumentBuilder());
			} 
			//*** FullSyncDocumentOperationResponse (DeleteDocument, PostDocument) ***//
			else if (response instanceof FullSyncDocumentOperationResponse) {
				return FullSyncTranslator.fullSyncDocumentOperationResponseToXML((FullSyncDocumentOperationResponse) response, c8o.getDocumentBuilder());
			}
			//*** QueryEnumerator (GetAllDocuments, GetView) ***// 
			else if (response instanceof QueryEnumerator) {
				try {
					return FullSyncTranslator.queryEnumeratorToXML((QueryEnumerator) response, c8o.getDocumentBuilder());
				} catch (C8oException e) {
					throw new C8oException(C8oExceptionMessage.queryEnumeratorToXML(), e);
				}
			} 
			//*** FullSyncDefaultResponse (Sync, ReplicatePull, ReplicatePush, Reset) ***//
			else if (response instanceof FullSyncDefaultResponse) {
				return FullSyncTranslator.fullSyncDefaultResponseToXML((FullSyncDefaultResponse) response, c8o.getDocumentBuilder());
			} else {
				throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentIncompatibleListener
						(listener.getClass().getName(), response.getClass().getName()));
			}		
		} else if (listener instanceof C8oFullSyncResponseListener) {
			//*** Document (GetDocument) ***// || //*** QueryEnumerator (GetAllDocuments, GetView) ***// 
			if (response instanceof Document || response instanceof QueryEnumerator) {
				return response;
			} else {
				throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentIncompatibleListener
						(listener.getClass().getName(), response.getClass().getName()));
			}		
		} else {
			throw new IllegalArgumentException(C8oExceptionMessage.unhandledListenerType(listener.getClass().getName()));
		}
	}
	
	/**
	 * Used for local cache.
	 * 
	 * @param databaseName
	 * @param documentId
	 * @return
	 * @throws C8oException
	 */
	public Document getDocucmentFromDatabase(C8o c8o, String databaseName, String documentId) throws C8oException {
		FullSyncDatabase fullSyncDatabase;
		try {
			fullSyncDatabase = this.getOrCreateFullSyncDatabase(databaseName);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.fullSyncGetOrCreateDatabase(databaseName), e);
		}
		return fullSyncDatabase.getDatabase().getDocument(documentId);
	}
	
	public static void overrideDocument(Document document, Map<String, Object> properties) throws C8oException {
		// Must add the current revision to the properties
		SavedRevision currentRevision = document.getCurrentRevision();
		if (currentRevision != null) {
			properties.put(FullSyncInterface.FULL_SYNC__REV, currentRevision.getId());
		}
		
		try {
			document.putProperties(properties);
		} catch (CouchbaseLiteException e) {
			throw new C8oException(C8oExceptionMessage.fullSyncPutProperties(properties), e);
		}
	}
	
}
