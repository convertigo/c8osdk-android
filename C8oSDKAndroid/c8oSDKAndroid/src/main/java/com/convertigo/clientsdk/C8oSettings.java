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
import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;


/**
 * Contains optional parameters of a C8o class instantiation.<br/>
 * Note that setters return the setting instance thereby C8oSettings can be instantiate like that :<br/>
 * new C8oSettings().setTimeout(5000).setTrustAllCetificates(true).setUseEncryption(true);
 *
 * @author julesg
 */
public class C8oSettings {
	
	//*** HTTP ***//
	
	/**
	 * The connection timeout to Convertigo in milliseconds. A value of zero means the timeout is not used.<br/>
	 * Default is 0.
	 */
	public int timeout;
	/**
	 * List of initial cookies.<br/>
	 * Default is null.
	 */
	public List<NameValuePair> cookies;
	
	
	//*** Encryption ***//

	/**
	 * Indicate if c8o calls variables are encrypted or not.<br/>
	 * Default is false.
	 */
	public boolean useEncryption;
	/**
	 * Indicate if https calls trust all certificates or not.<br/>
	 * Default is false.
	 */
	public boolean trustAllCertificates;
	/**
	 * Indicate if https calls must disable SSL (and use TLS).<br/>
	 * Default is false.
	 */
    public boolean disableSSL;
	/**
	 * The input stream of the key store file.<br/>
	 * Default is null.
	 */
	public InputStream keyStoreInputStream;
	/**
	 * The key store password.<br/>
	 * Default is null.
	 */
	public String keyStorePassword;
	/**
	 * The input stream of the trust store file.<br/>
	 * Default is null.
	 */
	public InputStream trustStoreInputStream;
	/**
	 * The trust store password.<br/>
	 * Default is null.
	 */
	public String trustStorePassword;
	
	//*** Log ***//
	
	/**
	 * Indicate if logs are sent to the Convertigo server.<br/>
	 * Default is true.
	 */
	public boolean isLogRemote;
	
	public boolean handleExceptionsOnLog;
	
	//*** FullSync ***//
	
	public String defaultFullSyncDatabaseName;
	private String authenticationCookieValue;
	
	//*** Constructor ***//
	
	/**
	 * Construct a C8oSettings instance with default values :<br/>
	 * - timeout = 0<br/>
	 * - useEncryption = false<br/>
	 * - trustAllCertificates = false<br/>
	 * - disableSSL = false<br/>
	 * - cookies = null<br/>
	 * - keystoreInputStream = null<br/>
	 * - keystorePassword = null<br/>
	 * - truststoreInputStream = null<br/>
	 * - truststorePassword = null<br/>
	 * - logRemote = true
	 */
	public C8oSettings() {
		//*** HTTP ***//
		this.timeout = 0;
		this.cookies =  new LinkedList<NameValuePair>();
		//*** Encryption ***//
		this.useEncryption = false;
		this.trustAllCertificates = false;
		this.disableSSL = false;
		this.keyStoreInputStream = null;
		this.keyStorePassword = null;
		this.trustStoreInputStream = null;
		this.trustStorePassword = null;
		//*** Logs ***//
		this.isLogRemote = true;
		this.handleExceptionsOnLog = false;
		//*** FullSync ***//
		this.authenticationCookieValue = null;
		this.defaultFullSyncDatabaseName = null;
	}
	
	//*** toString ***//
	
	@Override
	public String toString() {
		String string = this.getClass().getName() + "[";
		
		string += "timeout = " + timeout + ", " +
				"useEncryption = " + useEncryption + ", " +
				"trustAllCetificates = " + trustAllCertificates +
				"disableSSL = " + disableSSL +
				(cookies != null ? "cookies = " + cookies.toArray() : "") + 
				"]";
				
		return string;
	}
	
	//*** Getter / Setter ***//

	/**
	 * Set the connection timeout to Convertigo in milliseconds
	 * A value of zero means the timeout is not used but it's already instantiate like that
	 *
	 * @param timeout
	 * @return this
	 */
	public C8oSettings setTimeout(int timeout) {
		this.timeout = timeout;
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
	 * Set if https calls trust all certificates or not
	 *
	 * @param trustAllCertificates
	 * @return this
	 */
	public C8oSettings setTrustAllCertificates(boolean trustAllCertificates) {
		this.trustAllCertificates = trustAllCertificates;
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
	 * Add initial cookie.
	 *
	 * @param name
	 * @param value
	 * @return this
	 */
	public C8oSettings addCookie(String name, String value) {
		cookies.add(new BasicNameValuePair(name, value));
		return this;
	}
	
	/**
	 * Set key store input stream file and its password.
	 * 
	 * @param keyStoreInputStream
	 * @param keyStorePassword
	 * @return
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
	 * @return
	 */
	public C8oSettings setTrustStoreInputStream(InputStream trustStoreInputStream, String trustStorePassword) {
		this.trustStoreInputStream = trustStoreInputStream;
		this.trustStorePassword = trustStorePassword;
		return this;
	}
	
	/**
	 * Set if logs are sent to the Convertigo server.
	 * 
	 * @param isLogRemote
	 * @return
	 */
	public C8oSettings setIsLogRemote(boolean isLogRemote) {
		this.isLogRemote = isLogRemote;
		return this;
	}
	
	/**
	 * Specify the default FullSync database name. Must match a Convertigo Server 
	 * FullSync connector name
	 * 
	 * @param 
	 */
	public C8oSettings setDefaultDatabaseName(String name) {
		this.defaultFullSyncDatabaseName = name;
		return this;
	}

	public String getDefaultDatabaseName() {
		return this.defaultFullSyncDatabaseName;
	}

	public String getAuthenticationCookieValue() {
		return authenticationCookieValue;
	}

	public void setAuthenticationCookieValue(String authenticationCookieValue) {
		this.authenticationCookieValue = authenticationCookieValue;
	}

	public C8oSettings setHandleExceptionsOnLog(boolean handleExceptionsOnLog) {
		this.handleExceptionsOnLog = handleExceptionsOnLog;
		return this;
	}
	
}
