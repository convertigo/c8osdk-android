package com.convertigo.clientsdk;

import com.convertigo.clientsdk.exception.C8oCouchbaseLiteException;
import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.listener.C8oResponseJsonListener;
import com.convertigo.clientsdk.listener.C8oResponseListener;
import com.convertigo.clientsdk.listener.C8oResponseXmlListener;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Query;
import com.couchbase.lite.Query.IndexUpdateMode;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.replicator.Replication;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Contains many enumerations relating to the fullSync requests.
 */
class FullSyncEnum {
	
	//*** TAG Requestalbes ***//
	
	/**
	 * FullSync requestables.
	 */
	enum FullSyncRequestable {
		GET("get") {
			@Override
			protected Object handleFullSyncRequest(C8oFullSync c8oFullSync, String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
				String docid = C8oUtils.peekParameterStringValue(parameters, FullSyncGetDocumentParameter.DOCID.name, true);
				return c8oFullSync.handleGetDocumentRequest(databaseName, docid, parameters);
			}
		},
		DELETE("delete") {
			@Override
			protected Object handleFullSyncRequest(C8oFullSync c8oFullSync, String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
				String docid = C8oUtils.peekParameterStringValue(parameters, FullSyncGetDocumentParameter.DOCID.name, true);
				return c8oFullSync.handleDeleteDocumentRequest(databaseName, docid, parameters);
			}
		},
		POST("post") {
			@Override
			protected Object handleFullSyncRequest(C8oFullSync c8oFullSync, String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException	{
				// Gets the policy parameter
				String fullSyncPolicyParameter = C8oUtils.peekParameterStringValue(parameters, C8o.FS_POLICY, false);

				// Finds the policy corresponding to the parameter value if it exists
				FullSyncPolicy fullSyncPolicy = FullSyncPolicy.getFullSyncPolicy(fullSyncPolicyParameter);

				return c8oFullSync.handlePostDocumentRequest(databaseName, fullSyncPolicy, parameters);
			}
		},
		PUT_ATTACHMENT("put_attachment") {
			@Override
			protected Object handleFullSyncRequest(C8oFullSync c8oFullSync, String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException	{
				// Gets the docid parameter
				String docid = C8oUtils.getParameterStringValue(parameters, FullSyncAttachmentParameter.DOCID.name, false);

				// Gets the attachment name parameter
				String name = C8oUtils.getParameterStringValue(parameters, FullSyncAttachmentParameter.NAME.name, false);

				// Gets the attachment content_type parameter
				String contentType = C8oUtils.getParameterStringValue(parameters, FullSyncAttachmentParameter.CONTENT_TYPE.name, false);

				// Gets the attachment content parameter
				InputStream content = (InputStream)C8oUtils.getParameterObjectValue(parameters, FullSyncAttachmentParameter.CONTENT.name, false);

				return c8oFullSync.handlePutAttachmentRequest(databaseName, docid, name, contentType, content);
			}
		},
		DELETE_ATTACHMENT("delete_attachment") {
			@Override
			protected Object handleFullSyncRequest(C8oFullSync c8oFullSync, String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException	{
				// Gets the docid parameter
				String docid = C8oUtils.getParameterStringValue(parameters, FullSyncAttachmentParameter.DOCID.name, false);

				// Gets the attachment name parameter
				String name = C8oUtils.getParameterStringValue(parameters, FullSyncAttachmentParameter.NAME.name, false);

				return c8oFullSync.handleDeleteAttachmentRequest(databaseName, docid, name);
			}
		},
		ALL("all") {
			@Override
			protected Object handleFullSyncRequest(C8oFullSync c8oFullSync, String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
				return c8oFullSync.handleAllDocumentsRequest(databaseName, parameters);
			}
		},
		VIEW("view") {
			@Override
			protected Object handleFullSyncRequest(C8oFullSync c8oFullSync, String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
				// Gets the design doc parameter value
				String ddoc = C8oUtils.peekParameterStringValue(parameters, FullSyncGetViewParameter.DDOC.name, false);
				// Gets the view name parameter value
				String view = C8oUtils.peekParameterStringValue(parameters, FullSyncGetViewParameter.VIEW.name, false);

				return c8oFullSync.handleGetViewRequest(databaseName, ddoc, view, parameters);
			}
		},
		SYNC("sync") {
			@Override
			Object handleFullSyncRequest(final C8oFullSync c8oFullSync, String databaseName, Map<String, Object> parameters, final C8oResponseListener c8oResponseListener) throws C8oException {
                final boolean[] mutex = {false};
                final boolean[] pullFinish = {false};
                final boolean[] pushFinish = {false};
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
                c8oFullSync.c8o.log._debug("handleFullSyncRequest enter: " + databaseName);
				synchronized (mutex)
                {
                    c8oFullSync.handleSyncRequest(databaseName, parameters, new C8oResponseProgressListener() {

                        @Override
                        public void onProgressResponse(C8oProgress progress, Map<String, Object> parameters) {
							if (!mutex[0]) {
								if (!pullFinish[0] && progress.isPull() && progress.isFinished()) {
									pullFinish[0] = true;
                                    c8oFullSync.c8o.log._debug("handleFullSyncRequest pullFinish = true: " + progress);
								}

								if (!pushFinish[0] && progress.isPush() && progress.isFinished()) {
									pushFinish[0] = true;
                                    c8oFullSync.c8o.log._debug("handleFullSyncRequest pushFinish = true: " + progress);
								}
							}

                            if (c8oResponseListener instanceof C8oResponseJsonListener) {
                                c8oFullSync.c8o.log._trace("handleFullSyncRequest onJsonResponse: " + progress);
                                ((C8oResponseJsonListener) c8oResponseListener).onJsonResponse(null, parameters);
                            } else if (c8oResponseListener instanceof C8oResponseXmlListener) {
                                ((C8oResponseXmlListener) c8oResponseListener).onXmlResponse(null, parameters);
                            }

                            if (!mutex[0] && pullFinish[0] && pushFinish[0]) {
                                synchronized (mutex) {
                                    mutex[0] = true;
                                    c8oFullSync.c8o.log._debug("handleFullSyncRequest notify: " + progress);
                                    mutex.notify();
                                }
                            }
                        }
                    });

                    JSONObject response = new JSONObject();
                    try {
                        mutex.wait();
                        c8oFullSync.c8o.log._debug("handleFullSyncRequest after wait");
                        response.put("ok", true);
                    } catch (Exception e) {
                        throw new C8oException("TODO", e);
                    }
                    return response;
                }
			}
		},
		REPLICATE_PULL("replicate_pull") {
			@Override
			protected Object handleFullSyncRequest(C8oFullSync c8oFullSync, String databaseName, Map<String, Object> parameters, final C8oResponseListener c8oResponseListener) throws C8oException {
				final Object mutex = new Object();
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (mutex)
                {
                    c8oFullSync.handleReplicatePullRequest(databaseName, parameters, new C8oResponseProgressListener() {

                        @Override
                        public void onProgressResponse(C8oProgress progress, Map<String, Object> parameters) {
                            if (progress.isFinished()) {
                                synchronized (mutex) {
                                    mutex.notify();
                                }
                            }

                            if (c8oResponseListener instanceof C8oResponseJsonListener) {
                                ((C8oResponseJsonListener) c8oResponseListener).onJsonResponse(null, parameters);
                            } else if (c8oResponseListener instanceof C8oResponseXmlListener) {
                                ((C8oResponseXmlListener) c8oResponseListener).onXmlResponse(null, parameters);
                            }
                        }
                    });

                    JSONObject response = new JSONObject();
                    try {
                        mutex.wait();
                        response.put("ok", true);
                    } catch (Exception e) {
						throw new C8oException("TODO", e);
                    }
                    return response;
                }
			}
		},
		REPLICATE_PUSH("replicate_push") {
			@Override
			protected Object handleFullSyncRequest(C8oFullSync c8oFullSync, String databaseName, Map<String, Object> parameters, final C8oResponseListener c8oResponseListener) throws C8oException {
                final Object mutex = new Object();
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (mutex)
                {
                    c8oFullSync.handleReplicatePushRequest(databaseName, parameters, new C8oResponseProgressListener() {

                        @Override
                        public void onProgressResponse(C8oProgress progress, Map<String, Object> parameters) {
                            if (progress.isFinished()) {
                                synchronized (mutex) {
                                    mutex.notify();
                                }
                            }

                            if (c8oResponseListener instanceof C8oResponseJsonListener) {
                                ((C8oResponseJsonListener) c8oResponseListener).onJsonResponse(null, parameters);
                            } else if (c8oResponseListener instanceof C8oResponseXmlListener) {
                                ((C8oResponseXmlListener) c8oResponseListener).onXmlResponse(null, parameters);
                            }
                        }
                    });

                    JSONObject response = new JSONObject();
                    try {
                        mutex.wait();
                        response.put("ok", true);
                    } catch (Exception e) {
						throw new C8oException("TODO", e);
                    }
                    return response;
                }
			}
		},
		RESET("reset") {
			@Override
			protected Object handleFullSyncRequest(C8oFullSync c8oFullSync, String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
				return c8oFullSync.handleResetDatabaseRequest(databaseName);
			}
		},
		CREATE("create") {
			@Override
			protected Object handleFullSyncRequest(C8oFullSync c8oFullSync, String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
				return c8oFullSync.handleCreateDatabaseRequest(databaseName);
			}
		},
		DESTROY("destroy") {
			@Override
			protected Object handleFullSyncRequest(C8oFullSync c8oFullSync, String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException {
				return c8oFullSync.handleDestroyDatabaseRequest(databaseName);
			}
		};
		
		public String value;
		
		FullSyncRequestable(String value) {
			this.value = value;
		}
		
		abstract Object handleFullSyncRequest(C8oFullSync c8oFullSync, String databaseName, Map<String, Object> parameters, C8oResponseListener c8oResponseListener) throws C8oException;
	
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
	enum FullSyncRequestParameter {
		DESCENDING("descending", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Boolean) {
					query.setDescending((Boolean) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, Boolean.class.getName(), parameter.getClass().getName()));
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
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, String.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		GROUP_LEVEL("group_level", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Integer) {
					query.setGroupLevel((Integer) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, Integer.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		INCLUDE_DELETED("include_deleted", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Boolean) {
					query.setIncludeDeleted((Boolean) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, Boolean.class.getName(), parameter.getClass().getName()));
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
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, String.class.getName(), parameter.getClass().getName()));
				}
			}
		},
        KEY("key", true) {
            @SuppressWarnings("unchecked")
            @Override
            void addToQuery(Query query, Object parameter) {
                query.setKeys(Arrays.asList(parameter));
            }
        },
		KEYS("keys", true) {
			@SuppressWarnings("unchecked")
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof List<?>) {
					query.setKeys((List<Object>) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, List.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		LIMIT("limit", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Integer) {
					query.setLimit((Integer) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, Integer.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		INCLUDE_DOCS("include_docs", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Boolean) {
					query.setPrefetch((Boolean) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, Boolean.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		REDUCE("reduce", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Boolean) {
					query.setMapOnly(! (Boolean) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, Boolean.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		GROUP("group", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Boolean) {
					query.setGroupLevel((Boolean) parameter ? 99 : 0);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, Boolean.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		SKIP("skip", true) {
			@Override
			void addToQuery(Query query, Object parameter) {
				if (parameter instanceof Integer) {
					query.setSkip((Integer) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, Integer.class.getName(), parameter.getClass().getName()));
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
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, String.class.getName(), parameter.getClass().getName()));
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
	enum FullSyncGetViewParameter {
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
	enum FullSyncGetDocumentParameter {
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
	enum FullSyncDeleteDocumentParameter {
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

	enum FullSyncAttachmentParameter {
		DOCID("docid"),
		NAME("name"),
		CONTENT_TYPE("content_type"),
		CONTENT("content");

		/**
		 * The name of the parameter i.e. the key to send.
		 */
		public final String name;

		FullSyncAttachmentParameter(String name) {
			this.name = name;
		}
	}
	
	/**
	 * Specific parameters for the fullSync's replicateDatabase request (push or pull).
	 */
	enum FullSyncReplicateDatabaseParameter {
		CANCEL("cancel") {
			@Override
			void setReplication(Replication replication, Object parameter) {
			}
		},
		LIVE("live") {
			@Override
			void setReplication(Replication replication, Object parameter) {
				if (parameter instanceof Boolean) {
					replication.setContinuous((Boolean) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, Boolean.class.getName(), parameter.getClass().getName()));
				}
			}
		},
		DOCIDS("docids") {
			@Override
			void setReplication(Replication replication, Object parameter) {
				if (parameter instanceof List) {
                    //noinspection unchecked
					replication.setDocIds((List<String>) parameter);
				} else {
					throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, List.class.getName(), parameter.getClass().getName()));
				}
			}
		};
		
		/**
		 * The name of the parameter i.e. the key to send.
		 */
		public final String name;
		
		FullSyncReplicateDatabaseParameter(String name) {
			this.name = name;
		}
		
		abstract void setReplication(Replication replication, Object parameterValue);
	}
	
	//*** TAG Policy ***//
	
	/**
	 * The policies of the fullSync's postDocument request. 
	 */
	enum FullSyncPolicy {
		NONE(C8o.FS_POLICY_NONE) {
			@Override
			Document postDocument(Database database, Map<String, Object> newProperties) throws C8oException {
				Document createdDocument;
				try {
					String documentId = C8oUtils.getParameterStringValue(newProperties, C8oFullSync.FULL_SYNC__ID, false);

					// Removes special properties
					newProperties.remove(C8oFullSync.FULL_SYNC__ID);

					// Creates a new document or get an existing one (if the ID is specified)
                    createdDocument = (documentId == null) ? database.createDocument() : database.getDocument(documentId);

					createdDocument.putProperties(newProperties);
				} catch (CouchbaseLiteException e) {
					throw new C8oCouchbaseLiteException(C8oExceptionMessage.fullSyncPutProperties(newProperties), e);
				}
				return createdDocument;
			}
		},
		CREATE(C8o.FS_POLICY_CREATE) {
			@Override
			Document postDocument(Database database, Map<String, Object> newProperties) throws C8oException {
                Document createdDocument;
                try {
                    // Removes special properties in order to create a new document
                    newProperties.remove(C8oFullSync.FULL_SYNC__ID);
                    newProperties.remove(C8oFullSync.FULL_SYNC__REV);
                    createdDocument = database.createDocument();
                    createdDocument.putProperties(newProperties);
				} catch (CouchbaseLiteException e) {
					throw new C8oCouchbaseLiteException(C8oExceptionMessage.fullSyncPutProperties(newProperties), e);
				}
				return createdDocument;
			}
		},
		OVERRIDE(C8o.FS_POLICY_OVERRIDE) {
			@Override
			Document postDocument(Database database, Map<String, Object> newProperties) throws C8oException {
                Document createdDocument;
                try {
                    // Gets the document ID
                    String documentId = C8oUtils.getParameterStringValue(newProperties, C8oFullSync.FULL_SYNC__ID, false);

                    // Removes special properties
                    newProperties.remove(C8oFullSync.FULL_SYNC__ID);
                    newProperties.remove(C8oFullSync.FULL_SYNC__REV);

                    // Creates a new document or get an existing one (if the ID is specified)
                    if (documentId == null) {
                        createdDocument = database.createDocument();
                    } else {
                        createdDocument = database.getDocument(documentId);
                        // Must add the current revision to the properties
                        SavedRevision currentRevision = createdDocument.getCurrentRevision();
                        if (currentRevision != null) {
                            newProperties.put(C8oFullSync.FULL_SYNC__REV, currentRevision.getId());
                        }
                    }

					createdDocument.putProperties(newProperties);
				} catch (CouchbaseLiteException e) {
					throw new C8oCouchbaseLiteException(C8oExceptionMessage.fullSyncPutProperties(newProperties), e);
				}
				
				return createdDocument;
			}
		},
		MERGE(C8o.FS_POLICY_MERGE) {
			@Override
			Document postDocument(Database database, Map<String, Object> newProperties) throws C8oException {
                Document createdDocument;
                try {
                    // Gets the document ID
                    String documentId = C8oUtils.getParameterStringValue(newProperties, C8oFullSync.FULL_SYNC__ID, false);

                    // Removes special properties
                    newProperties.remove(C8oFullSync.FULL_SYNC__ID);
                    newProperties.remove(C8oFullSync.FULL_SYNC__REV);

                    // Creates a new document or get an existing one (if the ID is specified)
                    if (documentId == null) {
                        createdDocument = database.createDocument();
                    } else {
                        createdDocument = database.getDocument(documentId);
                    }

                    // Merges old properties with the new ones
                    Map<String, Object> oldProperties = createdDocument.getProperties();
                    if (oldProperties != null) {
                        C8oFullSyncCbl.mergeProperties(newProperties, oldProperties);
                    }

					createdDocument.putProperties(newProperties);
				} catch (CouchbaseLiteException e) {
					throw new C8oCouchbaseLiteException(C8oExceptionMessage.fullSyncPutProperties(newProperties), e);
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

		public static FullSyncPolicy getFullSyncPolicy(String name) {
			try {
				return valueOf(name.toUpperCase());
			} catch (Exception e) {
				return NONE;
			}
		}
		
		abstract Document postDocument(Database database, Map<String, Object> newProperties) throws C8oException;
	}
}
