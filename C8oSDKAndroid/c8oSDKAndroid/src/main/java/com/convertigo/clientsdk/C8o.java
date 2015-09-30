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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import android.content.Context;
import android.os.AsyncTask;
import android.provider.Settings;
import android.webkit.URLUtil;

import com.convertigo.clientsdk.C8oEnum.LocalCachePolicy;
import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oExceptionMessage;
import com.convertigo.clientsdk.exception.C8oHttpRequestException;
import com.convertigo.clientsdk.exception.C8oUnavailableLocalCacheException;
import com.convertigo.clientsdk.fullsync.FullSyncInterface;
import com.convertigo.clientsdk.http.HttpInterface;
import com.convertigo.clientsdk.listener.C8oExceptionListener;
import com.convertigo.clientsdk.listener.C8oFullSyncResponseListener;
import com.convertigo.clientsdk.listener.C8oJSONResponseListener;
import com.convertigo.clientsdk.listener.C8oResponseListener;
import com.convertigo.clientsdk.listener.C8oXMLResponseListener;
import com.convertigo.clientsdk.util.C8oUtils;
import com.couchbase.lite.QueryEnumerator;

// TODO :
// Meiux logger par etape
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
public class C8o {
	
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
	private static final Pattern RE_ENDPOINT = Pattern.compile("^(http(s)?://([^:]+)(?::[0-9]+)?/[^/]+)/projects/[^/]+$");
	
	//*** Engine reserved parameters ***//
	
	public static final String ENGINE_PARAMETER_PROJECT = "__project";
	public static final String ENGINE_PARAMETER_SEQUENCE = "__sequence";
	public static final String ENGINE_PARAMETER_CONNECTOR = "__connector";
	public static final String ENGINE_PARAMETER_TRANSACTION = "__transaction";
	public static final String ENGINE_PARAMETER_ENCODED = "__encoded";
	public static final String ENGINE_PARAMETER_LOCAL_CACHE = "__localCache";
	static final String ENGINE_PARAMETER_DEVICE_UUID = "__uuid";
	
	//*** Local cache keys ***//
	
	public static final String LOCAL_CACHE_PARAMETER_KEY_ENABLED = "enabled";
	public static final String LOCAL_CACHE_PARAMETER_KEY_POLICY = "policy";
	public static final String LOCAL_CACHE_PARAMETER_KEY_TTL = "ttl";
	private static final String LOCAL_CACHE_DOCUMENT_KEY_RESPONSE = "response";
	private static final String LOCAL_CACHE_DOCUMENT_KEY_RESPONSE_TYPE = "responseType";
	private static final String LOCAL_CACHE_DOCUMENT_KEY_EXPIRATION_DATE = "expirationDate";
	
	private static final String LOCAL_CACHE_DATABASE_NAME = "c8olocalcache";
	
	//*** Response type ***//
	
	public static final String RESPONSE_TYPE_XML = "pxml";
	public static final String RESPONSE_TYPE_JSON = "json";
	
	//*** Network ***//
	
	/**
	 * The Convertigo endpoint, syntax : &lt;protocol&gt;://&lt;host&gt;:&lt;port&gt;/&lt;Convertigo web app path&gt;/projects/&lt;project name&gt; (Example : http://127.0.0.1:18080/convertigo/projects/MyProject)
	 */
	private String endpoint;
	/**
	 * Contains groups generated by the endpoint regular expression.
	 */
	private String endpointGroups[];
	/**
	 * Used to run HTTP requests.
	 */
	private HttpInterface httpInterface;
	
	//*** Other ***//
	
	/**
	 * Define the behavior when there is an exception during execution except during c8o calls with a defined C8oExceptionListener.
	 */
	private C8oExceptionListener defaultC8oExceptionListener;
	/**
	 * Used to build a Document form an InputStream.
	 */
	private DocumentBuilder documentBuilder;
	private String deviceUUId;
	private C8oSettings c8oSettings;
	private Context context;
	public C8oLogger c8oLogger;
	
	//*** FullSync ***//
	
	/**
	 * Used to run fullSync requests.
	 */
	private FullSyncInterface fullSyncInterface;
	
	//*** TAG Constructor ***//
	
