package com.convertigo.clientsdk.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import com.convertigo.clientsdk.C8o;
import com.convertigo.clientsdk.C8oSettings;
import com.convertigo.clientsdk.C8oTranslator;
import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oExceptionMessage;
import com.convertigo.clientsdk.exception.C8oHttpRequestException;

public class HttpInterface {
	
	//*** Regular expression ***//
	
	/**
	 * The regex used to handle the c8o public key syntax.
	 */
	private static final Pattern RE_PUBLIC_KEY = Pattern.compile("(.*?)\\|(.*?)\\|(.*?)");
	
	//*** Constants ***//
	
	private static final String RSA_PUBLIC_KEY_PATH = "/rsa/publickey";
	private static final String RSA_ALGORITH_NAME = "RSA";
	private static final String RSA_TRANSFORMATION = "RSA/ECB/NoPadding";
	
	//*** Convertigo ***//
	
	/**
	 * 
	 */
	private C8o c8o;
	
	//*** HTTP ***//
	
	/**
	 * Used to run HTTP requests.
	 */
	private HttpClient httpClient;
	/**
	 * Used to save cookies with several HTTP requests.
	 */
	private HttpContext httpContext;
	
	//*** Encryption ***//
	
	/**
	 * Indicates if c8o calls variables are encrypted or not.
	 */
	private boolean useEncryption;
	/**
	 * Provides the functionality of a cryptographic cipher for encryption and decryption
	 * Use an array to synchronized on it before initialize a cipher
	 */
	private Cipher[] cipher;
	/**
	 * ???
	 */
	private int maxBlockSize;
	/**
	 * The last timestamp used in an encoded c8o call
	 * Use an array to synchronized on it before initialize a timestamp
	 */
	private long[] lastTs = {0};
	
	//*** Constructors ***//

	public HttpInterface(C8o c8o, String endpoint, C8oSettings c8oSettings) throws C8oException {
		this.c8o = c8o;
		this.useEncryption = c8oSettings.useEncryption;
		
		// Create a HttpParams to set the connection timeout
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, c8oSettings.timeout);
		// HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
		// HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
		
