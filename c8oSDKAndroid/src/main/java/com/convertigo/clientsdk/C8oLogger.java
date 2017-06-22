package com.convertigo.clientsdk;

import android.os.AsyncTask;
import android.util.Log;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oHttpRequestException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

public class C8oLogger {
    private final static Pattern RE_FORMAT_TIME = Pattern.compile("(\\d*?)([\\d ]{4})((?:\\.[\\d ]{3})|(?: {4}))");
	//*** Constants ***//

	/**
	 * The log tag used by the SDK.
	 */
	private final static String LOG_TAG = "c8o";
    private final static String LOG_INTERNAL_PREFIX = "[" + LOG_TAG + "] ";

	/**
	 * The maximum number of logs sent to the Convertigo server in one time.
	 */
	private final static int REMOTE_LOG_LIMIT = 100;

    /**
     * The JSON key used to get the Convertigo server log level.
     */
    private final static String JSON_KEY_REMOTE_LOG_LEVEL = "remoteLogLevel";
    private final static String JSON_KEY_TIME = "time";
    private final static String JSON_KEY_LEVEL = "level";
    private final static String JSON_KEY_MESSAGE = "msg";
    private final static String JSON_KEY_LOGS = "logs";
    private final static String JSON_KEY_ENV = "env";

	/**
	 * Convertigo log levels fixed to fit with Android log levels.<br/>
	 * Android log levels : 2 : VERBOSE, 3 : DEBUG, 4 : INFO, 5 : WARN, 6 : ERROR, 7 : ASSERT
	 */
	private final static String[] REMOTE_LOG_LEVELS = {"", "none", "trace", "debug", "info", "warn", "error", "fatal"};
	
	//*** Attributes ***//

	/**
	 * The URL with which logs are sent.
	 */
	private String remoteLogUrl;
	/**
	 * Contains logs to be sent to the Convertigo server.
	 */
	private LinkedBlockingQueue<JSONObject> remoteLogs;
	/**
	 * Indicate if a thread is logging.
	 */
	private boolean[] alreadyRemoteLogging;
	/**
	 * The log level return by the Convertigo server.
	 */
	private int remoteLogLevel;
	/**
	 * The UID sent to the Convertigo server.
	 */
	private String uidRemoteLogs;
	/**
	 * The date in milliseconds at the creation of the C8o instance.
	 */
	private long startTimeRemoteLog;
	/**
	 * Used to format the time parameter sent to the Convertigo server.
	 */
	private DecimalFormat logTimeFormat;
	/**
	 * Used to run HTTP requests.<br/>
	 * It is set out of the constructor.
	 */
	private C8o c8o;

	private String env;
	
	C8oLogger(C8o c8o) {
		this.c8o = c8o;

        remoteLogUrl = c8o.getEndpointConvertigo() + "/admin/services/logs.Add";
		remoteLogs = new LinkedBlockingQueue<JSONObject>();
		alreadyRemoteLogging = new boolean[] {false};

		remoteLogLevel = 0;

		long currentTime = new Date().getTime();
		startTimeRemoteLog = currentTime;
		uidRemoteLogs = Long.toString(currentTime, 16);
		logTimeFormat = new DecimalFormat(".###");
		try {
			env = new JSONObject()
                .put("uid", uidRemoteLogs)
                .put("uuid", c8o.getDeviceUUID())
                .put("project", c8o.getEndpointProject()).toString();
		} catch (Exception e) {
			// couldn't happen
		}
	}
	
	private boolean isLoggableRemote(int logLevel) {
		return c8o.isLogRemote()
				&& 1 < logLevel
				&& logLevel < C8oLogger.REMOTE_LOG_LEVELS.length
                && logLevel >= remoteLogLevel;
	}
	
	private boolean isLoggableConsole(int logLevel) {
		return 1 < logLevel
                && logLevel < C8oLogger.REMOTE_LOG_LEVELS.length
                && logLevel >= c8o.getLogLevelLocal();
	}

