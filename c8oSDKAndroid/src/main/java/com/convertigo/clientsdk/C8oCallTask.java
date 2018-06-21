package com.convertigo.clientsdk;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oHttpRequestException;
import com.convertigo.clientsdk.exception.C8oResponseException;
import com.convertigo.clientsdk.exception.C8oUnavailableLocalCacheException;
import com.convertigo.clientsdk.listener.C8oExceptionListener;
import com.convertigo.clientsdk.listener.C8oResponseCblListener;
import com.convertigo.clientsdk.listener.C8oResponseJsonListener;
import com.convertigo.clientsdk.listener.C8oResponseListener;
import com.convertigo.clientsdk.listener.C8oResponseXmlListener;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.util.IOUtils;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
/**
 * Allows to perform a background c8o call and publish results
 * C8oCallTask extends android.os.AsyncTask<C8oCallTaskParameter, Void, Object> defined by three types : Params, Progress, Result
 *  - Params = C8oCallTaskParameter is the type of the parameter sent to the task upon execution
 *  - Progress = Void is the type of the progress units published during the background computation (not used here !)
 *  - Result = Object is the type of the result of the background computation, the result depends of the used C8oListener type
 *    the result is returned by the method doInBackground(), and it is set as parameter called 'result' of the method onPostExecute()
 *    it can be Document (for XML response) or JSONObject (for JSON response) or Exception
 */
class C8oCallTask implements Runnable {
    static private Pattern pCharset = Pattern.compile("(?<=charset=)([^;]*)");

    private C8o c8o;
    private Map<String, Object> parameters;
    private C8oResponseListener c8oResponseListener;
    private C8oExceptionListener c8oExceptionListener;
    private String c8oCallUrl;

    public C8oCallTask(C8o c8o, Map<String, Object> parameters, C8oResponseListener c8oResponseListener, C8oExceptionListener c8oExceptionListener) {
        this.c8o = c8o;
        this.parameters = parameters;
        this.c8oResponseListener = c8oResponseListener;
        this.c8oExceptionListener = c8oExceptionListener;

        c8o.log.logMethodCall("C8oCallTask", parameters, c8oResponseListener, c8oExceptionListener);
    }

    public void execute() {
        c8o.runBG(this);
    }

    public void executeFromLive() {
        //parameters.remove(C8o.FS_LIVE);
        parameters.put(C8o.ENGINE_PARAMETER_FROM_LIVE, true);
        execute();
    }

    // Perform background operations
    @Override
    public void run() {
        try {
            Object response = handleRequest();
            handleResponse(response);
        } catch (C8oException e) {
            e.printStackTrace();
            this.c8oExceptionListener.onException(e, parameters);
        }
    }

