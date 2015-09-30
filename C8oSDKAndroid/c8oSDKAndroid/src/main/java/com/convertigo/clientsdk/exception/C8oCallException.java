package com.convertigo.clientsdk.exception;

import java.util.List;

import org.apache.http.NameValuePair;

/**
 * Thrown during a c8o call, it contains call parameters.
 */
public class C8oCallException extends Exception {

	private static final long serialVersionUID = -4127896015021351190L;
	
	private List<NameValuePair> parameters;
	
	public C8oCallException(String message, List<NameValuePair> parameters) {
		super(message);
		this.parameters = parameters;
	}
	
	public C8oCallException(String message, Throwable cause, List<NameValuePair> parameters) {
		super(message, cause);
		this.parameters = parameters;
	}
	
	public List<NameValuePair> getParameters() {
		return this.parameters;
	}
	
}
