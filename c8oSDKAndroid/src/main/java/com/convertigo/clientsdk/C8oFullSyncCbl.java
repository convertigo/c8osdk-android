package com.convertigo.clientsdk;

import android.content.Context;

import com.convertigo.clientsdk.FullSyncEnum.FullSyncDeleteDocumentParameter;
import com.convertigo.clientsdk.FullSyncEnum.FullSyncPolicy;
import com.convertigo.clientsdk.FullSyncEnum.FullSyncRequestParameter;
import com.convertigo.clientsdk.FullSyncResponse.FullSyncDefaultResponse;
import com.convertigo.clientsdk.FullSyncResponse.FullSyncDocumentOperationResponse;
import com.convertigo.clientsdk.exception.C8oCouchbaseLiteException;
import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oRessourceNotFoundException;
import com.convertigo.clientsdk.exception.C8oUnavailableLocalCacheException;
import com.convertigo.clientsdk.listener.C8oResponseCblListener;
import com.convertigo.clientsdk.listener.C8oResponseJsonListener;
import com.convertigo.clientsdk.listener.C8oResponseListener;
import com.convertigo.clientsdk.listener.C8oResponseXmlListener;
import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.Reducer;
import com.couchbase.lite.Revision;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.internal.RevisionInternal;
import com.couchbase.lite.util.ZipUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by nicolasa on 01/12/2015.
 */
class C8oFullSyncCbl extends C8oFullSync {
    private final static String ATTACHMENT_PROPERTY_KEY_CONTENT_URL = "content_url";

    /**
     * Manages a collection of CBL Database instances.
     */
    private Manager manager;
    /**
     * List of couch databases and their replications.
     */
    private Map<String, C8oFullSyncDatabase> fullSyncDatabases;
    private Map<String, Set<C8oFullSyncChangeListener>> fullSyncChangeListeners;
    private Map<String, Database.ChangeListener> cblChangeListeners;
    private Map<String, String> viewDDocRev;

    public C8oFullSyncCbl() {
    }

    @Override
    public void Init(C8o c8o, Context context) throws C8oException {
        super.Init(c8o, context);

        fullSyncDatabases = new HashMap<String, C8oFullSyncDatabase>();
        fullSyncChangeListeners = new HashMap<String, Set<C8oFullSyncChangeListener>>();
        cblChangeListeners = new HashMap<String, Database.ChangeListener>();
        viewDDocRev = new HashMap<String, String>();
        try {
            manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            throw new C8oException(C8oExceptionMessage.initCouchManager(), e);
        }
    }

    /**
     * Returns the database with this name in the list.<br/>
     * If it does not already exist yet then creates it and adds it to the list.
     *
     * @param databaseName
     * @return
     * @throws C8oException Failed to create a new fullSync database.
     */
    private C8oFullSyncDatabase getOrCreateFullSyncDatabase(String databaseName) throws C8oException {
        String localDatabaseName = databaseName + localSuffix;
        if (!fullSyncDatabases.containsKey(localDatabaseName))
        {
            fullSyncDatabases.put(localDatabaseName, new C8oFullSyncDatabase(c8o, manager, databaseName, fullSyncDatabaseUrlBase, localSuffix));
            if (cblChangeListeners.containsKey(databaseName)) {
                fullSyncDatabases.get(localDatabaseName).getDatabase().addChangeListener(cblChangeListeners.get(databaseName));
            }
        }
        return fullSyncDatabases.get(localDatabaseName);
    }

