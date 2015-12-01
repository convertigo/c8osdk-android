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

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.CookieStore;
import org.json.JSONObject;
import org.w3c.dom.Document;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import com.convertigo.clientsdk.C8oEnum.LocalCachePolicy;
import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oExceptionMessage;
import com.convertigo.clientsdk.exception.C8oUnavailableLocalCacheException;
import com.convertigo.clientsdk.listener.C8oExceptionListener;
import com.convertigo.clientsdk.listener.C8oResponseJsonListener;
import com.convertigo.clientsdk.listener.C8oResponseListener;
import com.convertigo.clientsdk.listener.C8oResponseXmlListener;
import com.convertigo.clientsdk.util.C8oUtils;

// TODO :
// Mieux logger par etape
// doc
// logger les c8o call avec les parametres pas chiffré et avec les parametre chiffrés

// To use http request with c8o call add the permission in app file AndroidManifest.xml adding the line '<uses-permission android:name="android.permission.INTERNET"/>'
// To use fullSync request with c8o call add the permission in app file AndroidManifest.xml adding the line '<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>'
// and '<uses-permission android:name="android.permission.READ_PHONE_STATE"/>'
/**
 * Allows to send requests to a Convertigo Server (or Studio), these requests are called c8o calls.<br/>
 * C8o calls are done thanks to a HTTP request.<br/>
 * An instance of C8o is connected to only one Convertigo and can't change it.<br/>
 * To use it, you have to first initialize the C8o instance with the Convertigo endpoint , then use call methods with Convertigo variables as parameter.<br/>
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
	private static final Pattern RE_ENDPOINT = Pattern.compile("^(http(s)?://([^:]+)(:[0-9]+)?/[^/]+)/projects/[^/]+$");
	
	//*** Engine reserved parameters ***//
	
	public static final String ENGINE_PARAMETER_PROJECT = "__project";
	public static final String ENGINE_PARAMETER_SEQUENCE = "__sequence";
	public static final String ENGINE_PARAMETER_CONNECTOR = "__connector";
	public static final String ENGINE_PARAMETER_TRANSACTION = "__transaction";
	public static final String ENGINE_PARAMETER_ENCODED = "__encoded";
	public static final String ENGINE_PARAMETER_LOCAL_CACHE = "__localCache";
	static final String ENGINE_PARAMETER_DEVICE_UUID = "__uuid";
	
	//*** Local cache keys ***//
	
	static final String LOCAL_CACHE_PARAMETER_KEY_ENABLED = "enabled";
	static final String LOCAL_CACHE_PARAMETER_KEY_POLICY = "policy";
	static final String LOCAL_CACHE_PARAMETER_KEY_TTL = "ttl";
	static final String LOCAL_CACHE_DOCUMENT_KEY_RESPONSE = "response";
	static final String LOCAL_CACHE_DOCUMENT_KEY_RESPONSE_TYPE = "responseType";
	static final String LOCAL_CACHE_DOCUMENT_KEY_EXPIRATION_DATE = "expirationDate";
	
	static final String LOCAL_CACHE_DATABASE_NAME = "c8olocalcache";
	
	//*** Response type ***//
	
	public static final String RESPONSE_TYPE_XML = "pxml";
	public static final String RESPONSE_TYPE_JSON = "json";
	
	//*** Network ***//
	
	/**
	 * The Convertigo endpoint, syntax : &lt;protocol&gt;://&lt;host&gt;:&lt;port&gt;/&lt;Convertigo web app path&gt;/projects/&lt;project name&gt; (Example : http://127.0.0.1:18080/convertigo/projects/MyProject)
	 */
	private String endpoint;
    private String endpointConvertigo;
    private boolean endpointIsSecure;
    private String endpointHost;
    private String endpointPort;

    private String deviceUUID;

	/**
	 * Used to run HTTP requests.
	 */
	HttpInterface httpInterface;

    /**
     * Allows to log locally and remotely to the Convertigo server.
     */
    C8oLogger c8oLogger;

    /**
     * Used to run fullSync requests.
     */
    C8oFullSync c8oFullSync;

	/**
	 * Used to build a Document form an InputStream.
	 */
	private DocumentBuilder documentBuilder;
	private Context context;
	private Handler mainLooperHandler;

	/**
	 * Initializes a new instance of the C8o class without specifying a C8oExceptionListener.
	 *
	 * @param context
	 * @param endpoint - The Convertigo endpoint, syntax : &lt;protocol&gt;://&lt;server&gt;:&lt;port&gt;/&lt;Convertigo web app path&gt;/projects/&lt;project name&gt; (Example : http://127.0.0.1:18080/convertigo/projects/MyProject)
	 * @throws C8oException
	 */
	public C8o(Context context, String endpoint) throws C8oException {
		this(context, endpoint, null);
	}
	
	/**
	 * Construct a C8o instance without specifying a C8oExceptionListener.
	 *
	 * @param context
	 * @param endpoint - The Convertigo endpoint, syntax : &lt;protocol&gt;://&lt;server&gt;:&lt;port&gt;/&lt;Convertigo web app path&gt;/projects/&lt;project name&gt; (Example : http://127.0.0.1:18080/convertigo/projects/MyProject)
	 * @param c8oSettings -  Contains optional parameters
	 *
	 * @throws C8oException
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

        deviceUUID = Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

		mainLooperHandler = new Handler(Looper.getMainLooper());

		if (c8oSettings != null) {
			copy(c8oSettings);
		}

        httpInterface = new HttpInterface(this);

		c8oLogger = new C8oLogger(this);
        c8oLogger.setRemoteLogParameters(httpInterface, logRemote, endpointConvertigo, deviceUUID);

		c8oLogger.logMethodCall("C8o", this);

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
                throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentNullParameter("Requestable"));
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
                throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidRequestalbe(requestable));
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
            C8o.handleCallException(c8oExceptionListener, parameters, e);
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
            this.c8oLogger.logMethodCall("call", parameters, c8oResponseListener, c8oExceptionListener);

            // Checks parameters validity
            if (parameters == null) {
                parameters = new HashMap<String, Object>();
            } else {
                // Clones parameters in order to modify them
                parameters = new HashMap<String, Object>(parameters);
            }

            // Creates C8oCallTask (extends android.os.AsyncTask)
            C8oCallTask task = new C8oCallTask(this, parameters, c8oResponseListener, c8oExceptionListener);
            // Performs the task
            task.execute();
        } catch (Exception e) {
            C8o.handleCallException(c8oExceptionListener, parameters, e);
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

    public C8oPromise<JSONObject> callJson(String requestable, Map<String, Object> parameters) {
        final C8oPromise<JSONObject> promise = new C8oPromise<JSONObject>(this);

        call(requestable, parameters, new C8oResponseJsonListener() {
            @Override
            public void onJsonResponse(JSONObject response, Map<String, Object> parameters) {
                promise.onResponse(response, parameters);
            }
        }, new C8oExceptionListener() {
            @Override
            public void onException(Exception exception, Map<String, Object> requestParameters) {
                promise.onFailure(exception, requestParameters);
            }
        });

        return promise;
    }

    public C8oPromise<JSONObject> callJson(String requestable, Object... parameters) {
        return callJson(requestable, toParameters(parameters));
    }

    public C8oPromise<Document> callXml(String requestable, Map<String, Object> parameters) {
        final C8oPromise<Document> promise = new C8oPromise<Document>(this);

        call(requestable, parameters, new C8oResponseXmlListener() {
            @Override
            public void onXmlResponse(Document response, Map<String, Object> requestParameters) {
                promise.onResponse(response, requestParameters);
            }
        }, new C8oExceptionListener() {
            @Override
            public void onException(Exception exception, Map<String, Object> requestParameters) {
                promise.onFailure(exception, requestParameters);
            }
        });

        return promise;
    }

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

    public void log(int logLevel, String message) {
        c8oLogger.log(logLevel, message);
    }

    public void runUI(Runnable code) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            code.run();
        } else {
            mainLooperHandler.post(code);
        }
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

    private static Map<String, Object> toParameters(Object[] parameters) {
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
    static void handleCallException(C8oExceptionListener c8oExceptionListener, Map<String, Object> requestParameters, Exception exception) {
        if (c8oExceptionListener != null) {
            c8oExceptionListener.onException(exception, requestParameters);
        } else {
            exception.printStackTrace();
        }
    }
}
