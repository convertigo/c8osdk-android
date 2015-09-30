package com.convertigo.clientsdk;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oExceptionMessage;
import com.convertigo.clientsdk.util.C8oUtils;
import com.convertigo.clientsdk.util.StringUtils;
import com.couchbase.lite.Manager;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class C8oTranslator {
	
	private static final String XML_KEY_ITEM = "item";
	private static final String XML_KEY_OBJECT = "object";
	private static final String XML_KEY__ATTACHMENTS = "_attachments";
	private static final String XML_KEY_ATTACHMENT = "attachment";
	private static final String XML_KEY_NAME = "name";
	
	//*** TAG JSON to XML ***//
	
	/**
	 * Translates a JSON value to an XML document and inserts it under an XML element.
	 * 
	 * @param jsonValue - The JSON value to translate.
	 * @param xmlDocument - The XML document in which translated elements will be created.
	 * @param parentElement - The XML element under which translated elements will be added.
	 * @throws C8oException 
	 */
	public static void jsonValueToXml(Object jsonValue, Document xmlDocument, Element parentElement) throws C8oException {
		// Translates the JSON object depending to its type
		// JSON structure case
		if (jsonValue instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject) jsonValue;
			
			// Gets all the elements of the JSON object and sorts them
			String[] keys = new String[jsonObject.length()];
			int index = 0;
			for (Iterator<String> i = C8oUtils.cast(jsonObject.keys()); i.hasNext();) {
				keys[index++] = i.next();
			}
			Arrays.sort(keys);
			
			// Translates each elements of the JSON object
			for (String key: keys) {
				try {
					Object keyValue = jsonObject.get(key);
					jsonKeyToXml(key, keyValue, xmlDocument, parentElement);
				} catch (JSONException e) {
					throw new C8oException(C8oExceptionMessage.getJsonKey(key), e);
				}
			}
		}
		// JSON array case
		else if (jsonValue instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) jsonValue;
			
			// Translates each items of the JSON array
			for (int i = 0; i < jsonArray.length(); i++) {
				// Create the XML element
				Element item = xmlDocument.createElement(XML_KEY_ITEM);
				parentElement.appendChild(item);
				try {
					jsonValueToXml(jsonArray.get(i), xmlDocument, item);
				} catch (JSONException e) {
					throw new C8oException(C8oExceptionMessage.jsonValueToXML(), e);
				}
			}
		}
		// JSON simple type value case
		else if (jsonValue != null && jsonValue != JSONObject.NULL) {
			parentElement.setTextContent(jsonValue.toString());
		}
	}
	
	/**
	 * Translates a JSON key to an XML document and then translates the JSON value related to this key.
	 * 
	 * @param jsonKey - The JSON key to translate.
	 * @param jsonValue - The JSON value related to the JSON key.
	 * @param xmlDocument - The XML document in which translated elements will be created.
	 * @param parentElement - The XML element under which translated elements will be added.
	 * @throws C8oException 
	 */
	public static void jsonKeyToXml(String jsonKey, Object jsonValue, Document xmlDocument, Element parentElement) throws C8oException {
		// Replaces the key if it is not specified
		if (jsonKey == null || "".equals(jsonKey)) {
			jsonKey = XML_KEY_OBJECT;
		}
		
		// If the parent node contains attachments (Specific to Couch)
		// TODO why ???
		if (XML_KEY__ATTACHMENTS.equals(parentElement.getNodeName())) {
			// Creates the attachment element and its child elements containing the attachment name
			Element attachmentElement = xmlDocument.createElement(XML_KEY_ATTACHMENT);
			Element attachmentNameElement = xmlDocument.createElement(XML_KEY_NAME);
			attachmentNameElement.setTextContent(jsonKey);
			attachmentElement.appendChild(attachmentNameElement);
			parentElement.appendChild(attachmentElement);
			
			// Translates the attachment value (it won't override attachment name element because the attachment value is normally a JSON object)s
			try {
				jsonValueToXml(jsonValue, xmlDocument, attachmentElement);
			} catch (C8oException e) {
				throw new C8oException(C8oExceptionMessage.jsonValueToXML(), e);
			}
		} else {
			// Creates the XML child element with its normalized name
			String normalizedKey = StringUtils.normalize(jsonKey);
			Element childElement = xmlDocument.createElement(normalizedKey);
			parentElement.appendChild(childElement);
			
			// Translates the JSON value
			try {
				jsonValueToXml(jsonValue, xmlDocument, childElement);
			} catch (C8oException e) {
				throw new C8oException(C8oExceptionMessage.jsonValueToXML(), e);
			}
		}
	}
	
	//*** TAG InputStream to XML / JSON / String ***//
	
	/**
	 * Converts an input stream to a document object and close the input stream after.<br/>
	 * Can't be executed in main thread.
	 * 
	 * @param inputStream
	 * @return
	 * @throws IllegalStateException
	 * @throws SAXException
	 * @throws IOException
	 * @throws C8oException
	 */
	public static Document inputStreamToXMLAndClose(InputStream inputStream, DocumentBuilder documentBuilder) throws C8oException {
		Document document;
		// Lock on documentBuilder
		synchronized(documentBuilder) {
			// Build an XML Document with the c8o call result
			try {
				document = documentBuilder.parse(inputStream);
			} catch (SAXException e) {
				throw new C8oException(C8oExceptionMessage.parseStreamToXml(), e);
			} catch (IOException e) {
				throw new C8oException(C8oExceptionMessage.parseStreamToXml(), e);
			}
		}
		try {
			inputStream.close();
		} catch (IOException e) {
			throw new C8oException(C8oExceptionMessage.closeInputStream(), e);
		}
		// Return this Document
		return document;
	}
	
	/**
	 * Converts an input stream to a JSON object and close the input stream after.<br/>
	 * Can't be executed in main thread.
	 * 
	 * @param inputStream
	 * @return
	 * @throws C8oException 
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONObject inputStreamToJSONAndClose(InputStream inputStream) throws C8oException {
		JSONObject json = null;
		String responseString;
		try {
			responseString = inputStreamToString(inputStream);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.parseInputStreamToString(), e);
		}
		try {
			inputStream.close();
		} catch (IOException e) {
			throw new C8oException(C8oExceptionMessage.closeInputStream(), e);
		}
		// Create a JSONObject from the c8o result String
		try {
			json = new JSONObject(responseString);
		} catch (JSONException e) {
			throw new C8oException(C8oExceptionMessage.parseStringToJson(), e);
		}
		// Return this JSON
		return json;
	}

	/**
	 * Converts an input stream to a string.
	 * 
	 * @param inputStream
	 * @return
	 * @throws C8oException 
	 * @throws IOException
	 */
	public static String inputStreamToString(InputStream inputStream) throws C8oException {
		StringBuilder stringBuilder = null;
		BufferedReader bufferedReader;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new C8oException(C8oExceptionMessage.inputStreamReaderEncoding(), e);
		}
		stringBuilder = new StringBuilder();
		// Build a String containing the JSON c8o call result
		String line;
		try {
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
			}
		} catch (IOException e) {
			throw new C8oException(C8oExceptionMessage.readLineFromBufferReader(), e);
		}
		return stringBuilder.toString();
	}
	
	//*** String to XML / JSON ***//
	
	public static JSONObject stringToJSON(String jsonString) throws C8oException {
		try {
			return new JSONObject(jsonString);
		} catch (JSONException e) {
			throw new C8oException(C8oExceptionMessage.parseStringToJson(), e);
		}
	}
	
	/**
	 * Deserializes JSON content from given JSON content String.
	 * 
	 * @param value
	 * @return
	 * @throws C8oException 
	 */
	public static Object stringToObjectValue(String value) throws C8oException {
        Object result = null;
        try {
			result = Manager.getObjectMapper().readValue(value, Object.class);
		/*} catch (JsonParseException e) {
			throw new C8oException(C8oExceptionMessage.stringToJson(value), e);
		} catch (JsonMappingException e) {
			throw new C8oException(C8oExceptionMessage.stringToJson(value), e);
		} catch (IOException e) {
			throw new C8oException(C8oExceptionMessage.stringToJson(value), e);
		}*/
        } catch (Exception e) {
        	result = value;
        }
        return result;
	 }
	
	public static Document stringToXML(String xmlString, DocumentBuilder documentBuilder) throws C8oException {
		try {
			return C8oTranslator.inputStreamToXMLAndClose(new ByteArrayInputStream(xmlString.getBytes()), documentBuilder);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.inputStreamToXML(), e);
		}
	}
	
	//*** TAG Others ***//
	
	/**
	 * Converts a document to a string.
	 * 
	 * @param document
	 * @return
	 * @throws C8oException 
	 */
	public static String xmlToString(Document document) throws C8oException {
		Source source = new DOMSource(document);
        StringWriter stringWriter = new StringWriter();
        Result result = new StreamResult(stringWriter);
        Transformer transformer;
        try {
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.transform(source, result);
        } catch (Exception e) {
        	throw new C8oException(C8oExceptionMessage.parseXmlToString(), e);
        }
        return stringWriter.getBuffer().toString();
	}
	
	public static JSONObject c8oCallRequestToJSON(List<NameValuePair> parameters) throws C8oException {
		JSONObject json = new JSONObject();
		try {
			for (NameValuePair parameter : parameters) {
				json.put(parameter.getName(), parameter.getValue());
			}
		} catch (JSONException e) {
			throw new C8oException(C8oExceptionMessage.putJson(), e);
		}
		return json;
	}
	
	/**
	 * Convert bytes array to a String with hexadecimal characters.
	 *
	 * @param bytes
	 * @return
	 */
	public static String byteArrayToHexString(byte[] bytes) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			result.append( Integer.toString( ( bytes[i] & 0xff ) + 0x100, 16).substring( 1 ) );
		}
		return result.toString();
	}
	
}
