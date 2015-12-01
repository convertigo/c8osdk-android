package com.convertigo.clientsdk.exception;

import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;

/**
 * Thrown during a c8o call, it contains call parameters.
 */
public class C8oCallException extends Exception {

	private static final long serialVersionUID = -4127896015021351190L;
	
	private Map<String, Object> parameters;
	
	public C8oCallException(String message, Map<String, Object> parameters) {
		super(message);
		this.parameters = parameters;
	}
	
	public C8oCallException(String message, Throwable cause, Map<String, Object> parameters) {
		super(message, cause);
		this.parameters = parameters;
	}
	
	public Map<String, Object> getParameters() {
		return this.parameters;
	}
	
}
