package com.convertigo.clientsdk.exception;

import com.couchbase.lite.CouchbaseLiteException;

/**
 * Created by Nicolas on 17/02/2016.
 */
public class C8oCouchbaseLiteException extends C8oException {

    private static final long serialVersionUID = -565811361589019533L;

    public C8oCouchbaseLiteException(String message, CouchbaseLiteException cause) {
        super(message, cause);
    }

    @Override
    public CouchbaseLiteException getCause() {
        return (CouchbaseLiteException) super.getCause();
    }
}
