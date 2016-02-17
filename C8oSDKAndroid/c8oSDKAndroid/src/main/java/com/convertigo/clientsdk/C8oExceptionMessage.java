package com.convertigo.clientsdk;

import com.convertigo.clientsdk.listener.C8oResponseListener;

import org.apache.http.NameValuePair;

import java.util.Map;

/**
 * Contains static functions returning the exception messages 
 * in order to make simpler their modification and avoid duplication.
 */
class C8oExceptionMessage {
	
	//*** TAG Illegal argument ***//
	
	public static String illegalArgumentInvalidFullSyncDatabaseUrl(String fullSyncDatabaseUrlStr) {
		return "The fullSync database url '" + fullSyncDatabaseUrlStr + "' is not a valid url";
	}
	
	public static String illegalArgumentMissParameter(String parameterName) {
		return "The parameter '" + parameterName + "' is missing";
	}
	
	private static String illegalArgumentInvalidParameterValue(String parameterName, String parameterValue) {
		return "'" + parameterValue + "' is not a valid value for the parameter '" + parameterName + "'";
	}
	
	public static String illegalArgumentInvalidParameterProjectRequestableFullSync(String projectParameter) {
		return C8oExceptionMessage.illegalArgumentInvalidParameterValue(C8o.ENGINE_PARAMETER_PROJECT, projectParameter) + 
		", to run a fullSync request this parameter must start with '" + C8oFullSync.FULL_SYNC_PROJECT + "'";
	}
	
	public static String illegalArgumentInvalidURL(String urlStr) {
		return "'" + urlStr + "' is not a valid URL";
	}
	
	public static String illegalArgumentInvalidEndpoint(String endpoint) {
		return "'" + endpoint + "' is not a valid Convertigo endpoint";
	}
	
	public static String illegalArgumentInvalidRequestalbe(String requestable) {
		return "'" + requestable + "' is not a valid requestable.";
	}
	
	public static String illegalArgumentInvalidParameterType(String parameterName, String wantedParameterType, String actualParameterType) {
		return "The parameter '" + parameterName + "' must be of type '" + wantedParameterType + "' and not '" + actualParameterType + "'";
	}
	
	public static String illegalArgumentIncompatibleListener(String listenerType, String responseType) {
		return "The listener type '" + listenerType + "' is incompatible with the response type '" + responseType + "'";
	}
	
	public static String illegalArgumentNullParameter(String parameterName) {
		return parameterName + " must be not null";
	}
	
	public static String invalidParamterValue(String parameterName, String details) {
		String errorMessage = "The parameter '" + parameterName + "' is invalid";
        if (details != null && details.isEmpty()) 
        {
            errorMessage += ", " + details;
        }
        return errorMessage;
	}
	
	//*** TAG Initialization ***//
	
	// TODO
	public static String initError() {
		return "Unable to initialize ";
	}
	
	public static String initRsaPublicKey() {
		return "Unable to initialize the RSA public key";
	}
	
	public static String initCouchManager() {
		return "Unable to initialize the fullSync databases manager";
	}
	
	public static String initSslSocketFactory() {
		return "Unable to initialize the ssl socket factory";
	}
	
	public static String initDocumentBuilder() {
		return "Unable to initialize the XML document builder";
	}
	
	//*** TAG Parse ***//
	
	public static String parseStreamToJson() {
		return "Unable to parse the input stream to a json document";
	}
	
	public static String parseStreamToXml() {
		return "Unable to parse the input stream to an xml document";
	}
	
	public static String parseInputStreamToString() {
		return "Unable to parse the input stream to a string";
	}
	
	public static String parseXmlToString() {
		return "Unable to parse the xml document to a string";
	}
	
	public static String parseRsaPublicKey() {
		return "Unable to parse the RSA public key";
	}
	
	public static String parseQueryEnumeratorToJson() {
		return "Unable to parse the query to a json document";
	}
	
	public static String parseLogsToJson() {
		return "Unable to parse logs to a json document";
	}
	
	public static String parseLogsStreamToJson() {
		return "Unable to parse stream containing logs response to a json document";
	}
	
	public static String parseC8oReplicationResultToJson() {
		return "Unable to parse the replication result to a json document";
	}
	
	public static String parseFullSyncDefaultResponseToJson() {
		return "Unable to parse the default fullSync result to a json document";
	}
	
	public static String parseFullSyncPostDocumentResponseToJson() {
		return "Unable to parse the post document fullSync result to a json document";
	}
	
	public static String parseStringToJson() {
		return "Unable to parse the string to a JSON document";
	}
	
	//*** TAG HTTP ***//
	
