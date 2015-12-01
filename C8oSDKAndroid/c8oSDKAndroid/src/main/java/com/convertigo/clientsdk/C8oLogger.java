package com.convertigo.clientsdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import android.os.AsyncTask;
import android.util.Log;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oExceptionMessage;
import com.convertigo.clientsdk.exception.C8oHttpRequestException;

public class C8oLogger {
	
	//*** Constants ***//
	
	/**
	 * The JSON key used to get the Convertigo server log level.
	 */
	private final static String REMOTE_LOG_LEVEL_KEY = "remoteLogLevel";
	/**
	 * The log tag used by the SDK.
	 */
	private final static String LOG_TAG = "c8o";
	/**
	 * The maximum number of logs sent to the Convertigo server in one time.
	 */
	private final static int REMOTE_LOG_LIMIT = 100;
	/**
	 * Convertigo log levels fixed to fit with Android log levels.<br/>
	 * Android log levels : 2 : VERBOSE, 3 : DEBUG, 4 : INFO, 5 : WARN, 6 : ERROR, 7 : ASSERT
	 */
	private final static String[] REMOTE_LOG_LEVELS = {"", "none", "trace", "debug", "info", "warn", "error", "fatal"};
	
	//*** Attributes ***//
	
	/**
	 * Indicates if there are logs remotely.
	 */
	private boolean isLogRemote;
	/**
	 * The URL with which logs are sent.
	 */
	private String remoteLogUrl;
	/**
	 * Contains logs to be sent to the Convertigo server.
	 */
	private LinkedBlockingQueue<C8oLog> remoteLogs;
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
	private HttpInterface httpInterface;
	private String deviceUuid;
	private C8o c8o;
	
	public C8oLogger(C8o c8o) {
		this.c8o = c8o;
		
		// Remote log
		this.isLogRemote = false;
		this.remoteLogs = new LinkedBlockingQueue<C8oLog>();
		this.alreadyRemoteLogging = new boolean[] {false};
		this.remoteLogLevel = 0;
		long currentTime = new Date().getTime();
		this.startTimeRemoteLog = currentTime;
		this.uidRemoteLogs = Long.toString(currentTime, 16);
		this.logTimeFormat = new DecimalFormat(".###");
	}
	
	private boolean isLoggableRemote(int logLevel) {
		return this.isLogRemote && logLevel > 1 && logLevel < C8oLogger.REMOTE_LOG_LEVELS.length && logLevel >= this.remoteLogLevel;
	}
	
	private boolean isLoggableConsole(int logLevel) {
		return Log.isLoggable(C8oLogger.LOG_TAG, logLevel);
	}
	
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
	public void log(int logLevel, String message) {
		this.log(logLevel, message, this.isLoggableConsole(logLevel), this.isLoggableRemote(logLevel));
	}
	
