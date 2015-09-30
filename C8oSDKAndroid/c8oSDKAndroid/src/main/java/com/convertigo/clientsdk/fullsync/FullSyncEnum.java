package com.convertigo.clientsdk.fullsync;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oExceptionMessage;
import com.convertigo.clientsdk.exception.C8oRessourceNotFoundException;
import com.convertigo.clientsdk.listener.C8oResponseListener;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.Query;
import com.couchbase.lite.Query.IndexUpdateMode;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.replicator.Replication;

/**
 * Contains many enumerations relating to the fullSync requests.
 */
public class FullSyncEnum {
	
	//*** TAG Requestalbes ***//
	
	/**
	 * FullSync requestables.
	 */
	public enum FullSyncRequestable {
		GET("get") {
			@Override
			protected Object handleFullSyncRequest(FullSyncInterface fullSyncinterface, FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) {
				return fullSyncinterface.handleGetDocumentRequest(fullsyncDatabase, parameters);
			}
		},
		DELETE("delete") {
			@Override
			protected Object handleFullSyncRequest(FullSyncInterface fullSyncinterface, FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) throws C8oException, C8oRessourceNotFoundException	{
				return fullSyncinterface.handleDeleteDocumentRequest(fullsyncDatabase, parameters);
			}
		},
		POST("post") {
			@Override
			protected Object handleFullSyncRequest(FullSyncInterface fullSyncinterface, FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) throws C8oException	{
				return fullSyncinterface.handlePostDocumentRequest(fullsyncDatabase, parameters);
			}
		},
		ALL("all") {
			@Override
			protected Object handleFullSyncRequest(FullSyncInterface fullSyncinterface, FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
				return fullSyncinterface.handleAllDocumentsRequest(fullsyncDatabase, parameters);
			}
		},
		VIEW("view") {
			@Override
			protected Object handleFullSyncRequest(FullSyncInterface fullSyncinterface, FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) throws C8oException, C8oRessourceNotFoundException {
				return fullSyncinterface.handleGetViewRequest(fullsyncDatabase, parameters);
			}
		},
		SYNC("sync") {
			@Override
			Object handleFullSyncRequest(FullSyncInterface fullSyncinterface, FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) {
				return fullSyncinterface.handleSyncRequest(fullsyncDatabase, parameters, c8oResponseListener);
			}
		},
		REPLICATE_PULL("replicate_pull") {
			@Override
			protected Object handleFullSyncRequest(FullSyncInterface fullSyncinterface, FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) {
				return fullSyncinterface.handleReplicatePullRequest(fullsyncDatabase, parameters, c8oResponseListener);
			}
		},
		REPLICATE_PUSH("replicate_push") {
			@Override
			protected Object handleFullSyncRequest(FullSyncInterface fullSyncinterface, FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) {
				return fullSyncinterface.handleReplicatePushRequest(fullsyncDatabase, parameters, c8oResponseListener);
			}
		},
		RESET("reset") {
			@Override
			protected Object handleFullSyncRequest(FullSyncInterface fullSyncinterface, FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
				return fullSyncinterface.handleResetDatabaseRequest(fullsyncDatabase, parameters);
			}
		};
		
		public String value;
		
		FullSyncRequestable(String value) {
			this.value = value;
		}
		
		abstract Object handleFullSyncRequest(FullSyncInterface fullSyncinterface, FullSyncDatabase fullsyncDatabase, List<NameValuePair> parameters, C8oResponseListener c8oResponseListener) throws C8oException, C8oRessourceNotFoundException;
	
		static FullSyncRequestable getFullSyncRequestable(String value) {
			FullSyncRequestable[] fullSyncRequestableValues = FullSyncRequestable.values();
			for(FullSyncRequestable fullSyncRequestable : fullSyncRequestableValues) {
				if (fullSyncRequestable.value.equals(value)) {
					return fullSyncRequestable;
				}
			}
			return null;
			// throw new IllegalArgumentException(C8oExceptionMessage.unhandledFullSyncRequestable(value));
		}
		
	}
	
	//*** TAG Common parameters ***//
	
