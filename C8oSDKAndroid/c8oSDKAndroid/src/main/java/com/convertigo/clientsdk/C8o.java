/*
 * Copyright (c) 2001-2014 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL: svn://devus.twinsoft.fr/convertigo/C8oSDK/Android/trunk/C8oSDKAndroid/src/com/convertigo/clientsdk/C8o.java $
 * $Author: julesg $
 * $Revision: 40542 $
 * $Date: 2015-09-28 15:00:02 +0200 (lun., 28 sept. 2015) $*
 *
 */

package com.convertigo.clientsdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.listener.C8oExceptionListener;
import com.convertigo.clientsdk.listener.C8oResponseJsonListener;
import com.convertigo.clientsdk.listener.C8oResponseListener;
import com.convertigo.clientsdk.listener.C8oResponseXmlListener;

import org.apache.http.client.CookieStore;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

// TODO :
// Mieux logger par etape
// doc
// logger les c8o call avec les parametres pas chiffré et avec les parametre chiffrés

// To use http request with c8o call add the permission in app file AndroidManifest.xml adding the line '<uses-permission android:name="android.permission.INTERNET"/>'
// To use fullSync request with c8o call add the permission in app file AndroidManifest.xml adding the line '<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>'
// and '<uses-permission android:name="android.permission.READ_PHONE_STATE"/>'
/**
 * Allows to send requests to a Convertigo Server (or Studio), these requests are called c8o calls.<br/>
 * C8o calls are done thanks to a HTTP request or a CouchbaseLite usage.<br/>
 * An instance of C8o is connected to only one Convertigo and can't change it.<br/>
 * To use it, you have to first initialize the C8o instance with the Convertigo endpoint, then use call methods with Convertigo variables as parameter.
 */
public class C8o extends C8oBase {
	
	// Log :
	// - VERBOSE (v) : methods parameters,
	// - DEBUG (d) : methods calls,
	// - INFO (i) :
	// - WARN (w) :
	// - ERROR (e) :
	
	//*** Regular expression ***//
	
	/**
	 * The regex used to handle the c8o requestable syntax ("&lt;project&gt;.&lt;sequence&gt;" or "&lt;project&gt;.&lt;connector&gt;.&lt;transaction&gt;")
	 */
	private static final Pattern RE_REQUESTABLE = Pattern.compile("^([^.]*)\\.(?:([^.]+)|(?:([^.]+)\\.([^.]+)))$");
	/**
	 * The regex used to get the part of the endpoint before '/projects/...'
	 */
	private static final Pattern RE_ENDPOINT = Pattern.compile("^(http(s)?://([^:]+)(:[0-9]+)?/?.*?)/projects/([^/]+)$");
	
	//*** Engine reserved parameters ***//
	
	static final String ENGINE_PARAMETER_PROJECT = "__project";
	static final String ENGINE_PARAMETER_SEQUENCE = "__sequence";
	static final String ENGINE_PARAMETER_CONNECTOR = "__connector";
	static final String ENGINE_PARAMETER_TRANSACTION = "__transaction";
	static final String ENGINE_PARAMETER_ENCODED = "__encoded";
    static final String ENGINE_PARAMETER_DEVICE_UUID = "__uuid";
    static final String ENGINE_PARAMETER_PROGRESS = "__progress";
    static final String ENGINE_PARAMETER_FROM_LIVE = "__fromLive";

    //*** FULLSYNC parameters ***//

