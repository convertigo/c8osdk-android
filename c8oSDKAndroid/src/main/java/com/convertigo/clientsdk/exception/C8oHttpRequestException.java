package com.convertigo.clientsdk.exception;

/**
 * Thrown during an HTTP request.
 */
public class C8oHttpRequestException extends C8oException {
	
	private static final long serialVersionUID = -2154357228873794455L;

	public C8oHttpRequestException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
