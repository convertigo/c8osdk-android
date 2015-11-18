package com.convertigo.clientsdk;

import org.apache.http.NameValuePair;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nicolasa on 13/11/2015.
 */
class C8oBase {
    //*** HTTP ***//

    protected int timeout = 0;
    protected boolean trustAllCertificates = false;
    protected List<NameValuePair> cookies = new LinkedList<NameValuePair>();

    //*** Log ***//

    protected boolean logRemote = true;
    protected boolean handleExceptionsOnLog;

    //*** FullSync ***//

    protected String defaultDatabaseName;
    protected String authenticationCookieValue;

    //*** Encryption ***//

    protected boolean useEncryption;
    protected boolean disableSSL;
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
    public List<NameValuePair> getCookies() {
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

    public boolean isHandleExceptionsOnLog() {
        return handleExceptionsOnLog;
    }

    public String getDefaultDatabaseName() {
        return defaultDatabaseName;
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
        cookies = new LinkedList<NameValuePair>(c8oBase.cookies);

        //*** Log ***//

        logRemote = c8oBase.logRemote;
        handleExceptionsOnLog = c8oBase.handleExceptionsOnLog;

        //*** FullSync ***//

        defaultDatabaseName = c8oBase.defaultDatabaseName;
        authenticationCookieValue = c8oBase.authenticationCookieValue;

        //*** Encryption ***//

        useEncryption = c8oBase.useEncryption;
        disableSSL = c8oBase.disableSSL;
        keyStoreInputStream = c8oBase.keyStoreInputStream;
        keyStorePassword = c8oBase.keyStorePassword;
        trustStoreInputStream = c8oBase.trustStoreInputStream;
        trustStorePassword = c8oBase.trustStorePassword;
    }
}