    /**
     * Constant to use as a parameter for a Call of "fs://.post" and must be followed by a FS_POLICY_* constant.
     * <pre>{@code
     * c8o.callJson("fs://.post",
     *   C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
     *   "docid", myid,
     *   "mykey", myvalue
     * ).sync();
     * }</pre>
     */
    static final public String FS_POLICY = "_use_policy";
    /**
     * Use it with "fs://.post" and C8o.FS_POLICY.<br/>
     * This is the default post policy that don't alter the document before the CouchbaseLite's insertion.
     */
    static final public String FS_POLICY_NONE = "none";
    /**
     * Use it with "fs://.post" and C8o.FS_POLICY.<br/>
     * This post policy remove the "_id" and "_rev" of the document before the CouchbaseLite's insertion.
     */
    static final public String FS_POLICY_CREATE = "create";
    /**
     * Use it with "fs://.post" and C8o.FS_POLICY.<br/>
     * This post policy inserts the document in CouchbaseLite even if a document with the same "_id" already exists.
     */
    static final public String FS_POLICY_OVERRIDE = "override";
    /**
     * Use it with "fs://.post" and C8o.FS_POLICY.<br/>
     * This post policy merge the document with an existing document with the same "_id" before the CouchbaseLite's insertion.
     */
    static final public String FS_POLICY_MERGE = "merge";
    /**
     * Use it with "fs://.post". Default value is ".".<br/>
     * This key allow to override the sub key separator in case of document depth modification.
     */
    static final public String FS_SUBKEY_SEPARATOR = "_use_subkey_separator";
    /**
     * Use it with "c8oSettings.setFullSyncStorageEngine" to choose the SQL fullsync storage engine.
     */
    static final public String FS_STORAGE_SQL = "SQL";
    /**
     * Use it with "c8oSettings.setFullSyncStorageEngine" to choose the FORESTDB fullsync storage engine.
     */
    static final public String FS_STORAGE_FORESTDB = "FORESTDB";
    /**
     * Use it with "fs://" request as parameter to enable the live request feature.<br/>
     * Must be followed by a string parameter, the 'liveid' that can be use to cancel the live
     * request using c8o.cancelLive(liveid) method.<br/>
     * A live request automatically recall the then or thenUI handler when the database changed.
     */
    static final public String FS_LIVE = "__live";
	
	//*** Local cache keys ***//
	static final String LOCAL_CACHE_DOCUMENT_KEY_RESPONSE = "response";
	static final String LOCAL_CACHE_DOCUMENT_KEY_RESPONSE_TYPE = "responseType";
	static final String LOCAL_CACHE_DOCUMENT_KEY_EXPIRATION_DATE = "expirationDate";
	
	static final String LOCAL_CACHE_DATABASE_NAME = "c8olocalcache";
	
	//*** Response type ***//
	static final String RESPONSE_TYPE_XML = "pxml";
	static final String RESPONSE_TYPE_JSON = "json";

    /**
     * Returns the current version of the SDK as "x.y.z".
     *
     * @return Current version of the SDK as "x.y.z".
     */
    public static String getSdkVersion() {
        return "2.1.0";
    }

    static final Executor executor;

