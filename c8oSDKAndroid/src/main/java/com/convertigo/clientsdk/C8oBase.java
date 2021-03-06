package com.convertigo.clientsdk;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nicolasa on 13/11/2015.
 */
class C8oBase {
    //*** HTTP ***//

    protected int timeout = 0;
    protected boolean trustAllCertificates = false;
    protected Map<String, String> cookies = new HashMap<String, String>();
    protected Map<String, String> headers = new HashMap<String, String>();

    //*** Log ***//

    protected boolean logRemote = true;
    protected int logLevelLocal = 0;
    protected boolean logC8o = true;
    protected C8oOnFail logOnFail = null;

    //*** FullSync ***//

    protected String defaultDatabaseName = null;
    protected String authenticationCookieValue = null;
    protected String fullSyncLocalSuffix = null;
    protected String fullSyncStorageEngine = C8o.FS_STORAGE_SQL;
    protected String fullSyncEncryptionKey = null;
    protected String fullSyncServerUrl = null;
    protected String fullSyncUsername = null;
    protected String fullSyncPassword = null;

    //*** Encryption ***//

    protected boolean useEncryption = false;
    protected boolean disableSSL = false;
    protected InputStream keyStoreInputStream;
    protected String keyStorePassword;
    protected InputStream trustStoreInputStream;
    protected String trustStorePassword;

    //*** Getter ***//

    /**
     * Gets the connection timeout to Convertigo in milliseconds. A value of zero means the timeout is not used.<br/>
     * Default is <b>0</b>.
     * @return The timeout.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Gets a value indicating whether https calls trust all certificates or not.<br/>
     * Default is <b>false</b>.
     * @return <b>true</b> if https calls trust all certificates; otherwise, <b>false</b>.
     */
    public boolean isTrustAllCertificates() {
        return trustAllCertificates;
    }

    /**
     * Gets initial cookies to send to the Convertigo server.<br/>
     * Default is <b>null</b>.
     * @return List of cookies.
     */
    public Map<String, String> getCookies() {
        return cookies;
    }

    /**
     * Gets initial headers to send to the Convertigo server.<br/>
     * Default is <b>null</b>.
     * @return List of headers.
     */
    public Map<String, String> getHeaders() {
        return cookies;
    }

    /**
     * Gets a value indicating if logs are sent to the Convertigo server.<br/>
     * Default is <b>true</b>.
     * @return <b>true</b> if logs are sent to the Convertigo server; otherwise, <b>false</b>.
     */
    public boolean isLogRemote() {
        return logRemote;
    }

    public int getLogLevelLocal() {
        return logLevelLocal;
    }

    public boolean isLogC8o() {
        return logC8o;
    }

    public C8oOnFail getLogOnFail() {
        return logOnFail;
    }

    public String getDefaultDatabaseName() {
        return defaultDatabaseName;
    }

    public String getFullSyncLocalSuffix() {
        return fullSyncLocalSuffix;
    }

    public String getFullSyncStorageEngine() {
        return fullSyncStorageEngine;
    }

    public String getFullSyncEncryptionKey() {
        return fullSyncEncryptionKey;
    }

    public String getFullSyncServerUrl() {
        return fullSyncServerUrl;
    }

    public String getFullSyncUsername() {
        return fullSyncUsername;
    }

    public String getFullSyncPassword() {
        return fullSyncPassword;
    }

    public String getAuthenticationCookieValue() {
        return authenticationCookieValue;
    }

    public boolean isUseEncryption() {
        return useEncryption;
    }

    public boolean isDisableSSL() {
        return disableSSL;
    }

    public InputStream getKeyStoreInputStream() {
        return keyStoreInputStream;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public InputStream getTrustStoreInputStream() {
        return trustStoreInputStream;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    protected void copy(C8oBase c8oBase) {

        //*** HTTP ***//

        timeout = c8oBase.timeout;
        trustAllCertificates = c8oBase.trustAllCertificates;
        cookies.putAll(c8oBase.cookies);
        headers.putAll(c8oBase.headers);

        //*** Log ***//

        logRemote = c8oBase.logRemote;
        logLevelLocal = c8oBase.logLevelLocal;
        logC8o = c8oBase.logC8o;
        logOnFail = c8oBase.logOnFail;

        //*** FullSync ***//

        defaultDatabaseName = c8oBase.defaultDatabaseName;
        authenticationCookieValue = c8oBase.authenticationCookieValue;
        fullSyncLocalSuffix = c8oBase.fullSyncLocalSuffix;
        fullSyncStorageEngine = c8oBase.fullSyncStorageEngine;
        fullSyncEncryptionKey = c8oBase.fullSyncEncryptionKey;
        fullSyncServerUrl = c8oBase.fullSyncServerUrl;
        fullSyncUsername = c8oBase.fullSyncUsername;
        fullSyncPassword = c8oBase.fullSyncPassword;

        //*** Encryption ***//

        useEncryption = c8oBase.useEncryption;
        disableSSL = c8oBase.disableSSL;
        keyStoreInputStream = c8oBase.keyStoreInputStream;
        keyStorePassword = c8oBase.keyStorePassword;
        trustStoreInputStream = c8oBase.trustStoreInputStream;
        trustStorePassword = c8oBase.trustStorePassword;
    }
}
