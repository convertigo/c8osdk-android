package com.convertigo.clientsdk;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.listener.C8oResponseListener;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.Replication.ChangeEvent;
import com.couchbase.lite.replicator.Replication.ChangeListener;

import org.apache.http.cookie.Cookie;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


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

	private URL c8oFullSyncDatabaseUrl;
	/**
	 * The fullSync Database instance.
	 */
	private Database database = null;
	/**
	 * Used to make pull replication (uploads changes from the local database to the remote one).
	 */
    private FullSyncReplication pullFullSyncReplication = new FullSyncReplication(true);
    /**
	 * Used to make push replication (downloads changes from the remote database to the local one).
	 */
    private FullSyncReplication pushFullSyncReplication = new FullSyncReplication(false);
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

		try {
			c8oFullSyncDatabaseUrl = new URL(fullSyncDatabases + databaseName + "/");
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidFullSyncDatabaseUrl(fullSyncDatabases + databaseName + "/"), e);
		}

        this.databaseName = (databaseName += localSuffix);

    	try {
			database = manager.getDatabase(databaseName);
		} catch (CouchbaseLiteException e) {
			throw new C8oException(C8oExceptionMessage.unableToGetFullSyncDatabase(databaseName), e);
		}

        /*
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
        */
    }

	private Replication getReplication(FullSyncReplication fsReplication) {
		if (fsReplication.replication != null) {
			fsReplication.replication.stop();
			if (fsReplication.changeListener != null) {
				fsReplication.replication.removeChangeListener(fsReplication.changeListener);
			}
		}
		Replication replication = fsReplication.replication = fsReplication.pull ?
				database.createPullReplication(c8oFullSyncDatabaseUrl) :
				database.createPushReplication(c8oFullSyncDatabaseUrl);

		for (Cookie cookie : c8o.getCookieStore().getCookies()) {
			replication.setCookie(cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getExpiryDate(), cookie.isSecure(), false);
		}

		return replication;
	}

    /**
     * Start pull and push replications.
     */
    public void startAllReplications(Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
	    startPullReplication(parameters, c8oResponseListener);
	    startPushReplication(parameters, c8oResponseListener);
    }

    /**
     * Start pull replication.
     */
    public void startPullReplication(Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
        startReplication(pullFullSyncReplication, parameters, c8oResponseListener);
    }
    
    /**
     * Start push replication.
     * @return 
     */
    public void startPushReplication(Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
		startReplication(pushFullSyncReplication, parameters, c8oResponseListener);
    }

    /**
     * Starts a replication taking into account parameters.<br/>
     * This action does not directly return something but setup a callback raised when the replication raises change events.
     *
     * @param fullSyncReplication
     * @param c8oResponseListener
     * @param parameters
     */
    private void startReplication(FullSyncReplication fullSyncReplication, Map<String, Object> parameters, final C8oResponseListener c8oResponseListener) throws C8oException {
		boolean continuous = false;
		boolean cancel = false;

		if (parameters.containsKey("continuous"))
		{
			continuous = parameters.get("continuous").toString().equalsIgnoreCase("true");
		}

		if (parameters.containsKey("cancel"))
		{
			cancel = parameters.get("cancel").toString().equalsIgnoreCase("true");
		}

		final Replication rep = getReplication(fullSyncReplication);

		if (cancel)
		{
            if (rep != null) {
                rep.stop();
            }
			return;
		}

		final Map<String, Object> param = new HashMap<String, Object>(parameters);
		C8oProgress progress = new C8oProgress();
        final C8oProgress[] _progress = {progress};
		progress.setRaw(rep);
		progress.setPull(rep.isPull());

		final Object mutex = new Object();

		rep.addChangeListener(
				fullSyncReplication.changeListener = new ChangeListener() {

                    @Override
                    public void changed(ChangeEvent changeEvent) {
                        C8oProgress progress = _progress[0];
                        progress.setTotal(changeEvent.getChangeCount());
                        progress.setCurrent(changeEvent.getCompletedChangeCount());
                        progress.setTaskInfo("n/a");//changeEvent.getTransition().toString();
                        progress.setStatus("" + rep.getStatus());
                        progress.setFinished(rep.getStatus() != Replication.ReplicationStatus.REPLICATION_ACTIVE);

                        if (progress.isFinished()) {
                            synchronized (mutex) {
                                mutex.notify();
                            }
                        }

                        if (progress.isChanged()) {
                            _progress[0] = new C8oProgress(progress);

                            if (c8oResponseListener != null && c8oResponseListener instanceof C8oResponseProgressListener)
                            {
                                param.put(C8o.ENGINE_PARAMETER_PROGRESS, progress);
                                ((C8oResponseProgressListener) c8oResponseListener).onProgressResponse(progress, param);
                            }
                        }
                    }
                }
        );

		synchronized (mutex)
		{
			// Finally starts the replication
			rep.start();
            try {
                mutex.wait();
            } catch (InterruptedException e) {
                throw new C8oException("TODO", e);
            }
            rep.stop();
		}

		if (continuous)
		{
            progress = _progress[0];
            final long lastCurrent = progress.getCurrent();
            final short[] stat = {0};
            Replication replication = getReplication(fullSyncReplication);
            progress.setRaw(replication);
            progress.setContinuous(true);
            replication.setContinuous(true);
            replication.addChangeListener(
                    fullSyncReplication.changeListener = new ChangeListener() {

                        @Override
                        public void changed(ChangeEvent changeEvent) {
                            long total = changeEvent.getChangeCount();
                            if (stat[0] == 0) {
                                c8o.log.warn("total >= lastCurrent ? " + total + " >= " + lastCurrent);
                                if (lastCurrent == 0 || total > 0) {
                                    stat[0] = 1;
                                }
                            }

                            if (stat[0] == 1) {
                                c8o.log.warn("total == 0 ? " + total);
                                if (total == 0) {
                                    stat[0] = 2;
                                }
                            }

                            if (stat[0] == 2 && total > 0) {
                                C8oProgress progress = _progress[0];
                                progress.setTotal(total);
                                progress.setCurrent(changeEvent.getCompletedChangeCount());
                                progress.setTaskInfo("n/a");//changeEvent.getTransition().toString();
                                progress.setStatus("" + rep.getStatus());

                                if (progress.isChanged()) {
                                    _progress[0] = new C8oProgress(progress);

                                    if (c8oResponseListener != null && c8oResponseListener instanceof C8oResponseProgressListener) {
                                        param.put(C8o.ENGINE_PARAMETER_PROGRESS, progress);
                                        ((C8oResponseProgressListener) c8oResponseListener).onProgressResponse(progress, param);
                                    }
                                }
                            }
                        }
                    }
            );
            replication.start();
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
		public boolean pull;
    	
    	// Replication history -> ?
		// Replication id version -> ?
		// Session ID -> replication.getSessionID();
		// Source last seq -> replication.getLocalDatabase().getLastSequenceNumber();
    	
    	public FullSyncReplication (boolean pull) {
    		this.pull = pull;
    	}
    }
}