	/**
	 * Initializes a new instance of the C8o class without specifying a C8oExceptionListener.
	 *
	 * @param context
	 * @param endpoint - The Convertigo endpoint, syntax : &lt;protocol&gt;://&lt;server&gt;:&lt;port&gt;/&lt;Convertigo web app path&gt;/projects/&lt;project name&gt; (Example : http://127.0.0.1:18080/convertigo/projects/MyProject)
	 * @throws C8oException
	 */
	public C8o(Context context, String endpoint) throws C8oException {
		this(context, endpoint, new C8oSettings(), null);
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
		this(context, endpoint, c8oSettings, null);
	}
	
	/**
	 * Construct a C8o instance specifying a C8oExceptionListener.
	 *
	 * @param context
	 * @param endpoint - The Convertigo endpoint, syntax : &lt;protocol&gt;://&lt;server&gt;:&lt;port&gt;/&lt;Convertigo web app path&gt;/projects/&lt;project name&gt; (Example : http://127.0.0.1:18080/convertigo/projects/MyProject)
	 * @param c8oSettings -  Contains optional parameters
	 * @param defaultC8oExceptionListener - Define the behavior when there is an exception during execution except during c8o calls with a defined C8oExceptionListener
	 *
	 * @throws C8oException
	 */
	public C8o(Context context, String endpoint, C8oSettings c8oSettings, C8oExceptionListener defaultC8oExceptionListener) throws C8oException {
		// IMPORTANT : All C8o constructors have to end here

		//*** Checks parameters validity ***//
		
		// Checks the URL validity
		if (!URLUtil.isValidUrl(endpoint)) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidURL(endpoint));
		}
		// Checks the endpoint validity
    	Matcher matches = RE_ENDPOINT.matcher(endpoint);
		if (!matches.find()) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidEndpoint(endpoint));
		}
		
		//*** Initializes attributes ***//

		this.context = context;
		this.endpoint = endpoint;
		this.defaultC8oExceptionListener = defaultC8oExceptionListener;
		this.c8oSettings = c8oSettings;
		this.c8oLogger = new C8oLogger(this.defaultC8oExceptionListener, this.c8oSettings);
		
		this.c8oLogger.logMethodCall("C8o", endpoint, c8oSettings, defaultC8oExceptionListener);
		
		this.endpointGroups = new String[4];
		for (int i = 0; i < this.endpointGroups.length; i++) {
			this.endpointGroups[i] = matches.group(i);
		}
		try {
			this.documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new C8oException(C8oExceptionMessage.initDocumentBuilder(), e);
		}
		this.deviceUUId = Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
		
		try {
			this.httpInterface = new HttpInterface(this, endpoint, c8oSettings);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.httpInterfaceInstance(), e);
		}
		try {
			this.fullSyncInterface = new FullSyncInterface(context, this, this.endpointGroups[1], c8oSettings);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.fullSyncInterfaceInstance(), e);
		}
		
		this.c8oLogger.setRemoteLogParameters(httpInterface, c8oSettings.isLogRemote, this.endpointGroups[1], this.deviceUUId);
	}
	
	//*** toString ***//
	
	@Override
	public String toString() {
		String string = this.getClass().getName() + "[";
		
		string += "endpoint=" + this.endpoint + "]";
		
		return string;
	}
	
	//*** TAG Utilities ***//
	
	/**
	 * Calls the exception listener callback if it is not null, else prints the exception stack trace.
	 * 
	 * @param c8oExceptionListener
	 * @param requestParameters
	 * @param exception
	 */
	public static void handleCallException(C8oExceptionListener c8oExceptionListener, List<NameValuePair> requestParameters, Exception exception) {
		if (c8oExceptionListener != null) {
			c8oExceptionListener.onException(requestParameters, exception);
		} else {
			exception.printStackTrace();
		}
	}
	
	public void log(int logLevel, String message) {
		this.c8oLogger.log(logLevel, message);
	}
	
	//*** TAG Convertigo call ***//
	
	/**
	 * Makes a c8o call with c8o requestable in parameters ('__project' and ('__sequence' or ('__connector' and '__transaction'))).<br/>
	 * And without specified the C8oExceptionListener, thereby the C8oExceptionListener of the C8o class will be used.<br/>
	 * To not use a C8oExceptionListener you can use the call version with C8oExceptionListener in parameter and set it to null.
	 *
	 * @param parameters - Contains c8o variables
	 * @param c8oResponseListener - Define the behavior with the c8o call response
	 */
	public void call(List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) {
		this.call(parameters, c8oResponseListener, this.defaultC8oExceptionListener);
	}
	
	/**
	 * Makes a c8o call with c8o requestable in parameters ('__project' and ('__sequence' or ('__connector' and '__transaction'))).<br/>
	 * To not use a C8oExceptionListener you can set the parameter to null.
	 *
	 * @param parameters - Contains c8o variables
	 * @param c8oResponseListener - Define the behavior with the c8o call response
	 * @param c8oExceptionListener - Define the behavior when there is an exception during execution
	 */
	public void call(List<NameValuePair> parameters, C8oResponseListener c8oResponseListener, C8oExceptionListener c8oExceptionListener) {
		// IMPORTANT : all c8o calls have to end here !		
		try {			
			// Log the method call
			this.c8oLogger.logMethodCall("call", parameters, c8oResponseListener, c8oExceptionListener);
			
			// Checks parameters validity
			if (parameters == null) {
				throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentNullParameter("Call parameters"));
			}
//			if (c8oResponseListener == null) {
//				throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentNullParameter("Call response listener"));
//			}
			
			// Clones parameters in order to modify them 
			parameters = C8oUtils.cloneList(parameters);
			
			// Creates C8oCallTask (extends android.os.AsyncTask)
			C8oCallTask task = new C8oCallTask(parameters, c8oResponseListener, c8oExceptionListener);
			// Performs the task
			task.execute();
		} catch (Exception e) {
			C8o.handleCallException(c8oExceptionListener, parameters, e);
		}
	}
	
	/**
	 * Makes a c8o call with c8o requestable out of parameters.<br/>
	 * And without specified the C8oExceptionListener, thereby the C8oExceptionListener of the C8o class will be used.<br/>
	 * To not use a C8oExceptionListener you can use the call version with C8oExceptionListener in parameter and set it to null.
	 *
	 * @param requestable - Contains the Convertigo Sequence or Transaction targeted  (Syntax : "<project>.<sequence>" or "<project>.<connector>.<transaction>")
	 * @param parameters - Contains c8o variables
	 * @param c8oResponseListener - Define the behavior with the c8o call response
	 */
	public void call(String requestable, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) {
		this.call(requestable, parameters, c8oResponseListener, this.defaultC8oExceptionListener);
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
	public void call(String requestable, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener, C8oExceptionListener c8oExceptionListener) {
		try {
			// Checks parameters validity
			if (requestable == null) {
				throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentNullParameter("Requestable"));
			}
			if (parameters == null) {
				throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentNullParameter("Parameters"));
			}
			
			// Clone parameters in order to modify them 
			parameters = C8oUtils.cloneList(parameters);
			
			// Use the requestable String to add parameters corresponding to the c8o project, sequence, connector and transaction
			Matcher matches = RE_REQUESTABLE.matcher(requestable);
			if (matches.find()) {
				// If the project name is specified
				if (!matches.group(1).equals("")) {
					parameters.add(new BasicNameValuePair(C8o.ENGINE_PARAMETER_PROJECT, matches.group(1)));
				}
				// If the C8o call use a sequence
				if (matches.group(2) != null) {
					parameters.add(new BasicNameValuePair(C8o.ENGINE_PARAMETER_SEQUENCE, matches.group(2)));
				} else {
					parameters.add(new BasicNameValuePair(C8o.ENGINE_PARAMETER_CONNECTOR, matches.group(3)));
					parameters.add(new BasicNameValuePair(C8o.ENGINE_PARAMETER_TRANSACTION, matches.group(4)));
				}
			} else {
				// The requestable is not correct so the default transaction of the default connector will be called
				throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidRequestalbe(requestable));
			}
			
			this.call(parameters, c8oResponseListener, c8oExceptionListener);
		} catch (Exception e) {
			C8o.handleCallException(c8oExceptionListener, parameters, e);
		}
	}
 	
	
	//*** TAG Local cache ***//
	
	/**
	 * Gets the specified document from the local cache database.
	 * 
	 * @param documentId
	 * @return
	 * @throws C8oException
	 */
	private com.couchbase.lite.Document getDocumentFromLocalCache(String documentId) throws C8oException {
		try {
			return this.fullSyncInterface.getDocucmentFromDatabase(this, C8o.LOCAL_CACHE_DATABASE_NAME, documentId);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.getDocumentFromDatabase(documentId), e);
		}
	}
	
	/**
	 * Deserializes and returns the local cache response stored into a fullSync document.
	 * 
	 * @param localCacheDocument
	 * @param localCachePolicyString
	 * @return
	 * @throws C8oException
	 * @throws C8oUnavailableLocalCacheException
	 */
	private Object getResponseFromLocalCacheDocument(com.couchbase.lite.Document localCacheDocument, String localCachePolicyString) throws C8oException, C8oUnavailableLocalCacheException {
		
		//*** Checks parameters validity ***//
		
		if (localCacheDocument.getCurrentRevision() == null) {
			throw new C8oUnavailableLocalCacheException(C8oExceptionMessage.localCacheDocumentJustCreated());
		}
		LocalCachePolicy localCachePolicy = LocalCachePolicy.getLocalCachePolicy(localCachePolicyString);
		if (localCachePolicy == null) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidLocalCachePolicy(localCachePolicyString));
		}
		if (!localCachePolicy.isAvailable(C8o.this.context)) {
			throw new C8oUnavailableLocalCacheException(C8oExceptionMessage.localCachePolicyIsDisable());
		}
		
		Object response = localCacheDocument.getProperty(C8o.LOCAL_CACHE_DOCUMENT_KEY_RESPONSE);
		Object responseType = localCacheDocument.getProperty(C8o.LOCAL_CACHE_DOCUMENT_KEY_RESPONSE_TYPE);
		Object expirationDate = localCacheDocument.getProperty(C8o.LOCAL_CACHE_DOCUMENT_KEY_EXPIRATION_DATE);
		
		String responseString;
		String responseTypeString;
		Long expirationDateLong;
		
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
		
		if (responseTypeString.equals(C8o.RESPONSE_TYPE_JSON)) {
			return C8oTranslator.stringToJSON(responseString);
		} else if (responseTypeString.equals(C8o.RESPONSE_TYPE_XML)) {
			return C8oTranslator.stringToXML(responseString, C8o.this.documentBuilder);
		} else {
			throw new C8oException(C8oExceptionMessage.invalidLocalCacheResponseInformation());
		}
	}
	
	private void saveResponseToLocalCache(com.couchbase.lite.Document localCacheDocument, String documentId, String responseString, String responseType, Integer timeToLive) throws C8oException {
		LinkedHashMap<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put(LOCAL_CACHE_DOCUMENT_KEY_RESPONSE, responseString);
		properties.put(LOCAL_CACHE_DOCUMENT_KEY_RESPONSE_TYPE, responseType);
		if (timeToLive != null) {
			long expirationDate = System.currentTimeMillis() + timeToLive;
			properties.put(LOCAL_CACHE_DOCUMENT_KEY_EXPIRATION_DATE, expirationDate);
		}
		try {
			FullSyncInterface.overrideDocument(localCacheDocument, properties);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.overrideDocument(), e);
		}
	}
	
	//*** TAG Getter / Setter ***//
	
	/**
	 * Get the DocumentBuilder.
	 *
	 * @return
	 */
	public DocumentBuilder getDocumentBuilder() {
		return this.documentBuilder;
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
	
	public CookieStore getCookieStore() {
		return this.httpInterface.getCookieStore();
	}
	
	public C8oExceptionListener getDefaultC8oExceptionListener() {
		return this.defaultC8oExceptionListener;
	}
	
	public String getEndpointGroup(int index) {
		return endpointGroups[index];
	}
	
	public String getDeviceId() {
		return this.deviceUUId;
	}

	public C8oSettings getC8oSettings() {
		return this.c8oSettings;
	}
	
	//*** TAG Private Classes ***//
	
	// Informations on AsyncTask :
	// AsyncTasks allows to perform background operations and publish results
	// It should ideally be used for short operations (a few seconds at the most)
	// An asynchronous task is defined by 3 generic types, called Params, Progress and Result (AsyncTask<Params, Progress, Result>),
	// and 4 steps, called onPreExecute, doInBackground, onProgressUpdate and onPostExecute.
	// The three types used by an asynchronous task are the following:
	// 1. Params, the type of the parameters sent to the task upon execution.
	// 2. Progress, the type of the progress units published during the background computation.
	// 3. Result, the type of the result of the background computation.
	// 		The result is the return of the method doInBackground(), it is set as parameter called 'result' of the method onPostExecute()
	
	// Threading rules that must be followed for this class to work properly:
	// - The AsyncTask class must be loaded on the UI thread. This is done automatically as of JELLY_BEAN.
	// - The task instance must be created on the UI thread.
	// - execute(Params...) must be invoked on the UI thread.
	// - Do not call onPreExecute(), onPostExecute(Result), doInBackground(Params...), onProgressUpdate(Progress...) manually.
	// - The task can be executed only once (an exception will be thrown if a second execution is attempted.)
	
	//*** TAG C8oCallTask ***//
	
	/**
	 * Allows to perform a background c8o call and publish results
	 * C8oCallTask extends android.os.AsyncTask<C8oCallTaskParameter, Void, Object> defined by three types : Params, Progress, Result
	 *  - Params = C8oCallTaskParameter is the type of the parameter sent to the task upon execution
	 *  - Progress = Void is the type of the progress units published during the background computation (not used here !)
	 *  - Result = Object is the type of the result of the background computation, the result depends of the used C8oListener type
	 *    the result is returned by the method doInBackground(), and it is set as parameter called 'result' of the method onPostExecute()
	 *    it can be Document (for XML response) or JSONObject (for JSON response) or Exception
	 */
	private class C8oCallTask extends AsyncTask<Void, Void, Object> {
		/**
		 * The c8o call URL parameters.
		 */
		private List<NameValuePair> parameters;
		/**
		 * Define the behavior with the response of the c8o call
		 */
		private C8oResponseListener c8oResponseListener;
		/**
		 * Define the behavior in case of an exception during the c8o call
		 */
		private C8oExceptionListener c8oExceptionListener;
		/**
		 * The URL of the c8o call
		 */
		private String c8oCallUrl;
		
		//*** Constructor ***//
		
		/**
		 * Constructs a C8oCallTask instance.
		 *
		 * @param c8oResponseListener - Define the behavior with the response of the c8o call
		 * @param c8oExceptionListener - Define the behavior in case of an exception during the c8o call, it can be set to null
		 * @throws JSONException 
		 * @throws InterruptedException 
		 * @throws UnsupportedEncodingException 
		 */
		public C8oCallTask(List<NameValuePair> parameters, C8oResponseListener c8oResponseListener, C8oExceptionListener c8oExceptionListener) {
			// Log the method call
			C8o.this.c8oLogger.logMethodCall("C8oCallTask", parameters, c8oResponseListener, c8oExceptionListener);
			
			this.parameters = parameters;
			this.c8oResponseListener = c8oResponseListener;
			this.c8oExceptionListener = c8oExceptionListener;
		}
		
		// Perform background operations
		@Override
		protected Object doInBackground(Void... params) {
			try {
				Object response = this.handleRequest();
				this.handleResponse(response);
			} catch (C8oException e) {
				e.printStackTrace();
				this.c8oExceptionListener.onException(this.parameters, e);
			}
			return null;
		}
		
		// Publish results after doInBackground()
		@Override
		protected void onPostExecute(Object result) {
			
		}

		@SuppressWarnings("unchecked")
		private Object handleRequest() throws C8oException {
			// Get parameters
//			List<NameValuePair> parameters = this.c8oCallTaskParameter.getParameters();
			
			boolean isFullSyncRequest = FullSyncInterface.isFullSyncRequest(parameters);
			
			if (isFullSyncRequest) {
				Object fullSyncResult;
				try {
					fullSyncResult = fullSyncInterface.handleFullSyncRequest(parameters, c8oResponseListener);
				} catch (C8oException e) {
					throw new C8oException(C8oExceptionMessage.handleFullSyncRequest(), e);
				}
				return fullSyncResult;
			} else {
				// Get the response type of the future c8o call
				String responseType = "";
				if (c8oResponseListener == null) {
					responseType = RESPONSE_TYPE_JSON;
				} else {
					if (this.c8oResponseListener instanceof C8oXMLResponseListener) {
						responseType = RESPONSE_TYPE_XML;
					} else if (this.c8oResponseListener instanceof C8oJSONResponseListener) {
						responseType = RESPONSE_TYPE_JSON;
					} else {
						// Return an Exception because the C8oListener used is unknown
						return new C8oException(C8oExceptionMessage.wrongListener(this.c8oResponseListener));
					}
				}
				
				//*** Local cache ***//

				String c8oCallRequestIdentifier = null;
				// Allows to enable or disable the local cache on a Convertigo requestable, default value is true
				Boolean localCacheEnabledParameterValue = Boolean.FALSE;
				// Defines the time to live of the cached response, in milliseconds
				Integer localCacheTimeToLiveParameterValue = null;
				// Checks if the local cache must be used
				Object localCacheParameterValue;
				com.couchbase.lite.Document localCacheDocument = null;
				try {
					localCacheParameterValue = C8oUtils.getParameterObjectValue(parameters, C8o.ENGINE_PARAMETER_LOCAL_CACHE, false);
				} catch (C8oException e) {
					return new C8oException(C8oExceptionMessage.getNameValuePairObjectValue(C8o.ENGINE_PARAMETER_LOCAL_CACHE), e);
				}
				// If the engine parameter for local cache is specified
				if (localCacheParameterValue != null) {
					// Checks if this is a JSON object (represented by a LinkedHashMap once translated) 
					if (localCacheParameterValue instanceof LinkedHashMap<?, ?>) {
						LinkedHashMap<String, ?> localCacheParameterValueJson = (LinkedHashMap<String, ?>) localCacheParameterValue;
						// The local cache policy
						String localCachePolicyParameterValue;
						try {
							// Gets local cache properties
							localCacheEnabledParameterValue = C8oUtils.getParameterAndCheckType(localCacheParameterValueJson, C8o.LOCAL_CACHE_PARAMETER_KEY_ENABLED, Boolean.class, false, Boolean.TRUE);
							localCachePolicyParameterValue = C8oUtils.getParameterAndCheckType(localCacheParameterValueJson, C8o.LOCAL_CACHE_PARAMETER_KEY_POLICY, String.class, true, null);
							localCacheTimeToLiveParameterValue = C8oUtils.getParameterAndCheckType(localCacheParameterValueJson, C8o.LOCAL_CACHE_PARAMETER_KEY_TTL, Integer.class, false, localCacheTimeToLiveParameterValue);
						} catch (C8oException e) {
							return new C8oException(C8oExceptionMessage.getLocalCacheParameters(), e);
						}
						if (localCacheEnabledParameterValue) {
							// Checks if the same request is stored in the local cache
							C8oUtils.removeParameter(parameters, C8o.ENGINE_PARAMETER_LOCAL_CACHE);
							try {
								c8oCallRequestIdentifier = C8oUtils.identifyC8oCallRequest(parameters, responseType);
							} catch (C8oException e) {
								return new C8oException(C8oExceptionMessage.serializeC8oCallRequest(), e);
							}
							try {
								localCacheDocument = C8o.this.getDocumentFromLocalCache(c8oCallRequestIdentifier);
							} catch (C8oException e) {
								return new C8oException(C8oExceptionMessage.getResponseFromLocalCache(), e);
							}
							try {
								return C8o.this.getResponseFromLocalCacheDocument(localCacheDocument, localCachePolicyParameterValue);
							} catch (C8oException e) {
								return new C8oException(C8oExceptionMessage.getResponseFromLocalCacheDocument(), e);
							} catch (C8oUnavailableLocalCacheException e) {
								// Does nothing because in this case it means that the local cache is unavailable for this request
							}
						}
					} else {
						return new IllegalArgumentException(C8oExceptionMessage.toDo());
					}
				}
				
				//*** Get response ***//
				
				parameters.add(new BasicNameValuePair(ENGINE_PARAMETER_DEVICE_UUID, C8o.this.deviceUUId));
				
				// Build the c8o call URL
				this.c8oCallUrl = C8o.this.endpoint + "/." + responseType;

				HttpResponse httpResponse = null;
				InputStream inputStream = null;
				
				// Do the c8o call
				try {
					httpResponse = httpInterface.handleC8oCallRequest(this.c8oCallUrl, parameters);
				} catch (C8oException e) {
					return new C8oException(C8oExceptionMessage.handleC8oCallRequest(), e);
				} catch (C8oHttpRequestException e) {
					if (localCacheEnabledParameterValue) {
						try {
							return C8o.this.getResponseFromLocalCacheDocument(localCacheDocument, LocalCachePolicy.PRIORITY_LOCAL.value);
						} catch (C8oException e1) {
							return new C8oException(C8oExceptionMessage.handleC8oCallRequest(), e);
						} catch (C8oUnavailableLocalCacheException e1) {
							return new C8oException(C8oExceptionMessage.handleC8oCallRequest(), e);
						}
					} else {
						return new C8oException(C8oExceptionMessage.handleC8oCallRequest(), e);
					}
				}
				// Get the c8o call result
				try {
					inputStream = httpResponse.getEntity().getContent();
				} catch (IllegalStateException e) {
					return new C8oException(C8oExceptionMessage.getInputStreamFromHttpResponse(), e);
				} catch (IOException e) {
					return new C8oException(C8oExceptionMessage.getInputStreamFromHttpResponse(), e);
				}
				
				//*** Handle response ***//
				
				// Define the return type
				Object response;
				String responseString = null;
				if (this.c8oResponseListener instanceof C8oXMLResponseListener) {
					try {
						if (localCacheEnabledParameterValue) {
							try {
								responseString = C8oTranslator.inputStreamToString(inputStream);
							} catch (C8oException e) {
								return new C8oException(C8oExceptionMessage.parseInputStreamToString(), e);
							}
						}
						response = C8oTranslator.inputStreamToXMLAndClose(inputStream, C8o.this.documentBuilder);
					} catch (C8oException e) {
						return new C8oException(C8oExceptionMessage.inputStreamToXML(), e);
					}
				} else if (this.c8oResponseListener instanceof C8oJSONResponseListener) {
					try {
						try {
							responseString = C8oTranslator.inputStreamToString(inputStream);
						} catch (C8oException e) {
							throw new C8oException(C8oExceptionMessage.parseInputStreamToString(), e);
						}
						response = C8oTranslator.stringToJSON(responseString);
					} catch (C8oException e) {
						return e;
					}
				} else {
					// Return an Exception because the C8oListener used is unknown
					return new C8oException(C8oExceptionMessage.wrongListener(this.c8oResponseListener));
				}
				
				if (localCacheEnabledParameterValue) {
					try {
						C8o.this.saveResponseToLocalCache(localCacheDocument, c8oCallRequestIdentifier, responseString, responseType, localCacheTimeToLiveParameterValue);
					} catch (C8oException e) {
						return new C8oException(C8oExceptionMessage.saveResponseToLocalCache());
					}
				}
				
				return response;
			}
		}
		
		private void handleResponse(Object result) {
			try {
				if (result instanceof VoidResponse) {
					return;
				}
				if (this.c8oResponseListener == null) {
					return;
				}
				
				// Check the type of the result
				if (result instanceof Document) {
					// Log the XML result
					C8o.this.c8oLogger.logC8oCallXMLResponse((Document) result, this.c8oCallUrl, this.parameters);
					// The result is a XML Document
					((C8oXMLResponseListener) this.c8oResponseListener).onXMLResponse(this.parameters, (Document) result);
				} else if (result instanceof JSONObject) {
					// Log the JSON result
					C8o.this.c8oLogger.logC8oCallJSONResponse((JSONObject) result, this.c8oCallUrl, this.parameters);
					// The result is a JSONObject
					((C8oJSONResponseListener) this.c8oResponseListener).onJSONResponse(this.parameters, (JSONObject) result);
				} else if (result instanceof com.couchbase.lite.Document) {
					// TODO log
					
					// The result is a fillSync query response
					((C8oFullSyncResponseListener) this.c8oResponseListener).onDocumentResponse(this.parameters, (com.couchbase.lite.Document) result); 
				} else if (result instanceof QueryEnumerator) {
					// TODO log
					
					// The result is a fillSync query response
					((C8oFullSyncResponseListener) this.c8oResponseListener).onQueryEnumeratorResponse(this.parameters, (QueryEnumerator) result); 
				} else if (result instanceof Exception){
					// The result is an Exception
					C8o.handleCallException(this.c8oExceptionListener, this.parameters, (Exception) result);
				} else {
					// The result type is unknown
					C8o.handleCallException(this.c8oExceptionListener, this.parameters, new C8oException(C8oExceptionMessage.wrongResult(result)));
				}
			} catch (Exception e) {
				C8o.handleCallException(this.c8oExceptionListener, this.parameters, e);
			}
		}
		
	}
	
}
