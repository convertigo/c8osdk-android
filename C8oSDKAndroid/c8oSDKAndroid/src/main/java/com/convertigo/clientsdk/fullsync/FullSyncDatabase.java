package com.convertigo.clientsdk.fullsync;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.cookie.Cookie;

import com.convertigo.clientsdk.C8o;
import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oExceptionMessage;
import com.convertigo.clientsdk.fullsync.FullSyncEnum.FullSyncReplicateDatabaseParameter;
import com.convertigo.clientsdk.listener.C8oFullSyncChangeEventListener;
import com.convertigo.clientsdk.listener.C8oFullSyncResponseListener;
import com.convertigo.clientsdk.listener.C8oJSONChangeEventListener;
import com.convertigo.clientsdk.listener.C8oJSONResponseListener;
import com.convertigo.clientsdk.listener.C8oResponseListener;
import com.convertigo.clientsdk.listener.C8oXMLChangeEventListener;
import com.convertigo.clientsdk.listener.C8oXMLResponseListener;
import com.convertigo.clientsdk.util.C8oUtils;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.Replication.ChangeListener;


public class FullSyncDatabase {
	
	//*** TAG Constants ***//
	
	/**
	 * The name of the authentication cookie.
	 */
	private final static String AUTHENTICATION_COOKIE_NAME = "SyncGatewaySession";
	/**
	 * The suffix added to locally replicated databases name.
	 */
	private static final String LOCAL_DATABASE_SUFFIX = "_mobile";
	
	//*** TAG Attributes ***//
	
	/**
	 * The fullSync database name.
	 */
	public String databaseName;
	/**
	 * The fullSync Database instance.
	 */
	public Database database;
	/**
	 * Used to make pull replication (uploads changes from the local database to the remote one).
	 */
    private FullSyncReplication pullFullSyncReplication;
    /**
	 * Used to make push replication (downloads changes from the remote database to the local one).
	 */
    private FullSyncReplication pushFullSyncReplication;
    /**
     * ???
     */
    // private boolean basicAuth;
    /**
     * Used to log.
     */
    private C8o c8o;

    //*** TAG Constructors ***//
    
    /**
     * Creates a fullSync database with the specified name and its location.
     * 
     * @param c8o
     * @param manager
     * @param databaseName
     * @param fullSyncDatabasesUrlStr
     * @throws C8oException Failed to get the fullSync database.
     */
    public FullSyncDatabase(C8o c8o, Manager manager, String databaseName, String fullSyncDatabasesUrlStr) throws C8oException {
        this.c8o = c8o;

    	try {
			this.database = manager.getDatabase(databaseName + FullSyncDatabase.LOCAL_DATABASE_SUFFIX);
		} catch (CouchbaseLiteException e) {
			throw new C8oException(C8oExceptionMessage.unableToGetFullSyncDatabase(databaseName), e);
		}
        this.databaseName = databaseName;
        
        URL fullSyncDatabaseUrl;
        // The "/" at the end is important
        String fullSyncDatabaseUrlStr = fullSyncDatabasesUrlStr + this.databaseName + "/";
		try {
			fullSyncDatabaseUrl = new URL(fullSyncDatabaseUrlStr);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidFullSyncDatabaseUrl(fullSyncDatabaseUrlStr), e);
		}

        Replication pullReplication = database.createPullReplication(fullSyncDatabaseUrl);
        Replication pushReplication = database.createPushReplication(fullSyncDatabaseUrl);
        
        // ??? Does surely something but do not know what, it is optional so it is still here ???
        String authenticationCookieValue = c8o.getAuthenticationCookieValue();
        if (authenticationCookieValue != null) {
        	// Create the expiration being tomorrow
        	Calendar calendar = Calendar.getInstance();
        	calendar.setTime(new Date());
        	int numDaysToAdd = 1;
        	calendar.add(Calendar.DATE, numDaysToAdd);
        	Date expirationDate = calendar.getTime();
  
        	boolean isSecure = false;
        	boolean httpOnly = false;
        	
        	pullReplication.setCookie(FullSyncDatabase.AUTHENTICATION_COOKIE_NAME, authenticationCookieValue, "/", expirationDate, isSecure, httpOnly);
        	pushReplication.setCookie(FullSyncDatabase.AUTHENTICATION_COOKIE_NAME, authenticationCookieValue, "/", expirationDate, isSecure, httpOnly);
        }
        
