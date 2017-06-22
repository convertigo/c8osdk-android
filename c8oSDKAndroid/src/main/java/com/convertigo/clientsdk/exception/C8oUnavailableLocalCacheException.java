package com.convertigo.clientsdk.exception;

/**
 * This Exception is not thrown to the user, it is used to know if the requested response from the local cache is available or no.
 */
public class C8oUnavailableLocalCacheException extends Exception {

	private static final long serialVersionUID = 5428876605265700709L;

	public C8oUnavailableLocalCacheException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public C8oUnavailableLocalCacheException(String detailMessage) {
		super(detailMessage);
	}

}