    static {
        executor = new ThreadPoolExecutor(1, 100, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    //*** Network ***//
	
	/**
	 * The Convertigo endpoint, syntax : &lt;protocol&gt;://&lt;host&gt;:&lt;port&gt;/&lt;Convertigo web app path&gt;/projects/&lt;project name&gt; (Example : http://127.0.0.1:18080/convertigo/projects/MyProject)
	 */
	private String endpoint;
    private String endpointConvertigo;
    private boolean endpointIsSecure;
    private String endpointHost;
    private String endpointPort;
    private String endpointProject;

    private String deviceUUID;

	/**
	 * Used to run HTTP requests.
	 */
	HttpInterface httpInterface;

    /**
     * Allows to log locally and remotely to the Convertigo server.
     */
    public final C8oLogger log;

    /**
     * Used to run fullSync requests.
     */
    C8oFullSync c8oFullSync;

    Map<String, C8oCallTask> lives = new HashMap<String, C8oCallTask>();
    Map<String, String> livesDb = new HashMap<String, String>();

	/**
	 * Used to build a Document form an InputStream.
	 */
	private DocumentBuilder documentBuilder;
	private Context context;
	private Handler mainLooperHandler;
    /**
     * This is the base object representing a Convertigo Server end point. This object should be instanciated
     * when the apps starts and be accessible from any class of the app. Although this is not common , you may have
     * several C8o objects instantiated in your app.
     *
     * @param context The current application Android Context
     * @param endpoint The Convertigo endpoint, syntax : &lt;protocol&gt;://&lt;server&gt;:&lt;port&gt;/&lt;Convertigo web app path&gt;/projects/&lt;project name&gt;<br/>
     *                 Example : http://computerName:18080/convertigo/projects/MyProject
     *
     * @throws C8oException In case of invalid parameter or initialization failure.
     */
	public C8o(Context context, String endpoint) throws C8oException {
		this(context, endpoint, null);
	}
	
	/**
     * This is the base object representing a Convertigo Server end point. This object should be instanciated
     * when the apps starts and be accessible from any class of the app. Although this is not common , you may have
     * several C8o objects instantiated in your app.
	 *
	 * @param context The current application Android Context
	 * @param endpoint The Convertigo endpoint, syntax : &lt;protocol&gt;://&lt;server&gt;:&lt;port&gt;/&lt;Convertigo web app path&gt;/projects/&lt;project name&gt;<br/>
     *                 Example : http://computerName:18080/convertigo/projects/MyProject
	 * @param c8oSettings Initialization options.<br/>
     *                    Example : new C8oSettings().setLogRemote(false).setDefaultDatabaseName("sample")
	 *
	 * @throws C8oException In case of invalid parameter or initialization failure.
	 */
    public C8o(Context context, String endpoint, C8oSettings c8oSettings) throws C8oException {
		// Checks the URL validity
		if (!C8oUtils.isValidUrl(endpoint)) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidURL(endpoint));
		}

		// Checks the endpoint validity
		Matcher matches = RE_ENDPOINT.matcher(endpoint);
		if (!matches.find()) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidEndpoint(endpoint));
		}

		this.context = context;
		this.endpoint = endpoint;

        endpointConvertigo = matches.group(1);
        endpointIsSecure = matches.group(2) != null;
        endpointHost = matches.group(3);
        endpointPort = matches.group(4);
        endpointProject = matches.group(5);

        deviceUUID = Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

		mainLooperHandler = new Handler(Looper.getMainLooper());

		if (c8oSettings != null) {
			copy(c8oSettings);
		}

        httpInterface = new HttpInterface(this);

		log = new C8oLogger(this);

		log.logMethodCall("C8o", this);

		try {
			documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new C8oException(C8oExceptionMessage.initDocumentBuilder(), e);
		}

		try {
			c8oFullSync = new C8oFullSyncCbl();
            c8oFullSync.Init(this, context);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.fullSyncInterfaceInstance(), e);
		}
	}

    /**
     * Makes a c8o call with c8o requestable out of parameters.<br/>
     * To not use a C8oExceptionListener you can set the parameter to null
     *
     * @param requestable - Contains the Convertigo Sequence or Transaction targeted  (Syntax : "<project>.<sequence>" or "<project>.<connector>.<transaction>")
     * @param parameters - Contains c8o variables
     * @param c8oResponseListener - Define the behavior with the c8o call response
     * @param c8oExceptionListener - Define the behavior when there is an exception during execution
     */
    public void call(String requestable, Map<String, Object> parameters, C8oResponseListener c8oResponseListener, C8oExceptionListener c8oExceptionListener) {
        try {
            if (requestable == null) {
                throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentNullParameter("requestable"));
            }

            if (parameters == null) {
                parameters = new HashMap<String, Object>();
            } else {
                // Clone parameters in order to modify them
                parameters = new HashMap<String, Object>(parameters);
            }

            // Use the requestable String to add parameters corresponding to the c8o project, sequence, connector and transaction
            Matcher matches = RE_REQUESTABLE.matcher(requestable);
            if (!matches.find()) {
                // The requestable is not correct so the default transaction of the default connector will be called
                throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidRequestable(requestable));
            }

            // If the project name is specified
            if (!matches.group(1).equals("")) {
                parameters.put(ENGINE_PARAMETER_PROJECT, matches.group(1));
            }
            // If the C8o call use a sequence
            if (matches.group(2) != null) {
                parameters.put(ENGINE_PARAMETER_SEQUENCE, matches.group(2));
            } else {
                parameters.put(ENGINE_PARAMETER_CONNECTOR, matches.group(3));
                parameters.put(ENGINE_PARAMETER_TRANSACTION, matches.group(4));
            }

            call(parameters, c8oResponseListener, c8oExceptionListener);
        } catch (Exception e) {
            handleCallException(c8oExceptionListener, parameters, e);
        }
    }

    public void call(String requestable, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) {
        call(requestable, parameters, c8oResponseListener, null);
    }

    public void call(String requestable, Map<String, Object> parameters) {
        call(requestable, parameters, null, null);
    }

    public void call(String requestable) {
        call(requestable, null, null, null);
    }

    /**
     * Makes a c8o call with c8o requestable in parameters ('__project' and ('__sequence' or ('__connector' and '__transaction'))).<br/>
     * To not use a C8oExceptionListener you can set the parameter to null.
     *
     * @param parameters - Contains c8o variables
     * @param c8oResponseListener - Define the behavior with the c8o call response
     * @param c8oExceptionListener - Define the behavior when there is an exception during execution
     */
    public void call(Map<String, Object> parameters, C8oResponseListener c8oResponseListener, C8oExceptionListener c8oExceptionListener) {
        // IMPORTANT : all c8o calls have to end here !
        try {
            log.logMethodCall("call", parameters, c8oResponseListener, c8oExceptionListener);

            // Checks parameters validity
            if (parameters == null) {
                parameters = new HashMap<String, Object>();
            } else {
                // Clones parameters in order to modify them
                parameters = new HashMap<String, Object>(parameters);
            }

            // Creates C8oCallTask (extends android.os.AsyncTask)
            new C8oCallTask(this, parameters, c8oResponseListener, c8oExceptionListener).execute();
        } catch (Exception e) {
            handleCallException(c8oExceptionListener, parameters, e);
        }
    }

	/**
	 * Makes a c8o call with c8o requestable in parameters ('__project' and ('__sequence' or ('__connector' and '__transaction'))).<br/>
	 * And without specified the C8oExceptionListener, thereby the C8oExceptionListener of the C8o class will be used.<br/>
	 * To not use a C8oExceptionListener you can use the call version with C8oExceptionListener in parameter and set it to null.
	 *
	 * @param parameters - Contains c8o variables
	 * @param c8oResponseListener - Define the behavior with the c8o call response
	 */
	public void call(Map<String, Object> parameters, C8oResponseListener c8oResponseListener) {
		call(parameters, c8oResponseListener, null);
	}

    public void call(Map<String, Object> parameters) {
        call(parameters, null, null);
    }

    public void call() {
        call((Map<String, Object>) null, null, null);
    }

    /**
     * Makes a c8o call with c8o requestable out of parameters, expecting a JSON response through a C8oPromise.<br/>
     * The C8oPromise allow to register response handler with .then and .thenUI,
     * error handler with .fail and failUI,
     * replication handler with .progress
     * and synchronous response with .sync().
     *
     * @param requestable - Contains the Convertigo Sequence or Transaction targeted  (Syntax : "<project>.<sequence>" or "<project>.<connector>.<transaction>")
     * @param parameters - Contains c8o variables as key/value pair in the Map
     * @return A C8oPromise that can deliver the JSON response
     */
    public C8oPromise<JSONObject> callJson(String requestable, Map<String, Object> parameters) {
        final C8oPromise<JSONObject> promise = new C8oPromise<JSONObject>(this);

        call(requestable, parameters, new C8oResponseJsonListener() {
            @Override
            public void onJsonResponse(JSONObject response, Map<String, Object> requestParameters) {
                if (response == null && requestParameters.containsKey(ENGINE_PARAMETER_PROGRESS)) {
                    promise.onProgress((C8oProgress) requestParameters.get(ENGINE_PARAMETER_PROGRESS));
                } else {
                    promise.onResponse(response, requestParameters);
                }
            }
        }, new C8oExceptionListener() {
            @Override
            public void onException(Exception exception, Map<String, Object> data) {
                promise.onFailure(exception, data);
            }
        });

        return promise;
    }

    /**
     * Makes a c8o call with c8o requestable out of parameters, expecting a JSON response through a C8oPromise.<br/>
     * The C8oPromise allow to register response handler with .then and .thenUI,
     * error handler with .fail and failUI,
     * replication handler with .progress
     * and synchronous response with .sync().
     *
     * @param requestable - Contains the Convertigo Sequence or Transaction targeted  (Syntax : "<project>.<sequence>" or "<project>.<connector>.<transaction>")
     * @param parameters - Contains c8o variables as key/value pair of sibling values
     * @return A C8oPromise that can deliver the JSON response
     */
    public C8oPromise<JSONObject> callJson(String requestable, Object... parameters) {
        return callJson(requestable, toParameters(parameters));
    }

    /**
     * Makes a c8o call with c8o requestable out of parameters, expecting a JSON response through a C8oPromise.<br/>
     * The C8oPromise allow to register response handler with .then and .thenUI,
     * error handler with .fail and failUI,
     * replication handler with .progress
     * and synchronous response with .sync().
     *
     * @param requestable - Contains the Convertigo Sequence or Transaction targeted  (Syntax : "<project>.<sequence>" or "<project>.<connector>.<transaction>")
     * @param parameters - Contains c8o variables as key/value pair inside the JSONObject
     * @return A C8oPromise that can deliver the JSON response
     */
    public C8oPromise<JSONObject> callJson(String requestable, JSONObject parameters) {
        Map<String, Object> map = new LinkedHashMap<String, Object>(parameters.length());
        for (Iterator i = parameters.keys(); i.hasNext();) {
            String key = (String) i.next();
            try {
                map.put(key, parameters.get(key));
            } catch (JSONException ex) {
                throw new IllegalArgumentException("Something wrong with this parameter", ex);
            }
        }
        return callJson(requestable, map);
    }

    /**
     * Makes a c8o call with c8o requestable out of parameters, expecting a XML Document response through a C8oPromise.<br/>
     * The C8oPromise allow to register response handler with .then and .thenUI,
     * error handler with .fail and failUI,
     * replication handler with .progress
     * and synchronous response with .sync().
     *
     * @param requestable - Contains the Convertigo Sequence or Transaction targeted  (Syntax : "<project>.<sequence>" or "<project>.<connector>.<transaction>")
     * @param parameters - Contains c8o variables as key/value pair in the Map
     * @return A C8oPromise that can deliver the XML Document response
     */
    public C8oPromise<Document> callXml(String requestable, final Map<String, Object> parameters) {
        final C8oPromise<Document> promise = new C8oPromise<Document>(this);

        call(requestable, parameters, new C8oResponseXmlListener() {
            @Override
            public void onXmlResponse(Document response, Map<String, Object> requestParameters) {
                if (response == null && requestParameters.containsKey(ENGINE_PARAMETER_PROGRESS)) {
                    promise.onProgress((C8oProgress) requestParameters.get(ENGINE_PARAMETER_PROGRESS));
                } else {
                    promise.onResponse(response, requestParameters);
                }
            }
        }, new C8oExceptionListener() {
            @Override
            public void onException(Exception exception, Map<String, Object> requestParameters) {
                promise.onFailure(exception, requestParameters);
            }
        });

        return promise;
    }

    /**
     * Makes a c8o call with c8o requestable out of parameters, expecting a XML Document response through a C8oPromise.<br/>
     * The C8oPromise allow to register response handler with .then and .thenUI,
     * error handler with .fail and failUI,
     * replication handler with .progress
     * and synchronous response with .sync().
     *
     * @param requestable - Contains the Convertigo Sequence or Transaction targeted  (Syntax : "<project>.<sequence>" or "<project>.<connector>.<transaction>")
     * @param parameters - Contains c8o variables as key/value pair of sibling values
     * @return A C8oPromise that can deliver the XML Document response
     */
    public C8oPromise<Document> callXml(String requestable, Object... parameters) {
        return callXml(requestable, toParameters(parameters));
    }

    /**
     * Add a cookie to the cookie store.<br/>
     * Automatically set the domain and secure flag using the c8o endpoint.
     *
     * @param name
     * @param value
     */
    public void addCookie(String name, String value) {
        this.httpInterface.addCookie(name, value);
    }

    /**
     * Enable the internal SDK log.<br/>
     * Add to the application log (generated using the c8o.log object) the logs generated internally
     * by the SDK. Useful for debugging the SDK.
     *
     * @param logC8o set true to enable internal SDK log.
     */
    public void setLogC8o(boolean logC8o) {
        this.logC8o = logC8o;
    }

    /**
     * Sets a value indicating if logs are sent to the Convertigo server.<br/>
     * Default is <b>true</b>.
     *
     * @param logRemote set true to enable remote logs.
     */
    public void setLogRemote(boolean logRemote) {
        this.logRemote = logRemote;
    }

    /**
     * Sets a value indicating the log level you want in the device console
     * 0: ALL, 1: NONE, 2: TRACE, 3: DEBUG, 4: INFO, 5: WARN, 6: ERROR, 7: FATAL
     * or use the android.util.Log constants
     * Default is <b>0</b>.
     */
    public void setLogLevelLocal(int logLevelLocal) {
        this.logLevelLocal = logLevelLocal;
    }

    /**
     * Set the storage engine for local FullSync databases. Use C8o.FS_STORAGE_SQL or C8o.FS_STORAGE_FORESTDB.
     *
     * @param fullSyncStorageEngine
     */
    public void setFullSyncStorageEngine(String fullSyncStorageEngine) {
        if (C8o.FS_STORAGE_SQL.equals(fullSyncStorageEngine) ||
                C8o.FS_STORAGE_FORESTDB.equals(fullSyncStorageEngine)) {
            this.fullSyncStorageEngine = fullSyncStorageEngine;
        }
    }

    /**
     * Set the encryption key for local FullSync databases encryption.
     *
     * @param fullSyncEncryptionKey
     */
    public void setFullSyncEncryptionKey(String fullSyncEncryptionKey) {
        this.fullSyncEncryptionKey = fullSyncEncryptionKey;
    }

    /**
     * Is the current thread is the UI thread.
     *
     * @return true if the current thread is the UI thread
     */
    public boolean isUI() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /**
     * Run a block of code in the UI Thread.<br/>
     * Run the code directly if already in the UI thread.
     *
     * @param code The code to run in the UI Thread.
     */
    public void runUI(Runnable code) {
        if (isUI()) {
            code.run();
        } else {
            mainLooperHandler.post(code);
        }
    }

    /**
     * Run a block of code in a background Thread.
     *
     * @param code The code to run in the background Thread.
     */
    public void runBG(Runnable code) {
        executor.execute(code);
    }

    @Override
    public String toString() {
        return "C8o[" + endpoint + "] " + super.toString();
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getEndpointConvertigo() {
        return endpointConvertigo;
    }

    public boolean getEndpointIsSecure() {
        return endpointIsSecure;
    }

    public String getEndpointHost() {
        return endpointHost;
    }

    public String getEndpointPort() {
        return  endpointPort;
    }

    public String getEndpointProject() {
        return  endpointProject;
    }

    public String getDeviceUUID() {
        return deviceUUID;
    }

    public CookieStore getCookieStore() {
        return this.httpInterface.getCookieStore();
    }

    public Context getContext() {
        return context;
    }

    public DocumentBuilder getDocumentBuilder() {
        return this.documentBuilder;
    }

    /**
     * Add a listener to monitor all changes of the 'db'.
     *
     * @param db the name of the fullsync database to monitor. Use the default database for a blank or a null value.
     * @param listener the listener to trigger on change.
     */
    public void addFullSyncChangeListener(String db, C8oFullSyncChangeListener listener) throws C8oException {
        c8oFullSync.addFullSyncChangeListener(db, listener);
    }

    /**
     * Remove a listener for changes of the 'db'.
     *
     * @param db the name of the fullsync database to monitor. Use the default database for a blank or a null value.
     * @param listener the listener instance to remove.
     */
    public void removeFullSyncChangeListener(String db, C8oFullSyncChangeListener listener) throws C8oException {
        c8oFullSync.removeFullSyncChangeListener(db, listener);
    }

    void addLive(String liveid, String db, C8oCallTask task) throws C8oException {
        cancelLive(liveid);
        synchronized (lives) {
            lives.put(liveid, task);
        }

        synchronized (livesDb) {
            livesDb.put(liveid, db);
        }

        addFullSyncChangeListener(db, handleFullSyncLive);
    }

    /**
     * Cancel a live request previously enabled by the C8o.FS_LIVE parameter.
     *
     * @param liveid The value associated with the C8o.FS_LIVE parameter.
     */
    public void cancelLive(String liveid) throws C8oException {
        if (livesDb.containsKey(liveid)) {
            String db;
            synchronized (livesDb) {
                db = livesDb.remove(liveid);
                if (livesDb.containsValue(db)) {
                    db = null;
                }
            }

            if (db != null) {
                removeFullSyncChangeListener(db, handleFullSyncLive);
            }
        }
        synchronized (lives) {
            lives.remove(liveid);
        }
    }

    /**
     * Transforms siblings values as key/value of a Map.
     *
     * @param parameters pair of values to transform into a Map.
     * @return a Map that contains all parameters
     */
    private static Map<String, Object> toParameters(Object... parameters) {
        if (parameters.length % 2 != 0) {
            throw new InvalidParameterException("TODO");
        }

        Map<String, Object> newParameters = new HashMap<String, Object>(parameters.length / 2);

        for (int i = 0; i < parameters.length; i += 2) {
            newParameters.put("" + parameters[i], parameters[i + 1]);
        }

        return newParameters;
    }

    /**
     * Calls the exception listener callback if it is not null, else prints the exception stack trace.
     *
     * @param c8oExceptionListener
     * @param requestParameters
     * @param exception
     */
    void handleCallException(C8oExceptionListener c8oExceptionListener, Map<String, Object> requestParameters, Exception exception) {
        log._warn("Handle a call exception", exception);

        if (c8oExceptionListener != null) {
            c8oExceptionListener.onException(exception, requestParameters);
        }
    }

    private C8oFullSyncChangeListener handleFullSyncLive = new C8oFullSyncChangeListener() {

        @Override
        public void onChange(JSONObject changes) {
            synchronized (lives) {
                for (C8oCallTask task: lives.values()) {
                    task.executeFromLive();
                }
            }
        }
    };
}