		// Convert the endpoint to URL
		URL url = null;
		try {
			url = new URL(endpoint);
		} catch (MalformedURLException e) {
			// Can never happen normally because this is already checked
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidURL(endpoint), e);
		}
		String protocol = url.getProtocol();
		
		// SSL certificates
		if (protocol.equals("https")) {
			int port = url.getPort();
			if (port == -1) {
				port = 443;
			}

			// Get the client key store file
			KeyStore keyStore = null;
			if (c8oSettings.keyStoreInputStream != null && c8oSettings.keyStorePassword != null) {
				try {
					keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				} catch (KeyStoreException e) {
					throw new C8oException(C8oExceptionMessage.clientKeyStore(), e);
				}
				try {
					keyStore.load(c8oSettings.keyStoreInputStream, c8oSettings.keyStorePassword.toCharArray());
				} catch (NoSuchAlgorithmException e) {
					throw new C8oException(C8oExceptionMessage.clientKeyStore(), e);
				} catch (CertificateException e) {
					throw new C8oException(C8oExceptionMessage.clientKeyStore(), e);
				} catch (IOException e) {
					throw new C8oException(C8oExceptionMessage.clientKeyStore(), e);
				}
			}
			// Get the server key store file
			KeyStore trustStore = null;
			if (c8oSettings.trustStoreInputStream != null && c8oSettings.trustStorePassword != null) {
				try {
					trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				} catch (KeyStoreException e) {
					throw new C8oException(C8oExceptionMessage.serverKeyStore(), e);
				}
				try {
					trustStore.load(c8oSettings.trustStoreInputStream, c8oSettings.trustStorePassword.toCharArray());
				} catch (NoSuchAlgorithmException e) {
					throw new C8oException(C8oExceptionMessage.serverKeyStore(), e);
				} catch (CertificateException e) {
					throw new C8oException(C8oExceptionMessage.serverKeyStore(), e);
				} catch (IOException e) {
					throw new C8oException(C8oExceptionMessage.serverKeyStore(), e);
				}
			}
			
			C8oSslSocketFactory sslSocketFactory;
			try {
				if (keyStore == null && trustStore == null) {
					sslSocketFactory = new C8oSslSocketFactory(c8oSettings.trustAllCertificates, c8oSettings.disableSSL, this.c8o);
				} else if (keyStore == null) {
					sslSocketFactory = new C8oSslSocketFactory(c8oSettings.trustAllCertificates, c8oSettings.disableSSL, trustStore, this.c8o);
				} else if (trustStore == null) {
					sslSocketFactory = new C8oSslSocketFactory(c8oSettings.trustAllCertificates, c8oSettings.disableSSL, keyStore, c8oSettings.keyStorePassword, this.c8o);
				} else {
					sslSocketFactory = new C8oSslSocketFactory(c8oSettings.trustAllCertificates, c8oSettings.disableSSL, keyStore, c8oSettings.keyStorePassword, trustStore, this.c8o);
				}
			} catch (C8oException e) {
				throw new C8oException(C8oExceptionMessage.initSslSocketFactory(), e);
			} catch (KeyManagementException e) {
				throw new C8oException(C8oExceptionMessage.initSslSocketFactory(), e);
			} catch (UnrecoverableKeyException e) {
				throw new C8oException(C8oExceptionMessage.initSslSocketFactory(), e);
			} catch (NoSuchAlgorithmException e) {
				throw new C8oException(C8oExceptionMessage.initSslSocketFactory(), e);
			} catch (KeyStoreException e) {
				throw new C8oException(C8oExceptionMessage.initSslSocketFactory(), e);
			}
				
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme(protocol, sslSocketFactory, port));
			
			this.httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(httpParams, schemeRegistry), httpParams);
		} else {
			// Initialize the HttpClient
			this.httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(httpParams, new DefaultHttpClient().getConnectionManager().getSchemeRegistry()), httpParams);
		}
		
		// Initialize a local instance of HttpContext to save cookies
		this.httpContext = new BasicHttpContext();
		// Create a local instance of cookie store
	    CookieStore cookieStore = new BasicCookieStore();
	    // Bind cookie store to the local context
	    this.httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	    
	    // Set initial cookies
    	for (NameValuePair cookie : c8oSettings.cookies) {
    		addCookie(cookie.getName(), cookie.getValue());
    	}
	    
	    // If the user want to encode queries string of c8o calls
	    if (this.useEncryption) {
	    	// Initialize the cipher array in order to synchronized it
	    	this.cipher = new Cipher[1];
	    	
	    	// Create an AsyncTask to initialize the cipher in a background thread
	    	// This operation need to call Convertigo to get the Convertigo public key
	    	// So it has to be executed in a background thread cause netwotk operations can't be executed on the main thread
	    	AsyncTask<Void, Void, Object> initCipherTask = new AsyncTask<Void, Void, Object>() {
				@Override
				protected Object doInBackground(Void... params) {
					try {
						// Initialize the cipher
						initCipher();
					} catch (C8oException e) {
						return new C8oException(C8oExceptionMessage.initCipher(), e);
					}
					
					return null;
				}

				@Override
				protected void onPostExecute(Object result) {
					// If doInBackGround didn't work
					if (result instanceof Exception) {
						// Handle exception with empty request parameters
						C8o.handleCallException(HttpInterface.this.c8o.getDefaultC8oExceptionListener(), new ArrayList<NameValuePair>(),  (Exception) result);
					}
				}
	    	};
	    	
	    	// Then init the cipher
	    	initCipherTask.execute();
	    }
	}
	
	public HttpResponse handleC8oCallRequest(String c8oCallUrl, List<NameValuePair> parameters) throws C8oHttpRequestException, C8oException {
		
		HttpPost request = new HttpPost(c8oCallUrl);
		
		if (this.useEncryption) {
			// Initialize the cipher
			try {
				this.initCipher();
			} catch (C8oException e) {
				throw new C8oException(C8oExceptionMessage.initCipher(), e);
			}
			
			// Get parameters iterator
			Iterator<NameValuePair> parametersIterator = parameters.iterator();
			
			// Build a query string with parameters
			String parametersString = "";
			while (parametersIterator.hasNext()) {
				NameValuePair parameter = parametersIterator.next();
				try {
					parametersString = parametersString + "&" + URLEncoder.encode(parameter.getName(), "UTF-8") + "=" + URLEncoder.encode(parameter.getValue(), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new C8oException(C8oExceptionMessage.urlEncode(), e);
				}
			}
			
			// Even if there is no parameter, Convertigo still need "&"
			if (parametersString == "") {
				parametersString = "&";
			}
			
			// Get the time stamp
			long ts = System.currentTimeMillis();
			
			// Check if the time stamp is the same or older than the last time stamp
			synchronized (this.lastTs) {
				if (ts <= this.lastTs[0]) {
					ts = this.lastTs[0] + 1;
				}
				this.lastTs[0] = ts;
			}
			
			parametersString = "ts=" + ts + parametersString;
			
			// Create a cipher with the timestamp and parameters
			byte[] toCipher;
			try {
				toCipher = (parametersString).getBytes("utf-8");
			} catch (UnsupportedEncodingException e) {
				throw new C8oException(C8oExceptionMessage.getParametersStringBytes(), e);
			}
			
			// Encode the query string
			StringBuffer encoded = new StringBuffer();
			for (int i = 0; i < toCipher.length; i += this.maxBlockSize) {
				try {
					encoded.append(C8oTranslator.byteArrayToHexString(cipher[0].doFinal(toCipher, i, Math.min(this.maxBlockSize, toCipher.length - i))));
				} catch (IllegalBlockSizeException e) {
					throw new C8oException(C8oExceptionMessage.encodeParameters(), e);
				} catch (BadPaddingException e) {
					throw new C8oException(C8oExceptionMessage.encodeParameters(), e);
				}
			}
			
			// Remove all parameters (clear ?)
			parameters = new ArrayList<NameValuePair>();
			// Add the encoded parameter
			parameters.add(new BasicNameValuePair(C8o.ENGINE_PARAMETER_ENCODED, encoded.toString()));
		}
		
		this.c8o.c8oLogger.logC8oCall(c8oCallUrl, parameters);
		
		// Set parameters of the POST request
		try {
			request.setEntity(new UrlEncodedFormEntity(parameters));
		} catch (UnsupportedEncodingException e) {
			throw new C8oException(C8oExceptionMessage.urlEncode(), e);
		}
						
		try {
			return this.httpClient.execute(request, this.httpContext);
		} catch (ClientProtocolException e) {
			throw new C8oHttpRequestException(C8oExceptionMessage.runHttpRequest(), e);
		} catch (IOException e) {
			throw new C8oHttpRequestException(C8oExceptionMessage.runHttpRequest(), e);
		}
	}
	
	public HttpResponse handleRequest(HttpPost request) throws C8oHttpRequestException {
		try {
			return this.httpClient.execute(request, this.httpContext);
		} catch (ClientProtocolException e) {
			throw new C8oHttpRequestException(C8oExceptionMessage.runHttpRequest(), e);
		} catch (IOException e) {
			throw new C8oHttpRequestException(C8oExceptionMessage.runHttpRequest(), e);
		}
	}
	
	/**
	 * Initialize the cipher used to encode query string in c8o calls.<br/>
	 * Have to be called in a background thread cause it need to call Convertigo to get the Convertigo public key (Network operations have to be executed in a backgrounf thread)
	 * @throws C8oException 
	 */
	@SuppressLint("TrulyRandom")
	private void initCipher() throws C8oException {
		this.c8o.c8oLogger.logMethodCall("initCipher");
		
		synchronized(cipher) {
			if (cipher[0] == null) {
				// build the URL to get the public key
				String publicKeyUrl = this.c8o.getEndpointGroup(1) + HttpInterface.RSA_PUBLIC_KEY_PATH;
				
				// Build the request with the url
		    	HttpPost request = new HttpPost(publicKeyUrl);
				// Do the http request to get the public key
				HttpResponse response = null;
				try {
					response = httpClient.execute(request, httpContext);
				} catch (ClientProtocolException e) {
					throw new C8oException(C8oExceptionMessage.retrieveRsaPublicKey(), e);
				} catch (IOException e) {
					throw new C8oException(C8oExceptionMessage.retrieveRsaPublicKey(), e);
				}
				
				// Get the result first line (normally the public key)
				String publicKey;
				BufferedReader bufferedReader;
				try {
					bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new C8oException(C8oExceptionMessage.parseRsaPublicKey(), e);
				} catch (IllegalStateException e) {
					throw new C8oException(C8oExceptionMessage.parseRsaPublicKey(), e);
				} catch (IOException e) {
					throw new C8oException(C8oExceptionMessage.parseRsaPublicKey(), e);
				}
				try {
					publicKey = bufferedReader.readLine();
				} catch (IOException e) {
					throw new C8oException(C8oExceptionMessage.parseRsaPublicKey(), e);
				}

				// Apply regex on the public key
				Matcher responseParts = HttpInterface.RE_PUBLIC_KEY.matcher(publicKey);
				if (responseParts.matches()) {
					// Convert base 16 (hexadecimal) to BigInteger
					BigInteger modulus = new BigInteger(responseParts.group(2), 16);
					BigInteger pubExp = new BigInteger(responseParts.group(1), 16);
					this.maxBlockSize = modulus.bitLength() / 8;

					KeyFactory keyFactory;
					try {
						keyFactory = KeyFactory.getInstance(HttpInterface.RSA_ALGORITH_NAME);
					} catch (NoSuchAlgorithmException e) {
						throw new C8oException(C8oExceptionMessage.keyFactoryInstance(), e);
					}
					RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(modulus, pubExp);
					RSAPublicKey key;
					try {
						key = (RSAPublicKey) keyFactory.generatePublic(pubKeySpec);
					} catch (InvalidKeySpecException e) {
						throw new C8oException(C8oExceptionMessage.generateRsaPublicKey(), e);
					}
					try {
						cipher[0] = Cipher.getInstance(HttpInterface.RSA_TRANSFORMATION);
					} catch (NoSuchAlgorithmException e) {
						throw new C8oException(C8oExceptionMessage.getCipherInstance(), e);
					} catch (NoSuchPaddingException e) {
						throw new C8oException(C8oExceptionMessage.getCipherInstance(), e);
					}
					try {
						cipher[0].init(Cipher.ENCRYPT_MODE, key);
					} catch (InvalidKeyException e) {
						throw new C8oException(C8oExceptionMessage.initCipher(), e);
					}
				}
			}
		}
	}
	
	//*** Getter / Setter ***//
	
	/**
	 * Add a cookie to the cookie store.<br/>
	 * Automatically set the domain and secure flag using the c8o endpoint.
	 *
	 * @param name
	 * @param value
	 */
	public void addCookie(String name, String value) {
		CookieStore cookieStore = getCookieStore();
		if (cookieStore != null) {
			BasicClientCookie2 cookie = new BasicClientCookie2(name, value);
			cookie.setDomain(this.c8o.getEndpointGroup(3));
			cookie.setSecure(this.c8o.getEndpointGroup(2) != null);
			cookieStore.addCookie(cookie);
		}
	}
	
	/**
	 * Get the c8o cookie store, used by all HTTP request. Cookies can be read, modified and added.
	 *
	 * @return cookieStore
	 */
	public CookieStore getCookieStore() {
		return (CookieStore) this.httpContext.getAttribute(ClientContext.COOKIE_STORE);
	}
	
}