    public boolean canLog(int logLevel) {
        return isLoggableConsole(logLevel) || isLoggableRemote(logLevel);
    }

    public boolean isFatal() {
        return canLog(Log.ASSERT);
    }

    public boolean isError() {
        return canLog(Log.ERROR);
    }

    public boolean isWarn() {
        return canLog(Log.WARN);
    }

    public boolean isInfo() {
        return canLog(Log.INFO);
    }

    public boolean isDebug() {
        return canLog(Log.DEBUG);
    }

    public boolean isTrace() {
        return canLog(Log.VERBOSE);
    }

//    public void log(int logLevel, String message) {
//        log(logLevel, message, null);
//    }
	/**
	 * Log the message in the console and in Convertigo server if the log level is enough high.<br/>
	 * Android : 1 : NONE, 2 : VERBOSE, 3 : DEBUG, 4 : INFO, 5 : WARN, 6 : ERROR, 7 : ASSERT<br/>
	 * Convertigo : 1 : NONE, 2 : TRACE, 3 : DEBUG, 4 : INFO, 5 : WARN, 6 : ERROR, 7 : FATAL
	 * 
	 * @param logLevel
	 * @param message
	 * @throws UnsupportedEncodingException
	 * @throws JSONException
	 * @throws InterruptedException
	 */
	public void log(int logLevel, String message, Exception exception) {
		boolean isLoggableConsole = isLoggableConsole(logLevel);
        boolean isLoggableRemote = isLoggableRemote(logLevel);

        if (isLoggableConsole || isLoggableRemote) {
            if (exception != null) {
                message += "\n" + exception;
            }

            long time = (new Date().getTime() - this.startTimeRemoteLog) / 1000;
            String stringLevel = REMOTE_LOG_LEVELS[logLevel];

            if (isLoggableRemote) {
                // If the capacity of the queue is reached then it will wait here
                try {
                    remoteLogs.add(new JSONObject()
                            .put(JSON_KEY_TIME, "" + time)
                            .put(JSON_KEY_LEVEL, stringLevel)
                            .put(JSON_KEY_MESSAGE, message));
                    logRemote();
                } catch (Throwable t){
                    Log.println(Log.DEBUG, C8oLogger.LOG_TAG, "[C8oLogger] Failed to queue remote log: " + t);
                }
            }

            if (isLoggableConsole) {
                Log.println(logLevel, C8oLogger.LOG_TAG, this.logTimeFormat.format(time) + " [" + stringLevel + "] " + message);
            }
        }
	}

    public void fatal(String message) {
        log(Log.ASSERT, message, null);
    }

    public void fatal(String message, Exception exception) {
        log(Log.ASSERT, message, exception);
    }

    public void error(String message) {
        log(Log.ERROR, message, null);
    }

    public void error(String message, Exception exception) {
        log(Log.ERROR, message, exception);
    }

    public void warn(String message) {
        log(Log.WARN, message, null);
    }

    public void warn(String message, Exception exception) {
        log(Log.WARN, message, exception);
    }

    public void info(String message) {
        log(Log.INFO, message, null);
    }

    public void info(String message, Exception exception) {
        log(Log.INFO, message, exception);
    }

    public void debug(String message) {
        log(Log.DEBUG, message, null);
    }

    public void debug(String message, Exception exception) {
        log(Log.DEBUG, message, exception);
    }

    public void trace(String message) {
        log(Log.VERBOSE, message, null);
    }

    public void trace(String message, Exception exception) {
        log(Log.VERBOSE, message, exception);
    }

    void _log(int logLevel, String message) {
        _log(logLevel, message, null);
    }

	void _log(int logLevel, String message, Exception exception) {
		if (c8o.isLogC8o()) {
			log(logLevel, LOG_INTERNAL_PREFIX + message, exception);
		}
	}

    void _fatal(String message) {
        _log(Log.ASSERT, message, null);
    }

    void _fatal(String message, Exception exception) {
        _log(Log.ASSERT, message, exception);
    }

