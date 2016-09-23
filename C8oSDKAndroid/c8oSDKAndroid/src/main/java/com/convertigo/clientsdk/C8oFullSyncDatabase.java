package com.convertigo.clientsdk;

import android.util.Log;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.listener.C8oResponseListener;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Manager;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.Replication.ChangeEvent;
import com.couchbase.lite.replicator.Replication.ChangeListener;

import org.apache.http.cookie.Cookie;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


class C8oFullSyncDatabase {
	
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

            // manager.enableLogging("Sync", Log.DEBUG);
            // manager.enableLogging("RemoteRequest", Log.VERBOSE);
            DatabaseOptions options = new DatabaseOptions();
            options.setCreate(true);
            if (c8o.getFullSyncEncryptionKey() != null) {
                options.setEncryptionKey(c8o.getFullSyncEncryptionKey());
            }
            if (C8o.FS_STORAGE_SQL.equals(c8o.getFullSyncStorageEngine())) {
                options.setStorageType(Manager.SQLITE_STORAGE);
            } else {
                options.setStorageType(Manager.FORESTDB_STORAGE);
            }
            database = manager.openDatabase(databaseName, options);
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

    void delete() {
        if (database != null) {
            try {
                database.delete();
            } catch (CouchbaseLiteException e) {
                c8o.log._info("Failed to close database", e);
            } finally {
                database = null;
            }
        }
    }

    private Replication createReplication(FullSyncReplication fsReplication) {
        Replication replication = fsReplication.replication = fsReplication.pull ?
                database.createPullReplication(c8oFullSyncDatabaseUrl) :
                database.createPushReplication(c8oFullSyncDatabaseUrl);

        for (Cookie cookie : c8o.getCookieStore().getCookies()) {
            replication.setCookie(cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getExpiryDate(), cookie.isSecure(), false);
        }

        return replication;
    }

    private void stopReplication(FullSyncReplication fsReplication) {
        if (fsReplication.replication != null) {
            fsReplication.replication.stop();
            if (fsReplication.changeListener != null) {
                fsReplication.replication.removeChangeListener(fsReplication.changeListener);
                fsReplication.changeListener = null;
            }
            fsReplication.replication = null;
        }
    }

	private Replication getReplication(FullSyncReplication fsReplication) {
		stopReplication(fsReplication);
		Replication replication = createReplication(fsReplication);
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
    private void startReplication(final FullSyncReplication fullSyncReplication, Map<String, Object> parameters, final C8oResponseListener c8oResponseListener) throws C8oException {
		final boolean continuous;
		boolean cancel = false;

		if (parameters.containsKey("continuous")) {
			continuous = parameters.get("continuous").toString().equalsIgnoreCase("true");
		} else {
            continuous = false;
        }

		if (parameters.containsKey("cancel"))
		{
			cancel = parameters.get("cancel").toString().equalsIgnoreCase("true");
		}

		final Replication rep = getReplication(fullSyncReplication);
        C8oProgress progress = new C8oProgress();
        progress.setRaw(rep);
        progress.setPull(rep.isPull());

		if (cancel)
		{
            stopReplication(fullSyncReplication);
            progress.setFinished(true);

            if (c8oResponseListener != null && c8oResponseListener instanceof C8oResponseProgressListener) {
                ((C8oResponseProgressListener) c8oResponseListener).onProgressResponse(progress, null);
            }
			return;
		}

        final Map<String, Object> param = new HashMap<String, Object>(parameters);
        final C8oProgress[] _progress = {progress};

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
                        Log.d("C8O", "rep.getStatus()==>" + rep.getStatus());

                        if (progress.isChanged()) {
                            _progress[0] = new C8oProgress(progress);

                            if (c8oResponseListener != null && c8oResponseListener instanceof C8oResponseProgressListener)
                            {
                                param.put(C8o.ENGINE_PARAMETER_PROGRESS, progress);
                                ((C8oResponseProgressListener) c8oResponseListener).onProgressResponse(progress, param);
                            }
                        }

                        if (progress.isFinished()) {
                            stopReplication(fullSyncReplication);
                            if (continuous) {
                                final long lastCurrent = 0;//progress.getCurrent();
                                final short[] stat = {0};
                                final Replication replication = getReplication(fullSyncReplication);
                                _progress[0].setRaw(replication);
                                _progress[0].setContinuous(true);
                                replication.setContinuous(true);
                                replication.addChangeListener(
                                        fullSyncReplication.changeListener = new ChangeListener() {

                                            @Override
                                            public void changed(ChangeEvent changeEvent) {
                                                C8oProgress progress = _progress[0];
                                                progress.setTotal(replication.getChangesCount());
                                                progress.setCurrent(replication.getCompletedChangesCount());
                                                progress.setTaskInfo("n/a");//changeEvent.getTransition().toString();
                                                progress.setStatus("" + replication.getStatus());

                                                //if (progress.getCurrent() > lastCurrent && progress.isChanged()) {
                                                if (progress.isChanged()) {
                                                    _progress[0] = new C8oProgress(progress);

                                                    if (c8oResponseListener != null && c8oResponseListener instanceof C8oResponseProgressListener) {
                                                        param.put(C8o.ENGINE_PARAMETER_PROGRESS, progress);
                                                        ((C8oResponseProgressListener) c8oResponseListener).onProgressResponse(progress, param);
                                                    }
                                                }
                                            }
                                        }
                                );
                                replication.start();
                            }
                        }
                    }
                }
        );

        rep.start();
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
