package com.convertigo.clientsdk;

import android.webkit.URLUtil;

import com.convertigo.clientsdk.exception.C8oException;

import org.apache.http.NameValuePair;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class C8oUtils {

	/**
	 * FullSync parameters prefix.
	 */
	private final static String USE_PARAMETER_IDENTIFIER = "_use_";
	
	//*** TAG Class ***//
	
	/**
	 * Casts an object to a specific class, use this function to disable the warning and easily flag all casts done.
	 * 
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <E> E cast(Object object) {
		return (E) object;
	}
	
	/**
	 * Returns the class name of the object as a String, if the object is null then returns the String "null".
	 * 
	 * @param object
	 * @return
	 */
	public static String getObjectClassName(Object object) {
		String className = "null";
		if (object != null) {
			className = object.getClass().getName();
		}
		return className;
	}
	
	//*** TAG Parameter ***//
	
	private static Entry<String, Object> getParameter(Map<String, Object> parameters, String name, boolean useName) {
		for (Entry<String, Object> parameter : parameters.entrySet())
		{
			String parameterName = parameter.getKey();
			if (name.equals(parameterName) || (useName && name.equals(USE_PARAMETER_IDENTIFIER + parameterName))) {
				return parameter;
			}
		}
		return null;
	}
	
	/**
	 * Searches in the list the parameter with this specific name (or the same name with the prefix '_use_') and returns it.<br/>
	 * Returns null if the parameter is not found.
	 * 
	 * @param parameters
	 * @param name
	 * @return
	 */
	public static String getParameterStringValue(Map<String, Object> parameters, String name, boolean useName) {
		Entry<String, Object> parameter = C8oUtils.getParameter(parameters, name, useName);
		if (parameter != null) {
			return "" + parameter.getValue();
		}
		return null;
	}
	
	public static Object getParameterObjectValue(Map<String, Object> parameters, String name, boolean useName) throws C8oException {
		Entry<String, Object> parameter = getParameter(parameters, name, useName);
		if (parameter != null) {
			return parameter.getValue();
		}
		return null;
	}
	
	//*** TAG Others ***//
	
	/**
	 * Serializes a c8o call request thanks to its parameters and response type.
	 * 
	 * @param parameters
	 * @param responseType
	 * @return
	 * @throws C8oException
	 */
	public static String identifyC8oCallRequest(Map<String, Object> parameters, String responseType) throws C8oException {
		try {
			return responseType + C8oTranslator.c8oCallRequestToJSON(parameters).toString();
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.c8oCallRequestToJson(), e);
		}
	}

	public static boolean isValidUrl(String url) {
		return URLUtil.isValidUrl(url);
	}

	public static String peekParameterStringValue(Map<String, Object> parameters, String name, boolean exceptionIfMissing) {
		String value = getParameterStringValue(parameters, name, false);

		if (value == null) {
			if (exceptionIfMissing)
			{
				throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentMissParameter(name));
			}
		} else {
			parameters.remove(name);
		}
		return value;
	}
}