	/**
	 * Parameters common to some fullSync's requests.
	 */
	public enum FullSyncRequestParameter {
		DESCENDING("descending", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Boolean) {
					query.setDescending((Boolean) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(this.name, Boolean.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		ENDKEY("endkey", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				query.setEndKey(parameter);
			}
		},
		ENDKEY_DOCID("endkey_docid") {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof String) {
					query.setEndKeyDocId((String) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(this.name, String.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		GROUP_LEVEL("group_level", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Integer) {
					query.setGroupLevel((Integer) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(this.name, Integer.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		INCLUDE_DELETED("include_deleted", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Boolean) {
					query.setIncludeDeleted((Boolean) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(this.name, Boolean.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		INDEX_UPDATE_MODE("index_update_mode") {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof String) {
					IndexUpdateMode[] indexUpdateModeValues = IndexUpdateMode.values();
					for (IndexUpdateMode indexUpdateMode : indexUpdateModeValues) {
						if (((String) parameter).equalsIgnoreCase(indexUpdateMode.toString())) {
							query.setIndexUpdateMode(indexUpdateMode);
							return;
						}
					}
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(this.name, String.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		KEYS("keys", true) {
			@SuppressWarnings("unchecked")
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof List<?>) {
					query.setKeys((List<Object>) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(this.name, List.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		LIMIT("limit", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Integer) {
					query.setLimit((Integer) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(this.name, Integer.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		MAP_ONLY("map_only", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Boolean) {
					query.setMapOnly((Boolean) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(this.name, Boolean.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		PREFETCH("prefetch", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Boolean) {
					query.setPrefetch((Boolean) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(this.name, Boolean.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		SKIP("skip", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Integer) {
					query.setSkip((Integer) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(this.name, Integer.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		STARTKEY("startkey", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				query.setStartKey(parameter);
			}
		},
		STARTKEY_DOCID("startkey_docid") {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof String) {
					query.setStartKeyDocId((String) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(this.name, String.class.getName(), parameter.getClass().getName()));
				}
			}
		};
				
		/**
		 * The name of the parameter i.e. the key to send.
		 */
		public final String name;
		/**
		 * Indicates if the value of this parameter have to be written like a JSON value, it could be a string, an integer, a boolean, a JSON object, or a JSON array.
		 */
		public final boolean isJson;
		
		FullSyncRequestParameter(String name, boolean isJson) {
			this.name = name;
			this.isJson = isJson;
		}
		
		FullSyncRequestParameter(String value) {
			this(value, false);
		}
		
		abstract void addToQuery(Query query, Object parameter);
	}
	
	//*** TAG Specific parameters ***//
	
	/**
	 * Specific parameters for the fullSync's getView request.
	 */
	public enum FullSyncGetViewParameter {
		VIEW("view"),
		DDOC("ddoc");
		
		/**
		 * The name of the parameter i.e. the key to send.
		 */
		public final String name;
		
		FullSyncGetViewParameter(String name) {
			this.name = name;
		}
	}
	
	/**
	 * Specific parameters for the fullSync's getDocument request.
	 */
	public enum FullSyncGetDocumentParameter {
		DOCID("docid");
		
		/**
		 * The name of the parameter i.e. the key to send.
		 */
		public final String name;
		
		FullSyncGetDocumentParameter(String name) {
			this.name = name;
		}
	}
	
	/**
	 * Specific parameters for the fullSync's deleteDocument request.
	 */
	public enum FullSyncDeleteDocumentParameter {
		DOCID("docid"),
		REV("rev");
		
		/**
		 * The name of the parameter i.e. the key to send.
		 */
		public final String name;
		
		FullSyncDeleteDocumentParameter(String name) {
			this.name = name;
		}
	}
	
	/**
	 * Specific parameters for the fullSync's postDocument request.
	 */
	public enum FullSyncPostDocumentParameter {
		POLICY("_use_policy"),
		SUBKEY_SEPARATOR("_use_subkey_separator");
		
		/**
		 * The name of the parameter i.e. the key to send.
		 */
		public final String name;
		
		FullSyncPostDocumentParameter(String name) {
			this.name = name;
		}
	}
	
	/**
	 * Specific parameters for the fullSync's replicateDatabase request (push or pull).
	 */
	public enum FullSyncReplicateDatabaseParameter {
		CANCEL("cancel") {
			@Override
			void setReplication(Replication replication, String parameterValue) {
			}
		},
		LIVE("live") {
			@Override
			void setReplication(Replication replication, String parameterValue) {
				boolean live = parameterValue.equals("true");
				replication.setContinuous(live);
			}
		};
		
		/**
		 * The name of the parameter i.e. the key to send.
		 */
		public final String name;
		
		FullSyncReplicateDatabaseParameter(String name) {
			this.name = name;
		}
		
		abstract void setReplication(Replication replication, String parameterValue);
	}
	
	//*** TAG Policy ***//
	
	/**
	 * The policies of the fullSync's postDocument request. 
	 */
	public enum FullSyncPolicy {
		NONE("none") {
			@Override
			Document postDocument(FullSyncDatabase fullsyncDatabase, LinkedHashMap<String, Object> newProperties) throws C8oException {
				Document createdDocument = fullsyncDatabase.database.createDocument();
				try {
					createdDocument.putProperties(newProperties);
				} catch (CouchbaseLiteException e) {
					throw new C8oException(C8oExceptionMessage.fullSyncPutProperties(newProperties), e);
				}
				return createdDocument;
			}
		},
		CREATE("create") {
			@Override
			Document postDocument(FullSyncDatabase fullsyncDatabase, LinkedHashMap<String, Object> newProperties) throws C8oException {
				// Removes special properties in order to create a new document
				newProperties.remove(FullSyncInterface.FULL_SYNC__ID);
				newProperties.remove(FullSyncInterface.FULL_SYNC__REV);
				Document createdDocument = fullsyncDatabase.database.createDocument();
				try {
					createdDocument.putProperties(newProperties);
				} catch (CouchbaseLiteException e) {
					throw new C8oException(C8oExceptionMessage.fullSyncPutProperties(newProperties), e);
				}
				return createdDocument;
			}
		},
		OVERRIDE("override") {
			@Override
			Document postDocument(FullSyncDatabase fullsyncDatabase, LinkedHashMap<String, Object> newProperties) throws C8oException {
				// Gets the document ID
				String documentId = null;
				Object documentIdObj = newProperties.get(FullSyncInterface.FULL_SYNC__ID);
				if (documentIdObj != null) {
					if (documentIdObj instanceof String) {
						documentId = (String) documentIdObj;
					} else {
						throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(FullSyncInterface.FULL_SYNC__ID, String.class.getName(), documentIdObj.getClass().getName()));
					}
				}
				
				// Removes special properties
				newProperties.remove(FullSyncInterface.FULL_SYNC__ID);
				newProperties.remove(FullSyncInterface.FULL_SYNC__REV);
				
				// Creates a new document or get an existing one (if the ID is specified)
				Document createdDocument;
				if (documentId == null) {
					createdDocument = fullsyncDatabase.database.createDocument();
				} else {
					createdDocument = fullsyncDatabase.database.getDocument(documentId);
					// Must add the current revision to the properties
					SavedRevision currentRevision = createdDocument.getCurrentRevision();
					if (currentRevision != null) {
						newProperties.put(FullSyncInterface.FULL_SYNC__REV, currentRevision.getId());
					}
				}
				
				try {
					createdDocument.putProperties(newProperties);
				} catch (CouchbaseLiteException e) {
					throw new C8oException(C8oExceptionMessage.fullSyncPutProperties(newProperties), e);
				}
				
				return createdDocument;
			}
		},
		MERGE("merge") {
			@Override
			Document postDocument(FullSyncDatabase fullsyncDatabase, LinkedHashMap<String, Object> newProperties) throws C8oException {
				// Gets the document ID
				String documentId = null;
				Object documentIdObj = newProperties.get(FullSyncInterface.FULL_SYNC__ID);
				if (documentIdObj != null) {
					if (documentIdObj instanceof String) {
						documentId = (String) documentIdObj;
					} else {
						throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(FullSyncInterface.FULL_SYNC__ID, String.class.getName(), documentIdObj.getClass().getName()));
					}
				}
				
				// Removes special properties
				newProperties.remove(FullSyncInterface.FULL_SYNC__ID);
				newProperties.remove(FullSyncInterface.FULL_SYNC__REV);
				
				// Creates a new document or get an existing one (if the ID is specified)
				Document createdDocument;
				if (documentId == null) {
					createdDocument = fullsyncDatabase.database.createDocument();
					documentId = createdDocument.getId();
				} else {
					createdDocument = fullsyncDatabase.database.getDocument(documentId);
				}
				
				// Merges old properties with the new ones
				Map<String, Object> oldProperties = createdDocument.getProperties();
				if (oldProperties != null) {
					FullSyncInterface.mergeProperties(newProperties, oldProperties);
				}
				
				try {
					createdDocument.putProperties(newProperties);
				} catch (CouchbaseLiteException e) {
					throw new C8oException(C8oExceptionMessage.fullSyncPutProperties(newProperties), e);
				}
				
				return createdDocument;
			}
		};
		
		/**
		 * The value to send.
		 */
		public final String value;
		
		FullSyncPolicy(String value) {
			this.value = value;
		}
		
		abstract Document postDocument(FullSyncDatabase fullsyncDatabase, LinkedHashMap<String, Object> newProperties) throws C8oException;
	}
}
