package com.convertigo.clientsdk;

import org.apache.http.NameValuePair;

/**
 * Allow to use any Object type as a NameValuePair value instead of String that will be translated to this Object type.<br/>
 * This feature is mainly useful for fullSync requests.
 */
public class ObjectNameValuePair implements NameValuePair {
	/**
	 * The name.
	 */
	private String name;
	/**
	 * The value.
	 */
	private Object value;
	
	/**
	 * Default Constructor taking a name and a value.
	 * 
	 * @param name
	 * @param value
	 */
	public ObjectNameValuePair(String name, Object value) {
		this.name = name;
		this.value = value;
	}
	
	/**
	 * Returns the name.
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Returns the value as a String.
	 */
	public String getValue() {
		return "" + this.value;
	}
	
	/**
	 * Returns the real value.
	 * @return
	 */
	public Object getObjectValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.name + "=" + this.getValue();
	}

}