        this.pullFullSyncReplication = new FullSyncReplication(pullReplication);
        this.pushFullSyncReplication = new FullSyncReplication(pushReplication);
    }
    
    //*** TAG Replication ***//

    /**
     * Start pull and push replications.
     */
    public void startAllReplications(C8oResponseListener c8oResponseListener, List<NameValuePair> parameters) {
	    this.startPullReplication(c8oResponseListener, parameters);
	    this.startPushReplication(c8oResponseListener, parameters);
    }

    /**
     * Start pull replication.
     */
    public void startPullReplication(C8oResponseListener c8oResponseListener, List<NameValuePair> parameters) {
    	startReplication(this.c8o, this.pullFullSyncReplication, c8oResponseListener, parameters);
    }
    
    /**
     * Start push replication.
     * @return 
     */
    public void startPushReplication(C8oResponseListener c8oResponseListener, List<NameValuePair> parameters) {
    	startReplication(this.c8o, this.pushFullSyncReplication, c8oResponseListener, parameters);
    }

    /**
     * Starts a replication taking into account parameters.<br/>
     * This action does not directly return something but setup a callback raised when the replication raises change events.
     * 
     * @param c8o
     * @param c8oReplication
     * @param c8oResponseListener
     * @param parameters
     */
    private static void startReplication(C8o c8o, FullSyncReplication c8oReplication, C8oResponseListener c8oResponseListener, List<NameValuePair> parameters) {
    	synchronized (c8oReplication.replication) {
    		
    		// Cancel the replication if it is already running
    		if (c8oReplication.replication.isRunning()) {
    			c8oReplication.replication.stop();
    		}
    		
    		// Adds parameter to the replication
    		for (FullSyncReplicateDatabaseParameter fullSyncParameter : FullSyncReplicateDatabaseParameter.values()) {
    			String parameterValue = C8oUtils.getParameterStringValue(parameters, fullSyncParameter.name, false);
    			if (parameterValue != null) {
    				// Cancel the replication
    				if (fullSyncParameter == FullSyncReplicateDatabaseParameter.CANCEL && parameterValue.equals("true")) {
    					return;
    				}
    				fullSyncParameter.setReplication(c8oReplication.replication, parameterValue);
    			}
    		}
    		
    		List<Cookie> cookies = c8o.getCookieStore().getCookies();
    		for (Cookie cookie : cookies) {
    			c8oReplication.replication.setCookie(cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getExpiryDate(), cookie.isSecure(), false);
    		}
    		
    		// Removes the current change listener
    		if (c8oReplication.changeListener != null) {
    			c8oReplication.replication.removeChangeListener(c8oReplication.changeListener);
    		}
    		// Replaces the change listener by a new one according to the C8oResponseListener type
    		c8oReplication.changeListener = null;
    		if (c8oResponseListener != null) {
    			if (c8oResponseListener instanceof C8oFullSyncResponseListener) {
    				c8oReplication.changeListener = new C8oFullSyncChangeEventListener((C8oFullSyncResponseListener) c8oResponseListener, c8oReplication.replication, parameters);
    			} else if (c8oResponseListener instanceof C8oJSONResponseListener) {
    				c8oReplication.changeListener = new C8oJSONChangeEventListener((C8oJSONResponseListener) c8oResponseListener, c8oReplication.replication, parameters);
    			} else if (c8oResponseListener instanceof C8oXMLResponseListener) {
    				c8oReplication.changeListener = new C8oXMLChangeEventListener((C8oXMLResponseListener) c8oResponseListener, c8oReplication.replication, c8o.getDocumentBuilder(), parameters);
    			}
    			// TODO else error ?
    			
    			if (c8oReplication.changeListener != null) {
    				c8oReplication.replication.addChangeListener(c8oReplication.changeListener);
    			}
    		}
    		
    		// Finally starts the replication
    		c8oReplication.replication.start();
		}
    }
    
    /**
     * Stops and destroys pull and push replications.
     */
    public void destroyReplications() {
    	if (pullFullSyncReplication.replication != null) {
    		pullFullSyncReplication.replication.stop();
    		pullFullSyncReplication.replication.deleteCookie(FullSyncDatabase.AUTHENTICATION_COOKIE_NAME);
    		pullFullSyncReplication.replication = null;
    	}
    	pullFullSyncReplication = null;
    	
    	if (pushFullSyncReplication.replication != null) {
    		pushFullSyncReplication.replication.stop();
    		pushFullSyncReplication.replication.deleteCookie(FullSyncDatabase.AUTHENTICATION_COOKIE_NAME);
    		pushFullSyncReplication.replication = null;
    	}
    	pushFullSyncReplication = null;
    }
    
    //*** TAG Getter / Setter ***//
    
    public String getDatabaseName() {
    	return this.databaseName;
    }
    
    public Database getDatabase() {
    	return this.database;
    }
    
    //*** TAG private classes ***//
    
    /**
     * Combines the Replication and the ChangeListener instances.
     */
    private class FullSyncReplication {
    	
    	public Replication replication;
    	public ChangeListener changeListener;
    	
    	// Replication history -> ?
		// Replication id version -> ?
		// Session ID -> replication.getSessionID();
		// Source last seq -> replication.getLocalDatabase().getLastSequenceNumber();
    	
    	public FullSyncReplication (Replication replication) {
    		this.replication = replication;
    		this.changeListener = null;
    	}
    	
    }
    
}