    void _error(String message) {
        _log(Log.ERROR, message, null);
    }

    void _error(String message, Exception exception) {
        _log(Log.ERROR, message, exception);
    }

    void _warn(String message) {
        _log(Log.WARN, message, null);
    }

    void _warn(String message, Exception exception) {
        _log(Log.WARN, message, exception);
    }

    void _info(String message) {
        _log(Log.INFO, message, null);
    }

    void _info(String message, Exception exception) {
        _log(Log.INFO, message, exception);
    }

    void _debug(String message) {
        _log(Log.DEBUG, message, null);
    }

    void _debug(String message, Exception exception) {
        _log(Log.DEBUG, message, exception);
    }

    void _trace(String message) {
        _log(Log.VERBOSE, message, null);
    }

    void _trace(String message, Exception exception) {
        _log(Log.VERBOSE, message, exception);
    }
	
	/** 
	 * Sends stored logs to the Convertigo server if there is not already another thread doing it.
	 * 
	 * @throws InterruptedException
	 * @throws JSONException
	 * @throws UnsupportedEncodingException
	 */
	private void logRemote() {
		// Check if there is another thread logging
		boolean canLog = false;
		synchronized (alreadyRemoteLogging) {
			canLog = !alreadyRemoteLogging[0] && !remoteLogs.isEmpty();
			if (canLog) {
				alreadyRemoteLogging[0] = true;
			}
		}
		
		if (canLog) {
			AsyncTask<Void, Void, JSONObject> remoteLogTask = new AsyncTask<Void, Void, JSONObject>() {
				@Override
				protected JSONObject doInBackground(Void... params) {
					List<NameValuePair> parameters = new LinkedList<NameValuePair>();
					try {
						// Take logs in the queue and add it to a json array
						int count = 0;
						int listSize = remoteLogs.size();
						JSONArray logsArray = new JSONArray();

						while (count < listSize && count < REMOTE_LOG_LIMIT) {
							logsArray.put(remoteLogs.take());
							count++;
						}
						// Http request sending logs
						HttpPost request = new HttpPost(remoteLogUrl);
						parameters.add(new BasicNameValuePair(JSON_KEY_LOGS, logsArray.toString()));
						parameters.add(new BasicNameValuePair(JSON_KEY_ENV, env));
						try {
							request.setEntity(new UrlEncodedFormEntity(parameters));
						} catch (UnsupportedEncodingException e) {
							throw new C8oException(C8oExceptionMessage.urlEncode(), e);
						}
						// Get http response
						HttpResponse response = c8o.httpInterface.handleRequest(request);
						
						JSONObject jsonResponse;
						InputStream inputStreamResponse;
						try {
							inputStreamResponse = response.getEntity().getContent();
						} catch (IllegalStateException e) {
							throw new C8oException(C8oExceptionMessage.getInputStreamFromHttpResponse(), e);
						} catch (IOException e) {
							throw new C8oException(C8oExceptionMessage.remoteLogHttpRequest(), e);
						}
						try {
							jsonResponse = C8oTranslator.inputStreamToJSONAndClose(inputStreamResponse);
						} catch (C8oException e) {
							throw new C8oException(C8oExceptionMessage.inputStreamToJSON(), e);
						}
						
						return jsonResponse;
					} catch (Exception e) {
						// If there is an error then it stop logging remotely
						c8o.setLogRemote(false);
						if (c8o.getLogOnFail() != null) {
							c8o.getLogOnFail().run(e, null);
						} else {
							c8o.handleCallException(null, null, e);
						}
					}
					return null;
				}

				@Override
				protected void onPostExecute(JSONObject result) {
					if (result != null) {
						try {
							String logLevelStr = (String) result.get(C8oLogger.JSON_KEY_REMOTE_LOG_LEVEL);
							int logLevel = Arrays.asList(C8oLogger.REMOTE_LOG_LEVELS).indexOf(logLevelStr);
							if (logLevel > 1 && logLevel < C8oLogger.REMOTE_LOG_LEVELS.length) {
								remoteLogLevel = logLevel;
							}
							logRemote();
						} catch (Exception e) {
							// If there is an error then it stop logging remotely
							c8o.setLogRemote(false);
							if (c8o.getLogOnFail() != null) {
								c8o.getLogOnFail().run(e, null);
							} else {
								c8o.handleCallException(null, null, e);
							}
						}
					}

                    synchronized(alreadyRemoteLogging) {
                        alreadyRemoteLogging[0] = false;
                    }
				}
			};
			remoteLogTask.execute();
		}
	}
	
