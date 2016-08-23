/*
 * Copyright (c) 2001-2014 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL: svn://devus.twinsoft.fr/convertigo/C8oSDK/Android/trunk/C8oSDKAndroid/src/com/convertigo/clientsdk/C8oSettings.java $
 * $Author: julesg $
 * $Revision: 40249 $
 * $Date: 2015-08-05 17:43:09 +0200 (mer., 05 août 2015) $*
 *
 */

package com.convertigo.clientsdk;

import java.io.InputStream;
import java.util.HashMap;


/**
 * Contains optional parameters of a C8o class instantiation.<br/>
 * Note that setters return the setting instance thereby C8oSettings can be instantiate like that :<br/>
 * new C8oSettings().setTimeout(5000).setTrustAllCetificates(true).setUseEncryption(true);
 *
 * @author julesg
 */
public class C8oSettings extends C8oBase {

	public C8oSettings() {
	}

	public C8oSettings(C8o c8o) {
		copy(c8o);
	}

	/**
	 * Sets the connection timeout to Convertigo in milliseconds. A value of zero means the timeout is not used.<br/>
	 * Default is <b>0</b>.
	 * @param timeout The timeout.
	 * @return The current <b>C8oSettings</b>, for chaining.
	 */
	public C8oSettings setTimeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * Sets a value indicating whether https calls trust all certificates or not.<br/>
	 * Default is <b>false</b>.
	 * @param trustAllCertificates <b>true</b> if https calls trust all certificates; otherwise, <b>false</b>.
	 * @return The current <b>C8oSettings</b>, for chaining.
	 */
	public C8oSettings setTrustAllCertificates(boolean trustAllCertificates) {
		this.trustAllCertificates = trustAllCertificates;
		return this;
	}

	/**
	 * Add a new cookie to the initial cookies send to the Convertigo server.
	 * @param name The name of the new cookie.
	 * @param value The value of the new cookie.
	 * @return The current <b>C8oSettings</b>, for chaining.
	 */
	public C8oSettings addCookie(String name, String value) {
		if (cookies == null) {
			cookies = new HashMap<String, String>();
		}
		cookies.put(name, value);
		return this;
	}

	public C8oSettings setLogC8o(boolean logC8o) {
		this.logC8o = logC8o;
		return this;
	}

	/**
	 * Sets a value indicating if logs are sent to the Convertigo server.<br/>
	 * Default is <b>true</b>.
	 * @return The current <b>C8oSettings</b>, for chaining.
	 */
	public C8oSettings setLogRemote(boolean logRemote) {
		this.logRemote = logRemote;
		return this;
	}

	/**
	 * Sets a value indicating the log level you want in the device console
	 * 0: ALL, 1: NONE, 2: TRACE, 3: DEBUG, 4: INFO, 5: WARN, 6: ERROR, 7: FATAL
	 * or use the android.util.Log constants
	 * Default is <b>0</b>.
	 * @return The current <b>C8oSettings</b>, for chaining.
	 */
	public C8oSettings setLogLevelLocal(int logLevelLocal) {
		this.logLevelLocal = logLevelLocal;
		return this;
	}

	public C8oSettings setLogOnFail(C8oOnFail logOnFail) {
		this.logOnFail = logOnFail;
		return this;
	}

	/**
	 * Specify the default FullSync database name. Must match a Convertigo Server
	 * FullSync connector name
	 *
	 * @param
	 */
	public C8oSettings setDefaultDatabaseName(String defaultDatabaseName) {
		this.defaultDatabaseName = defaultDatabaseName;
		return this;
	}

	public C8oSettings setFullSyncLocalSuffix(String fullSyncLocalSuffix) {
		this.fullSyncLocalSuffix = fullSyncLocalSuffix;
		return this;
	}

	public C8oSettings setAuthenticationCookieValue(String authenticationCookieValue) {
		this.authenticationCookieValue = authenticationCookieValue;
		return this;
	}

	/**
	 * Set if c8o calls variables are encrypted or not
	 *
	 * @param useEncryption
	 * @return this
	 */
	public C8oSettings setUseEncryption(boolean useEncryption) {
		this.useEncryption = useEncryption;
		return this;
	}

	/**
	 * Set if https calls must disable SSL (and use TLS)
	 *
	 * @param disableSSL
	 * @return this
	 */
	public C8oSettings setDisableSSL(boolean disableSSL) {
		this.disableSSL = disableSSL;
		return this;
	}

	/**
	 * Set key store input stream file and its password.
	 *
	 * @param keyStoreInputStream
	 * @param keyStorePassword
	 * @return this
	 */
	public C8oSettings setKeyStoreInputStream(InputStream keyStoreInputStream, String keyStorePassword) {
		this.keyStoreInputStream = keyStoreInputStream;
		this.keyStorePassword = keyStorePassword;
		return this;
	}

	/**
	 * Set trust store input stream file and its password.
	 *
	 * @param trustStoreInputStream
	 * @param trustStorePassword
	 * @return this
	 */
	public C8oSettings setTrustStoreInputStream(InputStream trustStoreInputStream, String trustStorePassword) {
		this.trustStoreInputStream = trustStoreInputStream;
		this.trustStorePassword = trustStorePassword;
		return this;
	}

    /**
     * Set the storage engine for local FullSync databases. Use C8o.FS_STORAGE_SQL or C8o.FS_STORAGE_FORESTDB.
     *
     * @param fullSyncStorageEngine
     * @return this
     */
    public C8oSettings setFullSyncStorageEngine(String fullSyncStorageEngine) {
        if (C8o.FS_STORAGE_SQL.equals(fullSyncStorageEngine) ||
                C8o.FS_STORAGE_FORESTDB.equals(fullSyncStorageEngine)) {
            this.fullSyncStorageEngine = fullSyncStorageEngine;
        }
        return this;
    }

    /**
     * Set the encryption key for local FullSync databases encryption.
     *
     * @param fullSyncEncryptionKey
     * @return
     */
    public C8oSettings setFullSyncEncryptionKey(String fullSyncEncryptionKey) {
        this.fullSyncEncryptionKey = fullSyncEncryptionKey;
        return this;
    }
}