    private Object handleRequest() throws C8oException {
        boolean isFullSyncRequest = C8oFullSync.isFullSyncRequest(parameters);

        if (isFullSyncRequest) {
            c8o.log._debug("Is FullSync request");

            String liveid = C8oUtils.getParameterStringValue(parameters, C8o.FS_LIVE, false);
            if (liveid != null) {
                String dbName = C8oUtils.getParameterStringValue(parameters, C8o.ENGINE_PARAMETER_PROJECT, true).substring(C8oFullSync.FULL_SYNC_PROJECT.length());
                c8o.addLive(liveid, dbName, this);
            }
            try {
                Object fullSyncResult = c8o.c8oFullSync.handleFullSyncRequest(parameters, c8oResponseListener);
                return fullSyncResult;
            } catch (C8oException e) {
                throw e;
            } catch (Throwable e) {
                throw new C8oException(C8oExceptionMessage.handleFullSyncRequest(), e);
            }
        } else {
            // Get the response type of the future c8o call
            String responseType = "";
            if (c8oResponseListener == null || c8oResponseListener instanceof C8oResponseXmlListener) {
                responseType = C8o.RESPONSE_TYPE_XML;
            } else if (this.c8oResponseListener instanceof C8oResponseJsonListener) {
                responseType = C8o.RESPONSE_TYPE_JSON;
            } else {
                // Return an Exception because the C8oListener used is unknown
                return new C8oException(C8oExceptionMessage.wrongListener(this.c8oResponseListener));
            }

            //*** Local cache ***//

            String c8oCallRequestIdentifier = null;

            // Checks if the local cache must be used
            C8oLocalCache localCache = (C8oLocalCache) C8oUtils.getParameterObjectValue(parameters, C8oLocalCache.PARAM, false);
            boolean localCacheEnabled = false;

            // If the engine parameter for local cache is specified
            if (localCache != null) {
                // Checks if the same request is stored in the local cache
                parameters.remove(C8oLocalCache.PARAM);

                // The local cache policy
                if (localCacheEnabled = localCache.enabled) {
                    try {
                        c8oCallRequestIdentifier = C8oUtils.identifyC8oCallRequest(parameters, responseType);
                    } catch (C8oException e) {
                        return new C8oException(C8oExceptionMessage.serializeC8oCallRequest(), e);
                    }

                    if (localCache.priority.isAvailable(c8o)) {
                        try {
                            C8oLocalCacheResponse localCacheResponse = c8o.c8oFullSync.getResponseFromLocalCache(c8oCallRequestIdentifier);
                            if (!localCacheResponse.isExpired()) {
                                if (responseType == C8o.RESPONSE_TYPE_XML) {
                                    return C8oTranslator.stringToXML(localCacheResponse.getResponse(), c8o.getDocumentBuilder());
                                } else if (responseType == C8o.RESPONSE_TYPE_JSON) {
                                    return C8oTranslator.stringToJSON(localCacheResponse.getResponse());
                                }
                            }
                        } catch (C8oUnavailableLocalCacheException e) {
                            // no entry
                        }
                    }
                }
            }

            //*** Get response ***//

            parameters.put(C8o.ENGINE_PARAMETER_DEVICE_UUID, c8o.getDeviceUUID());

            // Build the c8o call URL
            c8oCallUrl = c8o.getEndpoint() + "/." + responseType;

            HttpResponse httpResponse = null;
            InputStream responseStream = null;

            // Do the c8o call
            try {
                httpResponse = c8o.httpInterface.handleC8oCallRequest(c8oCallUrl, parameters);
            } catch (C8oHttpRequestException e) {
                if (localCacheEnabled) {
                    try {
                        C8oLocalCacheResponse localCacheResponse = c8o.c8oFullSync.getResponseFromLocalCache(c8oCallRequestIdentifier);
                        if (!localCacheResponse.isExpired()) {
                            if (responseType == C8o.RESPONSE_TYPE_XML) {
                                return C8oTranslator.stringToXML(localCacheResponse.getResponse(), c8o.getDocumentBuilder());
                            } else if (responseType == C8o.RESPONSE_TYPE_JSON) {
                                return C8oTranslator.stringToJSON(localCacheResponse.getResponse());
                            }
                        }
                    } catch (C8oUnavailableLocalCacheException ex) {
                        // no entry
                    }
                }
                return e;
            } catch (C8oException e) {
                return new C8oException(C8oExceptionMessage.handleC8oCallRequest(), e);
            }

            int httpCode = httpResponse.getStatusLine().getStatusCode();
            c8o.log._info("(C8oCallTask) Http response code: " + httpCode);

            Map<String, String> headers = new HashMap<String, String>();
            for (Header header: httpResponse.getAllHeaders()) {
                headers.put(header.getName(), header.getValue());
            }
            c8o.log._debug("(C8oCallTask) Http response headers: " + headers);

            String responseString = null;
            // Get the c8o call result
            try {
                responseStream = httpResponse.getEntity().getContent();
            } catch (IllegalStateException e) {
                return new C8oResponseException(C8oExceptionMessage.getInputStreamFromHttpResponse(), e, httpCode, headers, responseString);
            } catch (IOException e) {
                return new C8oResponseException(C8oExceptionMessage.getInputStreamFromHttpResponse(), e, httpCode, headers, responseString);
            }

            //*** Handle response ***//

            // Define the return type
            Object response;
            byte[] byteArray;
            try {
                byteArray = IOUtils.toByteArray(responseStream);
                responseStream = new ByteArrayInputStream(byteArray);
            } catch (IOException e) {
                return new C8oResponseException("Failed to read the stream response", e, httpCode, headers, responseString);
            }
            try {
                Header headerContentType = httpResponse.getFirstHeader("Content-Type");
                String encoding = "UTF-8";
                if (headerContentType != null) {
                    Matcher m = pCharset.matcher("" + headerContentType.getValue());
                    if (m.find()) {
                        encoding = m.group(1);
                    }
                }
                responseString = new String(byteArray, encoding);
                c8o.log._trace("(C8oCallTask) Http response string:\n" + responseString);
            } catch (UnsupportedEncodingException e) {
                return new C8oResponseException("Failed to decode response content", e, httpCode, headers, responseString);
            }

            if (httpCode < 200 || httpCode >= 300) {
                return new C8oResponseException("Expected HTTP response 2xx but was " + httpCode, httpCode, headers, responseString);
            }
            if (c8oResponseListener instanceof C8oResponseXmlListener) {
                try {
                    response = C8oTranslator.inputStreamToXMLAndClose(responseStream, c8o.getDocumentBuilder());
                } catch (C8oException e) {
                    return new C8oResponseException(C8oExceptionMessage.inputStreamToXML(), e, httpCode, headers, responseString);
                }
            } else if (c8oResponseListener instanceof C8oResponseJsonListener) {
                try {
                    response = new JSONObject(responseString);
                } catch (JSONException e) {
                    return new C8oResponseException(C8oExceptionMessage.parseStringToJson(), e, httpCode, headers, responseString);
                }
            } else {
                // Return an Exception because the C8oListener used is unknown
                return new C8oResponseException(C8oExceptionMessage.wrongListener(c8oResponseListener), httpCode, headers, responseString);
            }

            if (localCacheEnabled) {
                try {
                    long expirationDate = -1;
                    if (localCache.ttl > 0) {
                        expirationDate = localCache.ttl + System.currentTimeMillis();
                    }
                    C8oLocalCacheResponse localCacheResponse = new C8oLocalCacheResponse(responseString, responseType, expirationDate);
                    c8o.c8oFullSync.saveResponseToLocalCache(c8oCallRequestIdentifier, localCacheResponse);
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
            if (c8oResponseListener == null) {
                return;
            }

            if (result instanceof Document) {
                c8o.log.logC8oCallXMLResponse((Document) result, c8oCallUrl, parameters);
                ((C8oResponseXmlListener) c8oResponseListener).onXmlResponse((Document) result, parameters);
            } else if (result instanceof JSONObject) {
                c8o.log.logC8oCallJSONResponse((JSONObject) result, c8oCallUrl, parameters);
                ((C8oResponseJsonListener) c8oResponseListener).onJsonResponse((JSONObject) result, parameters);
            } else if (result instanceof com.couchbase.lite.Document) {
                // TODO log

                // The result is a fillSync query response
                ((C8oResponseCblListener) c8oResponseListener).onDocumentResponse((com.couchbase.lite.Document) result, parameters);
            } else if (result instanceof QueryEnumerator) {
                // TODO log

                // The result is a fillSync query response
                ((C8oResponseCblListener) c8oResponseListener).onQueryEnumeratorResponse((QueryEnumerator) result, parameters);
            } else if (result instanceof Exception){
                // The result is an Exception
                c8o.handleCallException(c8oExceptionListener, parameters, (Exception) result);
            } else {
                // The result type is unknown
                c8o.handleCallException(c8oExceptionListener, parameters, new C8oException(C8oExceptionMessage.wrongResult(result)));
            }
        } catch (Exception e) {
            c8o.handleCallException(c8oExceptionListener, parameters, e);
        }
    }
}