    protected Object handleFullSyncResponse(Object response, C8oResponseListener listener) throws C8oException {
        response = super.handleFullSyncResponse(response, listener);
        if (response instanceof VoidResponse) {
            return response;
        }

        if (listener instanceof C8oResponseJsonListener) {
            //*** Document (GetDocument) ***//
            if (response instanceof Document) {
                return C8oFullSyncTranslator.documentToJson((Document) response);
            }
            //*** FullSyncDocumentOperationResponse (DeleteDocument, PostDocument) ***//
            else if (response instanceof FullSyncDocumentOperationResponse) {
                return C8oFullSyncTranslator.fullSyncDocumentOperationResponseToJson((FullSyncDocumentOperationResponse) response);
            }
            //*** QueryEnumerator (GetAllDocuments, GetView) ***//
            else if (response instanceof QueryEnumerator) {
                try {
                    return C8oFullSyncTranslator.queryEnumeratorToJson((QueryEnumerator) response);
                } catch (C8oException e) {
                    throw new C8oException(C8oExceptionMessage.queryEnumeratorToJSON(), e);
                }
            }
            //*** FullSyncDefaultResponse (Sync, ReplicatePull, ReplicatePush, Reset) ***//
            else if (response instanceof FullSyncDefaultResponse) {
                return C8oFullSyncTranslator.fullSyncDefaultResponseToJson((FullSyncDefaultResponse) response);
            }
        } else if (listener instanceof C8oResponseXmlListener) {
            //*** Document (GetDocument) ***//
            if (response instanceof Document) {
                return C8oFullSyncTranslator.documentToXML((Document) response, c8o.getDocumentBuilder());
            }
            //*** FullSyncDocumentOperationResponse (DeleteDocument, PostDocument) ***//
            else if (response instanceof FullSyncDocumentOperationResponse) {
                return C8oFullSyncTranslator.fullSyncDocumentOperationResponseToXML((FullSyncDocumentOperationResponse) response, c8o.getDocumentBuilder());
            }
            //*** QueryEnumerator (GetAllDocuments, GetView) ***//
            else if (response instanceof QueryEnumerator) {
                try {
                    return C8oFullSyncTranslator.queryEnumeratorToXml((QueryEnumerator) response, c8o.getDocumentBuilder());
                } catch (C8oException e) {
                    throw new C8oException(C8oExceptionMessage.queryEnumeratorToXML(), e);
                }
            }
            //*** FullSyncDefaultResponse (Sync, ReplicatePull, ReplicatePush, Reset) ***//
            else if (response instanceof FullSyncDefaultResponse) {
                return C8oFullSyncTranslator.fullSyncDefaultResponseToXml((FullSyncDefaultResponse) response, c8o.getDocumentBuilder());
            }
        } else if (listener instanceof C8oResponseCblListener) {
            //*** Document (GetDocument) ***// || //*** QueryEnumerator (GetAllDocuments, GetView) ***//
            if (response instanceof Document || response instanceof QueryEnumerator) {
                return response;
            }
        }

        return response;
    }

    @Override
    Document handleGetDocumentRequest(String databaseName, String docid, Map<String, Object> parameters) throws C8oException {
        C8oFullSyncDatabase fullSyncDatabase = getOrCreateFullSyncDatabase(databaseName);

        // Gets the document from the local database
        Document document = fullSyncDatabase.getDatabase().getExistingDocument(docid);

        // If there are attachments, compute for each one the url to local storage and add it to the attachment descriptor
        if (document != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attachments = (Map<String, Object>) document.getProperty(FULL_SYNC__ATTACHMENTS);

            if (attachments != null) {
                Revision rev = document.getCurrentRevision();

                for (String attachmentName: attachments.keySet()) {
                    Attachment attachment  = rev.getAttachment(attachmentName);
                    URL url  = attachment.getContentURL();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> attachmentDesc = (Map<String, Object>)attachments.get(attachmentName);
                    attachmentDesc.put(ATTACHMENT_PROPERTY_KEY_CONTENT_URL, URLDecoder.decode(url.toString()));
                }
            }
        } else {
            throw new C8oRessourceNotFoundException(C8oExceptionMessage.ressourceNotFound("requested document \"" + docid + "\""));
        }
        return document;
    }

