package com.convertigo.clientsdk.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.http.NameValuePair;

import com.convertigo.clientsdk.C8oTranslator;
import com.convertigo.clientsdk.ObjectNameValuePair;
import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oExceptionMessage;

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
	
	private static NameValuePair getParameter(List<NameValuePair> parameters, String name, boolean useName) {
		Iterator<NameValuePair> parametersIterator = parameters.iterator();
		while (parametersIterator.hasNext()) {
			NameValuePair parameter = parametersIterator.next();
			String parameterName = parameter.getName();
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
	public static String getParameterStringValue(List<NameValuePair> parameters, String name, boolean useName) {
		NameValuePair parameter = C8oUtils.getParameter(parameters, name, useName);
		if (parameter != null) {
			return parameter.getValue();
		}
		return null;
	}
	
	public static Object getParameterObjectValue(List<NameValuePair> parameters, String name, boolean useName) throws C8oException {
		NameValuePair parameter = C8oUtils.getParameter(parameters, name, useName);
		if (parameter != null) {
			try {
				return C8oUtils.getParameterObjectValue(parameter);
			} catch (C8oException e) {
				throw new C8oException(C8oExceptionMessage.getNameValuePairObjectValue(parameter), e);
			}
		}
		return null;
	}
	
	/**
	 * If the specified NameValuePair parameter's type is ObjectNameValuePair, then returns its Object value.<br/>
	 * Otherwise, deserializes the String value as a JSON String and returns it.
	 * 
	 * @param nameValuePair
	 * @return
	 * @throws C8oException
	 */
	public static Object getParameterObjectValue(NameValuePair nameValuePair) throws C8oException {
		if (nameValuePair instanceof ObjectNameValuePair) {
			return ((ObjectNameValuePair) nameValuePair).getObjectValue();
		} else {
			try {
				return C8oTranslator.stringToObjectValue(nameValuePair.getValue());
			} catch (C8oException e) {
				throw new C8oException(C8oExceptionMessage.stringToJson(nameValuePair.getValue()), e);
			}
		}
	}
	
	/**
	 * Removes the first entry found with this name from the list.<br/>
	 * Returns true if an entry is found, returns false otherwise.
	 * 
	 * @param parameters
	 * @param name
	 * @return
	 */
	public static boolean removeParameter(List<NameValuePair> parameters, String name) {
		int i = 0;
		for (NameValuePair parameter : parameters) {
			if (parameter.getName().equals(name)) {
				parameters.remove(i);
				return true;
			}
			i++;
		}
		return false;
	}
	
	public static ArrayList<NameValuePair> cloneList(List<NameValuePair> list) {
		ArrayList<NameValuePair> listClone = new ArrayList<NameValuePair>();
		for (NameValuePair item : list) {
			listClone.add(item);
		}
		return listClone;
	}
	
	/**
	 * Searches in a map an entry by its key and casts it to the specified class.<br/>
	 * If the entry is not found, then an exception can be thrown or a default value cans be returned. 
	 * 
	 * @param map
	 * @param name
	 * @param type
	 * @param exceptionIfNull
	 * @param defaultValue
	 * @return
	 * @throws C8oException 
	 */
	public static <E> E getParameterAndCheckType(LinkedHashMap<String, ?> map, String name, Class<E> type, boolean exceptionIfNull, E defaultValue) throws C8oException {
		Object value = map.get(name);
		if (value != null) {
			if (type.isAssignableFrom(value.getClass())) {
				return C8oUtils.cast(value);
			} else {
				throw new C8oException(C8oExceptionMessage.illegalArgumentInvalidParameterType(name, type.getName(), value.getClass().getName()));
			}
		} else if (exceptionIfNull) {
			throw new C8oException(C8oExceptionMessage.entryNotFound(name));
		}
		return defaultValue;
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
	public static String identifyC8oCallRequest(List<NameValuePair> parameters, String responseType) throws C8oException {
		try {
			return responseType + C8oTranslator.c8oCallRequestToJSON(parameters).toString();
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.c8oCallRequestToJson(), e);
		}
	}
	
}