	/**
	 * Log the method call in DEBUG log level and log method parameters in VERBOSE log level.
	 * 
	 * @param methodName - The method name
	 * @param params - Array containing method parameters
	 * @throws JSONException 
	 * @throws InterruptedException 
	 * @throws UnsupportedEncodingException 
	 */
	public void logMethodCall(String methodName, Object... params) {
        if (c8o.isLogC8o() && isDebug()) {
			String methodCallLogMessage = "Method call : " + methodName;

			if (isTrace()) {
				methodCallLogMessage += ", Parameters : [";
				// Add parameters
				for (Object param : params) {
					String paramStr = "null";
					if (param != null) {
						paramStr = param.toString();
					}
					methodCallLogMessage += "\n" + paramStr + ", ";
				}
				// Remove the last character
				methodCallLogMessage = methodCallLogMessage.substring(0, methodCallLogMessage.length() - 2) + "]";
				
				_trace(methodCallLogMessage);
			} else {
                _debug(methodCallLogMessage);
			}
		}
	}
	
	/**
	 * Log the c8o call in DEBUG log level.
	 * 
	 * @param url - The c8o call URL
	 * @param parameters - c8o call parameters
	 * @throws JSONException 
	 * @throws InterruptedException 
	 * @throws UnsupportedEncodingException 
	 */
	public void logC8oCall(String url, Map<String, Object> parameters) {
		boolean isLoggableConsole = isLoggableConsole(Log.DEBUG);
		boolean isLoggableRemote = isLoggableRemote(Log.DEBUG);
		
		if (c8o.isLogC8o() && isDebug()) {
			String c8oCallLogMessage = "C8o call :" + 
				" URL=" + url + ", " + 
				" Parameters=" + parameters;
			
			_debug(c8oCallLogMessage);
		}
	}
	
	/**
	 * Log the c8o call XML response in VERBOSE (TRACE) level.
	 * 
	 * @param response
	 * @param url
	 * @param parameters
	 * @throws C8oException 
	 * @throws JSONException 
	 * @throws InterruptedException 
	 * @throws UnsupportedEncodingException 
	 */
	public void logC8oCallXMLResponse(Document response, String url, Map<String, Object> parameters) throws C8oException {
        logC8oCallResponse(C8oTranslator.xmlToString(response), "XML", url, parameters);
	}
	
	/**
	 * Log the c8o call JSON response in VERBOSE (TRACE) level.
	 * 
	 * @param response
	 * @param url
	 * @param parameters
	 * @throws JSONException 
	 * @throws InterruptedException 
	 * @throws UnsupportedEncodingException 
	 */
	public void logC8oCallJSONResponse(JSONObject response, String url, Map<String, Object> parameters) {
        try {
            logC8oCallResponse(response.toString(2), "JSON", url, parameters);
        } catch (JSONException e) {
            // not probable
            e.printStackTrace();
        }
    }

    public void logC8oCallResponse(String responseStr, String responseType, String url, Map<String, Object> parameters) {
        if (c8o.isLogC8o() && isTrace()) {
            String c8oCallResponseLogMessage = "C8o call JSON response :" +
                    " URL=" + url + ", " +
                    " Parameters=" + parameters + ", " +
                    " Response=" + responseStr;

            _trace(c8oCallResponseLogMessage);
        }
    }
}
