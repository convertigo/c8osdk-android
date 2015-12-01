package com.convertigo.clientsdk;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.http.cookie.Cookie;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oExceptionMessage;
import com.convertigo.clientsdk.listener.C8oFullSyncChangeEventListener;
import com.convertigo.clientsdk.listener.C8oResponseCblListener;
import com.convertigo.clientsdk.listener.C8oJSONChangeEventListener;
import com.convertigo.clientsdk.listener.C8oResponseJsonListener;
import com.convertigo.clientsdk.listener.C8oResponseListener;
import com.convertigo.clientsdk.listener.C8oXMLChangeEventListener;
import com.convertigo.clientsdk.listener.C8oResponseXmlListener;
import com.convertigo.clientsdk.util.C8oUtils;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.Replication.ChangeListener;


public class C8oFullSyncDatabase {
	
	//*** TAG Constants ***//
	
	/**
	 * The name of the authentication cookie.
	 */
	private final static String AUTHENTICATION_COOKIE_NAME = "SyncGatewaySession";

	/**
	 * Used to log.
	 */
	private C8o c8o;
	
	//*** TAG Attributes ***//
	
	/**
	 * The fullSync database name.
	 */
    private String databaseName;
	/**
	 * The fullSync Database instance.
	 */
	private Database database = null;
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

    //*** TAG Constructors ***//
    
    /**
     * Creates a fullSync database with the specified name and its location.
     * 
     * @param c8o
     * @param manager
     * @param databaseName
     * @param fullSyncDatabases
     * @throws C8oException Failed to get the fullSync database.
     */
    public C8oFullSyncDatabase(C8o c8o, Manager manager, String databaseName, String fullSyncDatabases, String localSuffix) throws C8oException {
        this.c8o = c8o;

        String fullSyncDatabaseUrl = fullSyncDatabases + databaseName + "/";

        this.databaseName = (databaseName += localSuffix);

    	try {
			database = manager.getDatabase(databaseName);
		} catch (CouchbaseLiteException e) {
			throw new C8oException(C8oExceptionMessage.unableToGetFullSyncDatabase(databaseName), e);
		}
        
        URL fullSyncDatabaseUri;
        // The "/" at the end is important
		try {
			fullSyncDatabaseUri = new URL(fullSyncDatabaseUrl);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidFullSyncDatabaseUrl(fullSyncDatabaseUrl), e);
		}

        Replication pullReplication = database.createPullReplication(fullSyncDatabaseUri);
        Replication pushReplication = database.createPushReplication(fullSyncDatabaseUri);
        
        // ??? Does surely something but do not know what, it is optional so it is still here ???
        String authenticationCookieValue = c8o.getAuthenticationCookieValue();
        if (authenticationCookieValue != null) {
        	// Create the expiration being tomorrow
        	Calendar calendar = Calendar.getInstance();
        	calendar.add(Calendar.DATE, 1);
        	Date expirationDate = calendar.getTime();
  
        	boolean isSecure = false;
        	boolean httpOnly = false;
        	
        	pullReplication.setCookie(C8oFullSyncDatabase.AUTHENTICATION_COOKIE_NAME, authenticationCookieValue, "/", expirationDate, isSecure, httpOnly);
        	pushReplication.setCookie(C8oFullSyncDatabase.AUTHENTICATION_COOKIE_NAME, authenticationCookieValue, "/", expirationDate, isSecure, httpOnly);
        }
        
        pullFullSyncReplication = new FullSyncReplication(pullReplication);
        pushFullSyncReplication = new FullSyncReplication(pushReplication);
    }
    
    //*** TAG Replication ***//

    /**
     * Start pull and push replications.
     */
    public void startAllReplications(Map<String, Object> parameters, C8oResponseListener c8oResponseListener) {
	    startPullReplication(parameters, c8oResponseListener);
	    startPushReplication(parameters, c8oResponseListener);
    }

    /**
     * Start pull replication.
     */
    public void startPullReplication(Map<String, Object> parameters, C8oResponseListener c8oResponseListener) {
    	startReplication(this.c8o, this.pullFullSyncReplication, c8oResponseListener, parameters);
    }
    
    /**
     * Start push replication.
     * @return 
     */
    public void startPushReplication(Map<String, Object> parameters, C8oResponseListener c8oResponseListener) {
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
    private static void startReplication(C8o c8o, FullSyncReplication c8oReplication, C8oResponseListener c8oResponseListener, Map<String, Object> parameters) {
    	synchronized (c8oReplication.replication) {
    		
    		// Cancel the replication if it is already running
    		if (c8oReplication.replication.isRunning()) {
    			c8oReplication.replication.stop();
    		}
    		
    		// Adds parameter to the replication
    		for (FullSyncEnum.FullSyncReplicateDatabaseParameter fullSyncParameter : FullSyncEnum.FullSyncReplicateDatabaseParameter.values()) {
    			String parameterValue = C8oUtils.getParameterStringValue(parameters, fullSyncParameter.name, false);
    			if (parameterValue != null) {
    				// Cancel the replication
    				if (fullSyncParameter == FullSyncEnum.FullSyncReplicateDatabaseParameter.CANCEL && parameterValue.equals("true")) {
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
    			if (c8oResponseListener instanceof C8oResponseCblListener) {
    				c8oReplication.changeListener = new C8oFullSyncChangeEventListener((C8oResponseCblListener) c8oResponseListener, c8oReplication.replication, parameters);
    			} else if (c8oResponseListener instanceof C8oResponseJsonListener) {
    				c8oReplication.changeListener = new C8oJSONChangeEventListener((C8oResponseJsonListener) c8oResponseListener, c8oReplication.replication, parameters);
    			} else if (c8oResponseListener instanceof C8oResponseXmlListener) {
    				c8oReplication.changeListener = new C8oXMLChangeEventListener((C8oResponseXmlListener) c8oResponseListener, c8oReplication.replication, c8o.getDocumentBuilder(), parameters);
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
    		pullFullSyncReplication.replication.deleteCookie(C8oFullSyncDatabase.AUTHENTICATION_COOKIE_NAME);
    		pullFullSyncReplication.replication = null;
    	}
    	pullFullSyncReplication = null;
    	
    	if (pushFullSyncReplication.replication != null) {
    		pushFullSyncReplication.replication.stop();
    		pushFullSyncReplication.replication.deleteCookie(C8oFullSyncDatabase.AUTHENTICATION_COOKIE_NAME);
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
    		changeListener = null;
    	}
    }
}