    @Override
    FullSyncDocumentOperationResponse handleDeleteDocumentRequest(String databaseName, String docid, Map<String, Object> parameters) throws C8oException, C8oRessourceNotFoundException {
        C8oFullSyncDatabase fullSyncDatabase = getOrCreateFullSyncDatabase(databaseName);

        String revParameterValue = C8oUtils.getParameterStringValue(parameters, FullSyncDeleteDocumentParameter.REV.name, false);

        Document document = fullSyncDatabase.getDatabase().getExistingDocument(docid);
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

        return new FullSyncDocumentOperationResponse(docid, documentRevision, deleted);
    }

    @Override
    FullSyncDocumentOperationResponse handlePostDocumentRequest(String databaseName, FullSyncPolicy fullSyncPolicy, Map<String, Object> parameters) throws C8oException {
        C8oFullSyncDatabase fullSyncDatabase = getOrCreateFullSyncDatabase(databaseName);

        // Gets the subkey separator parameter
        String subkeySeparatorParameterValue = C8oUtils.getParameterStringValue(parameters, C8o.FS_SUBKEY_SEPARATOR, false);
        if (subkeySeparatorParameterValue == null) {
            subkeySeparatorParameterValue = ".";
        }

        // Filters and modifies wrong properties
        Map<String, Object> newProperties = new HashMap<String, Object>();
        for (Entry<String, Object> parameter : parameters.entrySet()) {
            String parameterName = parameter.getKey();

            // Ignores parameters beginning with "__" or "_use_"
            if (!parameterName.startsWith("__") && !parameterName.startsWith("_use_")) {
                Object objectParameterValue = parameter.getValue();

                try {
                    if (!isPlainObject(objectParameterValue)) {
                        // does nothing
                    } else if (objectParameterValue instanceof JSONObject) {
                        objectParameterValue = new ObjectMapper().readValue(objectParameterValue.toString(), LinkedHashMap.class);
                    } else if (objectParameterValue instanceof JSONArray) {
                        objectParameterValue = new ObjectMapper().readValue(objectParameterValue.toString(), ArrayList.class);
                    } else if (objectParameterValue instanceof Collection) {
                        objectParameterValue = new ObjectMapper().convertValue(objectParameterValue, ArrayList.class);
                    } else {
                        objectParameterValue = new ObjectMapper().convertValue(objectParameterValue, LinkedHashMap.class);
                    }
                } catch (Exception e) {
                    throw new C8oException(C8oExceptionMessage.invalidParameterValue(parameterName, objectParameterValue.toString()), e);
                }

                // Checks if the parameter name is splittable
                String[] paths = parameterName.split(Pattern.quote(subkeySeparatorParameterValue));

                if (paths.length > 1) {
                    // The first substring becomes the key
                    parameterName = paths[0];
                    // Next substrings create a hierarchy which will becomes json subkeys
                    int count = paths.length - 1;
                    while (count > 0) {
                        Map<String, Object> tmpObject = new HashMap<String, Object>();
                        tmpObject.put(paths[count], objectParameterValue);
                        objectParameterValue = tmpObject;
                        count--;
                    }
                    Object existProperty = newProperties.get(parameterName);
                    if (existProperty != null && existProperty instanceof Map) {
                        mergeProperties((Map) objectParameterValue, (Map) existProperty);
                    }
                }

                newProperties.put(parameterName, objectParameterValue);
            }
        }

        // Execute the query depending to the policy
        Document createdDocument = fullSyncPolicy.postDocument(fullSyncDatabase.getDatabase(), newProperties);
        String documentId = createdDocument.getId();
        String currentRevision = createdDocument.getCurrentRevisionId();
        return new FullSyncDocumentOperationResponse(documentId, currentRevision, true);
    }

    @Override
    Object handlePutAttachmentRequest(String databaseName, String docid, String attachmentName, String attachmentType, InputStream attachmentContent) throws C8oException {
        C8oFullSyncDatabase fullSyncDatabase = getOrCreateFullSyncDatabase(databaseName);

        // Gets the document from the local database
        Document document = fullSyncDatabase.getDatabase().getExistingDocument(docid);

        if (document != null) {
            UnsavedRevision newRev = document.getCurrentRevision().createRevision();
            newRev.setAttachment(attachmentName, attachmentType, attachmentContent);
            try {
                newRev.save();
            } catch (CouchbaseLiteException e) {
                throw new C8oCouchbaseLiteException("Unable to put the attachment " + attachmentName + " to the document " + docid + ".", e);
            }
        } else {
            throw new C8oRessourceNotFoundException(C8oExceptionMessage.toDo());
        }

        return new FullSyncDocumentOperationResponse(document.getId(), document.getCurrentRevisionId(), true);
    }