	private void log(int logLevel, String message, boolean isLoggableConsole, boolean isLoggableRemote) {
		if (isLoggableConsole) {
			Log.println(logLevel, C8oLogger.LOG_TAG, message);
		}
		
		if (isLoggableRemote) {
			String time = this.logTimeFormat.format((float) (new Date().getTime() - this.startTimeRemoteLog) / 1000);
			// If the capacity of the queue is reached then it will wait here
			this.remoteLogs.add(new C8oLog(time, logLevel, message));
			this.logRemote();
		}
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
		synchronized (this.alreadyRemoteLogging) {
			canLog = !this.remoteLogs.isEmpty() && !this.alreadyRemoteLogging[0];
			if (canLog) {
				this.alreadyRemoteLogging[0] = true;
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
						int listSize = C8oLogger.this.remoteLogs.size();
						JSONArray logsArray = new JSONArray();
						while (count < listSize && count < C8oLogger.REMOTE_LOG_LIMIT) {
							C8oLog c8oLog;
							try {
								c8oLog = C8oLogger.this.remoteLogs.take();
							} catch (InterruptedException e) {
								throw new C8oException(C8oExceptionMessage.takeLog(), e);
							}
							JSONObject jsonLog = new JSONObject();
							try {
								jsonLog.put("time", c8oLog.time);
								jsonLog.put("level", C8oLogger.REMOTE_LOG_LEVELS[c8oLog.logLevel]);
								jsonLog.put("msg", c8oLog.msg);
							} catch (JSONException e) {
								throw new C8oException(C8oExceptionMessage.putJson(), e);
							}
							logsArray.put(jsonLog);
							count++;
						}
						// Http request sending logs
						HttpPost request = new HttpPost(C8oLogger.this.remoteLogUrl);
						parameters.add(new BasicNameValuePair("logs", logsArray.toString()));
						parameters.add(new BasicNameValuePair("env", "{\"uid\":\"" + C8oLogger.this.uidRemoteLogs + "\"}"));
						parameters.add(new BasicNameValuePair(C8o.ENGINE_PARAMETER_DEVICE_UUID, C8oLogger.this.deviceUuid));
						try {
							request.setEntity(new UrlEncodedFormEntity(parameters));
						} catch (UnsupportedEncodingException e) {
							throw new C8oException(C8oExceptionMessage.urlEncode(), e);
						}
						// Get http response
						HttpResponse response;
						try {
							response = C8oLogger.this.httpInterface.handleRequest(request);
						} catch (C8oHttpRequestException e) {
							throw new C8oException(C8oExceptionMessage.remoteLogHttpRequest(), e);
						}
						
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
						C8oLogger.this.isLogRemote = false;
						if (c8o.getLogOnFail() != null) {
							c8o.getLogOnFail().run(e, null);
						} else {
							C8o.handleCallException(null, null, e);
						}
					}
					return null;
				}

				@Override
				protected void onPostExecute(JSONObject result) {
					synchronized(C8oLogger.this.alreadyRemoteLogging) {
						C8oLogger.this.alreadyRemoteLogging[0] = false;
					}
					
					if (result != null) {
						try {
							String logLevelStr = (String) result.get(C8oLogger.REMOTE_LOG_LEVEL_KEY);
							int logLevel = Arrays.asList(C8oLogger.REMOTE_LOG_LEVELS).indexOf(logLevelStr);
							if (logLevel > 1 && logLevel < C8oLogger.REMOTE_LOG_LEVELS.length) {
								C8oLogger.this.remoteLogLevel = logLevel;
							}
							
							C8oLogger.this.logRemote();
						} catch (Exception e) {
							// If there is an error then it stop logging remotely
							C8oLogger.this.isLogRemote = false;
							if (c8o.getLogOnFail() != null) {
								c8o.getLogOnFail().run(e, null);
							} else {
								C8o.handleCallException(null, null, e);
							}
						}
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
		boolean isLoggableConsole = this.isLoggableConsole(Log.DEBUG);
		boolean isLoggableRemote = this.isLoggableRemote(Log.DEBUG);

		if (isLoggableConsole || isLoggableRemote) {
			String methodCallLogMessage = "Method call : " + methodName;

			isLoggableConsole = this.isLoggableConsole(Log.VERBOSE);
			isLoggableRemote = this.isLoggableRemote(Log.VERBOSE);

			if (isLoggableConsole || isLoggableRemote) {
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
				
				this.log(Log.VERBOSE, methodCallLogMessage, isLoggableConsole, isLoggableRemote);
			} else {
				this.log(Log.DEBUG, methodCallLogMessage, isLoggableConsole, isLoggableRemote);
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
		
		if (isLoggableConsole || isLoggableRemote) {
			String c8oCallLogMessage = "C8o call :" + 
				" URL=" + url + ", " + 
				" Parameters=" + parameters;
			
			this.log(Log.DEBUG, c8oCallLogMessage, isLoggableConsole, isLoggableRemote);
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
		boolean isLoggableConsole = this.isLoggableConsole(Log.VERBOSE);
		boolean isLoggableRemote = this.isLoggableRemote(Log.VERBOSE);
		
		if (isLoggableConsole || isLoggableRemote) {
			String c8oCallXMLResponseLogMessage = "C8o call XML response :" + 
				" URL=" + url + ", " + 
				" Parameters=" + parameters + ", " + 
				" Response=" + C8oTranslator.xmlToString(response);
			
			this.log(Log.VERBOSE, c8oCallXMLResponseLogMessage, isLoggableConsole, isLoggableRemote);
		}
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
		boolean isLoggableConsole = this.isLoggableConsole(Log.VERBOSE);
		boolean isLoggableRemote = this.isLoggableRemote(Log.VERBOSE);
		
		if (isLoggableConsole || isLoggableRemote) {
			String c8oCallJSONResponseLogMessage = "C8o call JSON response :" + 
				" URL=" + url + ", " + 
				" Parameters=" + parameters + ", " + 
				" Response=" + response.toString();
			
			this.log(Log.VERBOSE, c8oCallJSONResponseLogMessage, isLoggableConsole, isLoggableRemote);
		}
	}
	
	public void setRemoteLogParameters(HttpInterface httpInterface, boolean logRemote, String urlBase, String deviceUuid) {
		this.httpInterface = httpInterface;
		this.isLogRemote = logRemote;
		this.remoteLogUrl = urlBase + "/admin/services/logs.Add";
		this.deviceUuid = deviceUuid;
	}
	
	/**
	 * Contains informations related to a log line.
	 */
	private class C8oLog {
		/**
		 * The logged message.
		 */
		public String msg;
		/**
		 * The log priority level.
		 */
		public int logLevel;
		/**
		 * The time elapsed since the instantiation of the C8o object used to send this log.
		 */
		public String time;
		
		public C8oLog(String time, int level, String message) {
			this.time = time;
			this.logLevel = level;
			this.msg = message;
		}
	}
	
}
