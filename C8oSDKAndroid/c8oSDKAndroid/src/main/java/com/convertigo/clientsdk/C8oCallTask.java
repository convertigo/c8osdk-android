package com.convertigo.clientsdk;

import android.os.AsyncTask;
import android.util.Log;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oExceptionMessage;
import com.convertigo.clientsdk.exception.C8oHttpRequestException;
import com.convertigo.clientsdk.exception.C8oUnavailableLocalCacheException;
import com.convertigo.clientsdk.listener.C8oExceptionListener;
import com.convertigo.clientsdk.listener.C8oResponseCblListener;
import com.convertigo.clientsdk.listener.C8oResponseJsonListener;
import com.convertigo.clientsdk.listener.C8oResponseListener;
import com.convertigo.clientsdk.listener.C8oResponseXmlListener;
import com.convertigo.clientsdk.util.C8oUtils;
import com.convertigo.clientsdk.C8oEnum.*;
import com.couchbase.lite.QueryEnumerator;

import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

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
class C8oCallTask extends AsyncTask<Void, Void, Object> {
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

        c8o.c8oLogger.logMethodCall("C8oCallTask", parameters, c8oResponseListener, c8oExceptionListener);
    }

    // Perform background operations
    @Override
    protected Object doInBackground(Void... params) {
        try {
            Object response = handleRequest();
            handleResponse(response);
        } catch (C8oException e) {
            e.printStackTrace();
            this.c8oExceptionListener.onException(e, parameters);
        }
        return null;
    }

    // Publish results after doInBackground()
    @Override
    protected void onPostExecute(Object result) {

    }

    private Object handleRequest() throws C8oException {
        boolean isFullSyncRequest = C8oFullSync.isFullSyncRequest(parameters);

        if (isFullSyncRequest) {
			c8o.log(Log.DEBUG, "Is FullSync request");
            try {
				Object fullSyncResult = c8o.c8oFullSync.handleFullSyncRequest(parameters, c8oResponseListener);
				return fullSyncResult;
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

            // Allows to enable or disable the local cache on a Convertigo requestable, default value is true
            boolean localCacheEnabled = false;

            // Defines the time to live of the cached response, in milliseconds
            long localCacheTimeToLive = -1;

            // Checks if the local cache must be used
            Object localCacheParameterValue = C8oUtils.getParameterObjectValue(parameters, C8o.ENGINE_PARAMETER_LOCAL_CACHE, false);

            // If the engine parameter for local cache is specified
            if (localCacheParameterValue != null) {
                // Checks if this is a JSON object (represented by a LinkedHashMap once translated)
                Map<String, Object> localCacheParameters = (Map<String, Object>) localCacheParameterValue;
                // The local cache policy
                try {
                    // Gets local cache properties
                    localCacheEnabled = C8oUtils.getParameterAndCheckType(localCacheParameters, C8o.LOCAL_CACHE_PARAMETER_KEY_ENABLED, Boolean.class, false, true);
                } catch (C8oException e) {
                    return new C8oException(C8oExceptionMessage.getLocalCacheParameters(), e);
                }
                if (localCacheEnabled) {
                    // Checks if the same request is stored in the local cache
                    parameters.remove(C8o.ENGINE_PARAMETER_LOCAL_CACHE);
                    try {
                        c8oCallRequestIdentifier = C8oUtils.identifyC8oCallRequest(parameters, responseType);
                    } catch (C8oException e) {
                        return new C8oException(C8oExceptionMessage.serializeC8oCallRequest(), e);
                    }

                    String localCachePolicyStr = C8oUtils.getParameterAndCheckType(localCacheParameters, C8o.LOCAL_CACHE_PARAMETER_KEY_POLICY, String.class, true, null);
                    LocalCachePolicy localCachePolicy = LocalCachePolicy.getLocalCachePolicy(localCachePolicyStr);

                    if (localCachePolicy != null && localCachePolicy.isAvailable(c8o.getContext())) {
                        try {
                            C8oLocalCacheResponse localCacheResponse = c8o.c8oFullSync.getResponseFromLocalCache(c8oCallRequestIdentifier);
                            if (!localCacheResponse.isExpired()) {
                                //TODO return response
                            }
                        } catch (C8oUnavailableLocalCacheException e) {
                            throw new C8oException("TODO", e);
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
            } catch (C8oException e) {
                return new C8oException(C8oExceptionMessage.handleC8oCallRequest(), e);
            } catch (C8oHttpRequestException e) {
                if (localCacheEnabled) {
                    /*
                    try {
                        return c8o.getResponseFromLocalCacheDocument(localCacheDocument, C8oEnum.LocalCachePolicy.PRIORITY_LOCAL.value);
                    } catch (C8oException e1) {
                        return new C8oException(C8oExceptionMessage.handleC8oCallRequest(), e);
                    } catch (C8oUnavailableLocalCacheException e1) {
                        return new C8oException(C8oExceptionMessage.handleC8oCallRequest(), e);
                    }
                    */
                } else {
                    return new C8oException(C8oExceptionMessage.handleC8oCallRequest(), e);
                }
            }
            // Get the c8o call result
            try {
                responseStream = httpResponse.getEntity().getContent();
            } catch (IllegalStateException e) {
                return new C8oException(C8oExceptionMessage.getInputStreamFromHttpResponse(), e);
            } catch (IOException e) {
                return new C8oException(C8oExceptionMessage.getInputStreamFromHttpResponse(), e);
            }

            //*** Handle response ***//

            // Define the return type
            Object response;
            String responseString = null;
            if (c8oResponseListener instanceof C8oResponseXmlListener) {
                try {
                    if (localCacheEnabled) {
                        try {
                            responseString = C8oTranslator.inputStreamToString(responseStream);
                        } catch (C8oException e) {
                            return new C8oException(C8oExceptionMessage.parseInputStreamToString(), e);
                        }
                    }
                    response = C8oTranslator.inputStreamToXMLAndClose(responseStream, c8o.getDocumentBuilder());
                } catch (C8oException e) {
                    return new C8oException(C8oExceptionMessage.inputStreamToXML(), e);
                }
            } else if (c8oResponseListener instanceof C8oResponseJsonListener) {
                try {
                    try {
                        responseString = C8oTranslator.inputStreamToString(responseStream);
                    } catch (C8oException e) {
                        throw new C8oException(C8oExceptionMessage.parseInputStreamToString(), e);
                    }
                    response = C8oTranslator.stringToJSON(responseString);
                } catch (C8oException e) {
                    return e;
                }
            } else {
                // Return an Exception because the C8oListener used is unknown
                return new C8oException(C8oExceptionMessage.wrongListener(c8oResponseListener));
            }

            if (localCacheEnabled) {
                try {
                    // String responseString = C8oTranslator.StreamToString(responseStream);
                    long expirationdate = localCacheTimeToLive;
                    if (expirationdate > 0) {
                        expirationdate = expirationdate + System.currentTimeMillis();
                    }
                    C8oLocalCacheResponse localCacheResponse = new C8oLocalCacheResponse(responseString, null, expirationdate);
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
                c8o.c8oLogger.logC8oCallXMLResponse((Document) result, c8oCallUrl, parameters);
                ((C8oResponseXmlListener) c8oResponseListener).onXmlResponse((Document) result, parameters);
            } else if (result instanceof JSONObject) {
                c8o.c8oLogger.logC8oCallJSONResponse((JSONObject) result, c8oCallUrl, parameters);
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
                C8o.handleCallException(c8oExceptionListener, parameters, (Exception) result);
            } else {
                // The result type is unknown
                C8o.handleCallException(c8oExceptionListener, parameters, new C8oException(C8oExceptionMessage.wrongResult(result)));
            }
        } catch (Exception e) {
            C8o.handleCallException(c8oExceptionListener, parameters, e);
        }
    }
}