	public static String retrieveRsaPublicKey() {
		return "Error during http request to get the RSA public key";
	}
	
	public static String httpLogs() {
		return "Error during http request to send logs to the Convertigo server";
	}
	
	//*** TAG Couch ***//
	
	public static String couchRequestGetView() {
		return "Unable to run the view query";
	}
	
	public static String couchRequestAllDocuments() {
		return "Unable to run the all query";
	}
	
	public static String couchRequestResetDatabase() {
		return "Unable to run the reset query";
	}
	
	public static String couchRequestDeleteDocument() {
		return "Unable to run the delete document query";
	}
	
	public static String couchRequestInvalidRevision() {
		return "The revision is invalid";
	}
	
	public static String couchRequestPostDocument() {
		return "Unable to run the post document query";
	}
	
	public static String unableToGetFullSyncDatabase(String databaseName) {
		return "Unable to get the fullSync database '" + databaseName + "' from the manager";
	}
	
	public static String couchNullResult() {
		return "An error occured during the fullSync request, its result is null";
	}

	public static String couchFullSyncNotActive() {
		return "Unable to use fullSync because it was not activated at the initialization";
	}
	
	public static String fullSyncPutProperties(Map<String, Object> properties) {
		return "Unable to put the following properties in the fullSync Document : " + properties;
	}
	
	public static String fullSyncGetOrCreateDatabase(String databaseName) {
		return "Unable to get or create the fullSync database '" + databaseName + "'";
	}
	
	public static String fullSyncHandleRequest(String requestable, String databaseName, Map<String, Object> parameters) {
		return "Error while running the fullSync request, requestalbe='" + requestable + "', databaseName='" + databaseName + "', parameters=" + parameters;
	}
	
	public static String fullSyncHandleResponse() {
		return "Error while handling the fullSync response";
	}
	
	//*** TAG Certificate ***//
	
	public static String loadKeyStore() {
		return "Failed to load key store";
	}
	
	public static String trustAllCertificates() {
		return "Unable to load a key store trusting all certificates";
	}
	
	public static String clientKeyStore() {
		return "Unable to load the client key store";
	}
	
	public static String serverKeyStore() {
		return "Unable to load the server key store";
	}
	
	//*** TAG Not found ***//
	
	public static String illegalArgumentNotFoundFullSyncView(String viewName, String databaseName) {
		return "Cannot found the view '" + viewName + "' in database '" + databaseName + "'";
	}
	
	//*** TAG Other ***//
	
	public static String unhandledResponseType(String responseType) {
		return "The response type '" + responseType + "' is not handled";
	}
	
	public static String unhandledListenerType(String listenerType) {
		return "The listener type '" + listenerType + "' is not handled";
	}
	
	public static String wrongListener(C8oResponseListener c8oListener) {
		return "The C8oListener class " + C8oUtils.getObjectClassName(c8oListener) + " is not handled";
	}
	
	public static String wrongResult(Object result) {
		return "The response class " + C8oUtils.getObjectClassName(result) + " is not handled";
	}

    public static String ressourceNotFound(String ressourceName) {
        return "The " + ressourceName + " was not found";
    }
	
	public static String toDo() {
		return "todo";
	}
	
	public static String unhandledFullSyncRequestable(String fullSyncRequestableValue) {
		return "The fullSync requestable '" + fullSyncRequestableValue + "' is not handled";
	}
	
	public static String closeInputStream() {
		return "Unable to close the input stream";
	}
	
	public static String stringToJson(String str) {
		return "Unable to deserialize the JSON object from the following String : '" + str + "'";
	}
	
	public static String getNameValuePairObjectValue(NameValuePair nameValuePair) {
		return "Unable to get the value from the NameValuePair with name '" + nameValuePair.getName() + "'";
	}
	
	public static String postDocument() {
		return "Unable to post document";
	}

	public static String getNameValuePairObjectValue(String name) {
		return "Unable to get the object value from the NameValuePair named '" + name + "'";
	}
	
	public static String queryEnumeratorToJSON() {
		return "Unable to parse the QueryEnumerato to a JSON document";
	}

	public static String queryEnumeratorToXML() {
		return "Unable to parse the QueryEnumerato to a XML document";
	}
	
	public static String addparametersToQuery() {
		return "Unable to add parameters to the fullSync query";
	}
	
	public static String putJson() {
		return "Failed to put data in JSON ...";
	}
	
	public static String changeEventToJson() {
		return "Failed to parse ChangeEvent to JSON document";
	}
	
	public static String initC8oSslSocketFactory() {
		return "Failed to initialize C8oSslSocketFactory";
	}
	