    @Override
    Object handleDeleteAttachmentRequest(String databaseName, String docid, String attachmentName) throws C8oException {
        C8oFullSyncDatabase fullSyncDatabase = getOrCreateFullSyncDatabase(databaseName);

        // Gets the document from the local database
        Document document = fullSyncDatabase.getDatabase().getExistingDocument(docid);

        if (document != null) {
            UnsavedRevision newRev = document.getCurrentRevision().createRevision();
            newRev.removeAttachment(attachmentName);
            try {
                newRev.save();
            } catch (CouchbaseLiteException e) {
                throw new C8oCouchbaseLiteException("Unable to delete the attachment " + attachmentName + " to the document " + docid + ".", e);
            }
        } else {
            throw new C8oRessourceNotFoundException(C8oExceptionMessage.toDo());
        }

        return new FullSyncDocumentOperationResponse(document.getId(), document.getCurrentRevisionId(), true);
    }

    @Override
    QueryEnumerator handleAllDocumentsRequest(String databaseName, Map<String, Object> parameters) throws C8oException {
        C8oFullSyncDatabase fullSyncDatabase = getOrCreateFullSyncDatabase(databaseName);

        // Creates the fullSync query and add parameters to it
        Query query = fullSyncDatabase.getDatabase().createAllDocumentsQuery();
        try {
            addParametersToQuery(query, parameters);
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

    @Override
    QueryEnumerator handleGetViewRequest(String databaseName, String ddocName, String viewName, Map<String, Object> parameters) throws C8oException, C8oRessourceNotFoundException {
        C8oFullSyncDatabase fullSyncDatabase = getOrCreateFullSyncDatabase(databaseName);

        // Gets the view depending to its programming language (Javascript / Java)
        View view;
        if (ddocName != null) {
            // Javascript view
            view = checkAndCreateJavaScriptView(fullSyncDatabase.getDatabase(), ddocName, viewName);
        } else {
            // Java view
            view = fullSyncDatabase.getDatabase().getView(viewName);
        }
        if (view == null) {
            throw new C8oRessourceNotFoundException(C8oExceptionMessage.illegalArgumentNotFoundFullSyncView(viewName, fullSyncDatabase.getLocalDatabaseName()));
        }

        // Creates the fullSync query and add parameters to it
        Query query = view.createQuery();
        try {
            addParametersToQuery(query, parameters);
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

    @Override
    VoidResponse handleSyncRequest(String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
        C8oFullSyncDatabase fullSyncDatabase = getOrCreateFullSyncDatabase(databaseName);

        fullSyncDatabase.startAllReplications(parameters, c8oResponseListener);
        return VoidResponse.getInstance();
    }

    @Override
    VoidResponse handleReplicatePullRequest(String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
        C8oFullSyncDatabase fullSyncDatabase = getOrCreateFullSyncDatabase(databaseName);

        fullSyncDatabase.startPullReplication(parameters, c8oResponseListener);
        return VoidResponse.getInstance();
    }

    @Override
    VoidResponse handleReplicatePushRequest(String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
        C8oFullSyncDatabase fullSyncDatabase = getOrCreateFullSyncDatabase(databaseName);

        fullSyncDatabase.startPushReplication(parameters, c8oResponseListener);
        return VoidResponse.getInstance();
    }

    @Override
    FullSyncDefaultResponse handleResetDatabaseRequest(String databaseName) throws C8oException {
        handleDestroyDatabaseRequest(databaseName);
        return handleCreateDatabaseRequest(databaseName);
    }

    @Override
    FullSyncDefaultResponse handleCreateDatabaseRequest(String databaseName) throws C8oException {
        C8oFullSyncDatabase fullSyncDatabase = getOrCreateFullSyncDatabase(databaseName);
        return new FullSyncDefaultResponse(true);
    }

    @Override
    FullSyncDefaultResponse handleDestroyDatabaseRequest(String databaseName) throws C8oException {
        getOrCreateFullSyncDatabase(databaseName).delete();

        String localDatabaseName = databaseName + localSuffix;
        if (fullSyncDatabases.containsKey(localDatabaseName)) {
            fullSyncDatabases.remove(localDatabaseName);
        }

        return new FullSyncDefaultResponse(true);
    }

    private void handleDownloadBulkRequestProgress(C8oResponseListener c8oResponseListener, long current, long total, boolean finished) {
        if (c8oResponseListener != null && c8oResponseListener instanceof C8oResponseProgressListener) {
            C8oProgress progress = new C8oProgress();
            progress.setCurrent(current);
            progress.setTotal(total);
            progress.setFinished(finished);
            progress.setStatus("downloadBulk");
            Map<String, Object> param = new HashMap<String, Object>();
            param.put(C8o.ENGINE_PARAMETER_PROGRESS, progress);
            ((C8oResponseProgressListener) c8oResponseListener).onProgressResponse(progress, param);
        }
    }

    @Override
    VoidResponse handleDownloadBulkRequest(String databaseName, Map<String, Object> parameters, final C8oResponseListener c8oResponseListener) throws C8oException {
        try {
            String localDabaseName = databaseName + localSuffix;

            String url = (String) parameters.get(C8o.FS_DB_ZIP_FILE);
            if (!url.contains("://")) {
                url = c8o.getEndpoint() + "/" + url;
            }

            c8o.log.info("handleDownloadBulkRequest before dl: " + url);

            HttpResponse response = c8o.httpInterface.handleRequest(new HttpGet(url));

            long length = -1;
            Header contentLength = response.getFirstHeader("Content-Length");
            try {
                length = Long.parseLong(contentLength.getValue());
            } catch (Exception e) { }

            File path =  new File(c8o.getContext().getFilesDir(), localDabaseName + ".cblite2");
            final InputStream content = response.getEntity().getContent();
            final long total = length;
            final long[] currentRead = new long[] {0};

            ZipUtils.unzip(new InputStream() {

                @Override
                public int available() throws IOException {
                    return content.available();
                }

                @Override
                public void close() throws IOException {
                    content.close();
                }

                @Override
                public void mark(int readlimit) {
                    content.mark(readlimit);
                }

                @Override
                public boolean markSupported() {
                    return content.markSupported();
                }

                @Override
                public int read(byte[] buffer) throws IOException {
                    int read = content.read(buffer);
                    handleDownloadBulkRequestProgress(c8oResponseListener, currentRead[0] += read, total, false);
                    return read;
                }

                @Override
                public int read(byte[] buffer, int offset, int length) throws IOException {
                    int read = content.read(buffer, offset, length);
                    handleDownloadBulkRequestProgress(c8oResponseListener, currentRead[0] += read, total, false);
                    return read;
                }

                @Override
                public synchronized void reset() throws IOException {
                    content.reset();
                }

                @Override
                public long skip(long byteCount) throws IOException {
                    return content.skip(byteCount);
                }

                @Override
                public int read() throws IOException {
                    int read = content.read();
                    handleDownloadBulkRequestProgress(c8oResponseListener, currentRead[0] += 1, total, false);
                    return read;
                }
            }, path);

            c8o.log.info("handleDownloadBulkRequest after dl : " + path.getAbsolutePath());
            getOrCreateFullSyncDatabase(databaseName).initBulk();
            handleDownloadBulkRequestProgress(c8oResponseListener, Math.max(currentRead[0], length), Math.max(currentRead[0], length), true);
        } catch (Exception e) {
            throw new C8oException("boom", e);
        }
        return VoidResponse.getInstance();
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

        String mapID = db.getName() + ":" + viewName + ":" + mapSource.hashCode();

        String reduceSource = (String) viewProps.get("reduce");
        Reducer reduceBlock = null;
        if (reduceSource != null) {
            reduceBlock = View.getCompiler().compileReduce(reduceSource, language);
            if (reduceBlock == null) {
                // C8oLogger.log("View " +viewName + " has unknown reduce function:" +reduceBlock, Log.ERROR);
                return null;
            }
            mapID += ":" + reduceSource.hashCode();
        }

        View view = db.getView(viewName);
        view.setMapReduce(mapBlock, reduceBlock, mapID);
        String collation = (String) viewProps.get("collation");
        if ("raw".equals(collation)) {
            view.setCollation(View.TDViewCollation.TDViewCollationRaw);
        }
        return view;
    }

    @SuppressWarnings("unchecked")
    private View checkAndCreateJavaScriptView(Database database, String ddocName, String viewName) {
        String tdViewName = ddocName + "/" + viewName;
        View view = database.getExistingView(tdViewName);

        //RevisionInternal rev = database.getDocument(String.format("_design/%s", ddocName), null, true);
        RevisionInternal rev = database.getDocument("_design/" + ddocName, null, true);

        if (rev == null) {
            return null;
        }

        String revID = rev.getRevID();

        if (view != null) {
            String mapVersion = view.getMapVersion();
            if (mapVersion == null || !revID.equals(viewDDocRev.get(mapVersion))) {
                view = null;
            }
        }

        if (view == null || view.getMap() == null) {
            // No TouchDB view is defined, or it hasn't had a map block assigned
            // Searches in the design document if there is a CouchDB view definition we can compile
            Map<String, Object> views = (Map<String, Object>) rev.getProperties().get("views");
            Map<String, Object> viewProps = (Map<String, Object>) views.get(viewName);
            if (viewProps == null) {
                // C8oLogger.log("view : " + tdViewName + " not found", Log.ERROR);
                return null;
            }
            // If there is a CouchDB view, see if it can be compiled from source:
            view = compileView(database, tdViewName, viewProps);
            if (view != null) {
                viewDDocRev.put(view.getMapVersion(), revID);
            }
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
    private static void addParametersToQuery(Query query, Map<String, Object> parameters) throws C8oException  {
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

    @SuppressWarnings("unchecked")
    static void mergeProperties(Map<String, Object> newProperties, Map<String, Object> oldProperties) {
        // Iterates on old document keys
        Iterator<Map.Entry<String, Object>> oldDocumentIterator = oldProperties.entrySet().iterator();
        while (oldDocumentIterator.hasNext()) {
            // Gets old document key and value
            Map.Entry<String, Object> oldDocumentEntry = oldDocumentIterator.next();
            String oldDocumentKey = oldDocumentEntry.getKey();
            Object oldDocumentValue = oldDocumentEntry.getValue();

            // Checks if the new document contains the same key
            if (newProperties.containsKey(oldDocumentKey)) {
                // Get the new document value
                Object newDocumentValue = newProperties.get(oldDocumentKey);
                if (newDocumentValue instanceof Map<?, ?> && oldDocumentValue instanceof Map<?, ?>) {
                    mergeProperties((Map<String, Object>) newDocumentValue, (Map<String, Object>) oldDocumentValue);
                } else if (newDocumentValue instanceof ArrayList && oldDocumentValue instanceof ArrayList) {
                    mergeArrayProperties((ArrayList<Object>) newDocumentValue, (ArrayList<Object>) oldDocumentValue);
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
                    mergeProperties((Map<String, Object>) newArrayValue, (Map<String, Object>) oldArrayValue);
                } else if (newArrayValue instanceof ArrayList && oldArrayValue instanceof ArrayList) {
                    mergeArrayProperties((ArrayList<Object>) newArrayValue, (ArrayList<Object>) oldArrayValue);
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
     * Used for local cache.
     *
     * @param databaseName
     * @param documentId
     * @return
     * @throws C8oException
     */
    public Document getDocucmentFromDatabase(C8o c8o, String databaseName, String documentId) throws C8oException {
        C8oFullSyncDatabase c8oFullSyncDatabase;
        try {
            c8oFullSyncDatabase = this.getOrCreateFullSyncDatabase(databaseName);
        } catch (C8oException e) {
            throw new C8oException(C8oExceptionMessage.fullSyncGetOrCreateDatabase(databaseName), e);
        }
        return c8oFullSyncDatabase.getDatabase().getDocument(documentId);
    }

    public static void overrideDocument(Document document, Map<String, Object> properties) throws C8oException {
        // Must add the current revision to the properties
        SavedRevision currentRevision = document.getCurrentRevision();
        if (currentRevision != null) {
            properties.put(C8oFullSync.FULL_SYNC__REV, currentRevision.getId());
        }

        try {
            document.putProperties(properties);
        } catch (CouchbaseLiteException e) {
            throw new C8oException(C8oExceptionMessage.fullSyncPutProperties(properties), e);
        }
    }

    C8oLocalCacheResponse getResponseFromLocalCache(String c8oCalRequestIdentifier) throws C8oException, C8oUnavailableLocalCacheException {
        C8oFullSyncDatabase fullSyncDatabase = getOrCreateFullSyncDatabase(C8o.LOCAL_CACHE_DATABASE_NAME);
        Document localCacheDocument = fullSyncDatabase.getDatabase().getExistingDocument(c8oCalRequestIdentifier);

        if (localCacheDocument == null) {
            throw new C8oUnavailableLocalCacheException(C8oExceptionMessage.localCacheDocumentJustCreated());
        }

        /*
        C8oEnum.LocalCachePolicy localCachePolicy = C8oEnum.LocalCachePolicy.getLocalCachePolicy(localCachePolicyString);
        if (localCachePolicy == null) {
            throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidLocalCachePolicy(localCachePolicyString));
        }
        if (!localCachePolicy.isAvailable(C8o.this.context)) {
            throw new C8oUnavailableLocalCacheException(C8oExceptionMessage.localCachePolicyIsDisable());
        }
        */

        Object response = localCacheDocument.getProperty(C8o.LOCAL_CACHE_DOCUMENT_KEY_RESPONSE);
        Object responseType = localCacheDocument.getProperty(C8o.LOCAL_CACHE_DOCUMENT_KEY_RESPONSE_TYPE);
        Object expirationDate = localCacheDocument.getProperty(C8o.LOCAL_CACHE_DOCUMENT_KEY_EXPIRATION_DATE);

        String responseString;
        String responseTypeString;
        long expirationDateLong = -1;

        // Checks if fields containing response informations are valid
        if (response != null && response instanceof String) {
            responseString = (String) response;
        } else {
            throw new C8oException(C8oExceptionMessage.invalidLocalCacheResponseInformation());
        }
        if (responseType != null && responseType instanceof String) {
            responseTypeString = (String) responseType;
        } else {
            throw new C8oException(C8oExceptionMessage.invalidLocalCacheResponseInformation());
        }
        if (expirationDate != null) {
            if (expirationDate instanceof Long) {
                expirationDateLong = (Long) expirationDate;
                long currentTime = System.currentTimeMillis();
                if (expirationDateLong < currentTime) {
                    throw new C8oUnavailableLocalCacheException(C8oExceptionMessage.timeToLiveExpired());
                }
            } else {
                throw new C8oException(C8oExceptionMessage.invalidLocalCacheResponseInformation());
            }
        }
        /*
        if (responseTypeString.equals(C8o.RESPONSE_TYPE_JSON)) {
            return C8oTranslator.stringToJSON(responseString);
        } else if (responseTypeString.equals(C8o.RESPONSE_TYPE_XML)) {
            return C8oTranslator.stringToXML(responseString, C8o.this.documentBuilder);
        } else {
            throw new C8oException(C8oExceptionMessage.invalidLocalCacheResponseInformation());
        }*/

        return new C8oLocalCacheResponse(responseString, responseTypeString, expirationDateLong);
    }

    void saveResponseToLocalCache(String c8oCalRequestIdentifier, C8oLocalCacheResponse localCacheResponse) throws C8oException {
        C8oFullSyncDatabase fullSyncDatabase = getOrCreateFullSyncDatabase(C8o.LOCAL_CACHE_DATABASE_NAME);
        Document localCacheDocument =  fullSyncDatabase.getDatabase().getDocument(c8oCalRequestIdentifier);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(C8o.LOCAL_CACHE_DOCUMENT_KEY_RESPONSE, localCacheResponse.getResponse());
        properties.put(C8o.LOCAL_CACHE_DOCUMENT_KEY_RESPONSE_TYPE, localCacheResponse.getResponseType());
        if (localCacheResponse.getExpirationDate() > 0) {
            properties.put(C8o.LOCAL_CACHE_DOCUMENT_KEY_EXPIRATION_DATE, localCacheResponse.getExpirationDate());
        }
        SavedRevision currentRevision = localCacheDocument.getCurrentRevision();
        if (currentRevision != null) {
            properties.put(FULL_SYNC__REV, currentRevision.getId());
        }

        try {
            localCacheDocument.putProperties(properties);
        } catch (Exception e) {
            throw new C8oException("TODO", e);
        }
    }

    public void addFullSyncChangeListener(String db, C8oFullSyncChangeListener listener) throws C8oException {
        if (db == null || db.isEmpty()) {
            db = c8o.getDefaultDatabaseName();
        }

        final Set<C8oFullSyncChangeListener>[] listeners = new Set[]{null};

        if (fullSyncChangeListeners.containsKey(db)) {
            listeners[0] = fullSyncChangeListeners.get(db);
        } else {
            listeners[0] = new HashSet<C8oFullSyncChangeListener>();
            fullSyncChangeListeners.put(db, listeners[0]);
            Database.ChangeListener evtHandler = new Database.ChangeListener() {
                @Override
                public void changed (final Database.ChangeEvent e) {
                    c8o.runBG(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject changes = new JSONObject();
                                JSONArray docs = new JSONArray();

                                changes.put("changes", docs);
                                changes.put("isExternal", e.isExternal());

                                for (DocumentChange change: e.getChanges()) {
                                    JSONObject doc = new JSONObject();
                                    doc.put("id", change.getDocumentId());
                                    doc.put("isConflict", change.isConflict());
                                    doc.put("isCurrentRevision", change.isCurrentRevision());
                                    doc.put("winningRevision", change.getWinningRevisionID());
                                    doc.put("revisionId", change.getRevisionId());
                                    if (change.getSource() != null) {
                                        doc.put("sourceUrl", "" + change.getSource());
                                    }
                                    docs.put(doc);
                                }

                                for (C8oFullSyncChangeListener handler :listeners[0])
                                {
                                    handler.onChange(changes);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            };
            getOrCreateFullSyncDatabase(db).getDatabase().addChangeListener(evtHandler);
            cblChangeListeners.put(db, evtHandler);
        }
        listeners[0].add(listener);
    }

    public void removeFullSyncChangeListener(String db, C8oFullSyncChangeListener listener) throws C8oException {
        if (db == null || db.isEmpty()) {
            db = c8o.getDefaultDatabaseName();
        }

        if (fullSyncChangeListeners.containsKey(db)) {
            Set<C8oFullSyncChangeListener> listeners = fullSyncChangeListeners.get(db);
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                getOrCreateFullSyncDatabase(db).getDatabase().removeChangeListener(cblChangeListeners.get(db));
                fullSyncChangeListeners.remove(db);
                cblChangeListeners.remove(db);
            }
        }
    }

    private static boolean isPlainObject(Object object) {
        if (object == null
                || object.getClass().isPrimitive()
                || object instanceof Number
                || object instanceof Boolean
                || object instanceof Character
                || object instanceof String
                || object instanceof StringBuilder
                || object instanceof StringBuffer) {
            return false;
        } else {
            return true;
        }
    }
}