	public static String createSslContext() {
		return "failed to create a new SSL context";
	}
	
	public static String keyManagerFactoryInstance() {
		return "Failed to instanciate KeyManagerFactory";
	}
	
	public static String initKeyManagerFactory() {
		return "Failed to initialize the key manager factory";
	}
	
	public static String trustManagerFactoryInstance() {
		return "Failed to instanciate KeyManagerFactory";
	}
	
	public static String initTrustManagerFactory() {
		return "Failed to initialize the key manager factory";
	}
	
	public static String initSslContext() {
		return "Failed to initialize the SSL context";
	}
	
	public static String initCipher() {
		return "Failed to initialize the cipher";
	}
	
	public static String urlEncode() {
		return "Failed to URL encode prameters";
	}
	
	public static String getParametersStringBytes() {
		return "Failed to get parameters string bytes";
	}
	
	public static String encodeParameters()  {
		return "Failed to encode parameters";
	}
	
	public static String runHttpRequest() {
		return "Failed to run the HTTP request";
	}
	
	public static String generateRsaPublicKey() {
		return "Failed to generate RSA public key";
	}
	
	public static String keyFactoryInstance() {
		return "Failed to get KeyFactory instance";
	}
	
	public static String getCipherInstance() {
		return "Failed to get Cipher instance";
	}
	
	public static String entryNotFound(String entryKey) {
		return "Entry key '" + entryKey + "' not found";
	}
	
	public static String c8oCallRequestToJson() {
		return "Failed to parse c8o call request to JSON";
	}
	
	public static String getJsonKey(String key) {
		return "Failed to get the JSON key '" + key + "'";
	}
	
	public static String jsonValueToXML() {
		return "Failed to parse JSON value to XML";
	}
	
	public static String inputStreamToXML() {
		return "Failed to parse InputStream to an XML document";
	}
	
	public static String inputStreamReaderEncoding() {
		return "Failed to instanciate the InputStreamReader";
	}
	
	public static String readLineFromBufferReader() {
		return "Failed to read line from the BufferReader";
	}
	
	public static String getLocalCacheParameters() {
		return "Failed to get local cache parameters";
	}
	
	public static String fullSyncJsonToXML() {
		return "Failed to translate full sync JSON to XML";
	}
	
	public static String takeLog() {
		return "Failed to take a log line in the list";
	}
	
	public static String remoteLogHttpRequest() {
		return "Failed while running the HTTP request sending logs to the Convertigo server";
	}
	
	public static String getInputStreamFromHttpResponse() {
		return "Failed to get InputStream from the HTTP response";
	}
	
	public static String inputStreamToJSON() {
		return "Failed to translate the input stream to a JSON document";
	}
	
	public static String httpInterfaceInstance() {
		return "Failed to instanciate the HTTP interface";
	}
	
	public static String fullSyncInterfaceInstance() {
		return "Failed to instanciate the FullSync interface";
	}
	
	public static String getDocumentFromDatabase(String documentId) {
		return "Failed to get fullSync document '" + documentId + "' from the database";
	}
	
	public static String localCachePolicyIsDisable() {
		return "Depending to the network state the local cache is disabled";
	}
	
	public static String localCacheDocumentJustCreated() {
		return "The local cache document is just created (empty)";
	}
	
	public static String illegalArgumentInvalidLocalCachePolicy(String localCachePolicyString) {
		return "The local cache policy '" + localCachePolicyString + "' is invalid";
	}
	
	public static String timeToLiveExpired() {
		return "The time to live expired";
	}
	
	public static String invalidLocalCacheResponseInformation() {
		return "Local cache response informations are invalid";
	}
	
	public static String overrideDocument() {
		return "Failed to override the fullSync document";
	}
	
	public static String handleFullSyncRequest() {
		return "Failed while running the fullSync request";
	}
	
	public static String serializeC8oCallRequest() {
		return "Failes to serialize the Convertigo call request";
	}
	
	public static String getResponseFromLocalCache() {
		return "Failed to get response from the local cache";
	}
	
	public static String getResponseFromLocalCacheDocument() {
		return "Failed to get response form the local cache document";
	}
	
	public static String handleC8oCallRequest() {
		return "Failed while runnig the c8o call request";
	}
	
	public static String saveResponseToLocalCache() {
		return "Failed to save the response to the local cache";
	}
	
//	public static String illegalArgumentCallParametersNull() {
//		return "Call parameters must be not null";
//	}
//	
//	public static String illegalArgumentCallC8oResponseListenerNull() {
//		return "Call response listener must be not null";
//	}
	
}
