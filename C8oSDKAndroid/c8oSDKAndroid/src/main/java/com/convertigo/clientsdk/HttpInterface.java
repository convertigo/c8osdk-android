package com.convertigo.clientsdk;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oHttpRequestException;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

class HttpInterface {
	
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

    private boolean[] firstCall = {true};

	//*** Constructors ***//

	public HttpInterface(final C8o c8o) throws C8oException {
		this.c8o = c8o;
		
		// Create a HttpParams to set the connection timeout
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, c8o.getTimeout());
		HttpConnectionParams.setConnectionTimeout(httpParams, c8o.getTimeout());
		// HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
		// HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
		
		// Convert the endpoint to URL
		URL url = null;
		try {
			url = new URL(c8o.getEndpoint());
		} catch (MalformedURLException e) {
			// Can never happen normally because this is already checked
			throw new IllegalArgumentException(C8oExceptionMessage.illegalArgumentInvalidURL(c8o.getEndpoint()), e);
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
			if (c8o.getKeyStoreInputStream() != null && c8o.getKeyStorePassword() != null) {
				try {
					keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				} catch (KeyStoreException e) {
					throw new C8oException(C8oExceptionMessage.clientKeyStore(), e);
				}
				try {
					keyStore.load(c8o.getKeyStoreInputStream(), c8o.getKeyStorePassword().toCharArray());
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
			if (c8o.getTrustStoreInputStream() != null && c8o.getTrustStorePassword() != null) {
				try {
					trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				} catch (KeyStoreException e) {
					throw new C8oException(C8oExceptionMessage.serverKeyStore(), e);
				}
				try {
					trustStore.load(c8o.getTrustStoreInputStream(), c8o.getTrustStorePassword().toCharArray());
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
					sslSocketFactory = new C8oSslSocketFactory(c8o.isTrustAllCertificates(), c8o.isDisableSSL(), this.c8o);
				} else if (keyStore == null) {
					sslSocketFactory = new C8oSslSocketFactory(c8o.isTrustAllCertificates(), c8o.isDisableSSL(), trustStore, this.c8o);
				} else if (trustStore == null) {
					sslSocketFactory = new C8oSslSocketFactory(c8o.isTrustAllCertificates(), c8o.isDisableSSL(), keyStore, c8o.getKeyStorePassword(), this.c8o);
				} else {
					sslSocketFactory = new C8oSslSocketFactory(c8o.isTrustAllCertificates(), c8o.isDisableSSL(), keyStore, c8o.getKeyStorePassword(), trustStore, this.c8o);
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
			schemeRegistry.register(new DefaultHttpClient().getConnectionManager().getSchemeRegistry().getScheme("http"));
			
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
		if (c8o.getCookies() != null) {
			for (Entry<String, String> cookie : c8o.getCookies().entrySet()) {
				addCookie(cookie.getKey(), cookie.getValue());
			}
		}
	    
	    // If the user want to encode queries string of c8o calls
	    if (c8o.isUseEncryption()) {
	    	// Initialize the cipher array in order to synchronized it
	    	this.cipher = new Cipher[1];
	    	
	    	// Create an AsyncTask to initialize the cipher in a background thread
	    	// This operation need to call Convertigo to get the Convertigo public key
	    	// So it has to be executed in a background thread cause network operations can't be executed on the main thread
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
						c8o.handleCallException(null, new HashMap<String, Object>(),  (Exception) result);
					}
				}
	    	};
	    	
	    	// Then init the cipher
	    	initCipherTask.execute();
	    }
	}
	
	public HttpResponse handleC8oCallRequest(String url, Map<String, Object> parameters) throws C8oHttpRequestException, C8oException {
		HttpPost request = new HttpPost(url);
		request.addHeader("x-convertigo-sdk", C8o.getSdkVersion());
		request.addHeader("User-Agent", "Convertigo Client SDK " + C8o.getSdkVersion());

		if (c8o.isUseEncryption()) {
			// Initialize the cipher
			try {
				this.initCipher();
			} catch (C8oException e) {
				throw new C8oException(C8oExceptionMessage.initCipher(), e);
			}
			
			// Get parameters iterator
			Iterator<Entry<String, Object>> parametersIterator = parameters.entrySet().iterator();
			
			// Build a query string with parameters
			String parametersString = "";
			while (parametersIterator.hasNext()) {
                Entry<String, Object> parameter = parametersIterator.next();

				Object value = parameter.getValue();
				if (value != null && value.getClass().isArray()) {
					value = Arrays.asList((Object[]) value);
				}
				if (value instanceof Collection) {
					for (Object v: (Collection) value) {
						try {
							parametersString += "&" + URLEncoder.encode(parameter.getKey(), "UTF-8") + "=" + URLEncoder.encode("" + v, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							throw new C8oException(C8oExceptionMessage.urlEncode(), e);
						}
					}
				} else {
					try {
						parametersString += "&" + URLEncoder.encode(parameter.getKey(), "UTF-8") + "=" + URLEncoder.encode("" + value, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						throw new C8oException(C8oExceptionMessage.urlEncode(), e);
					}
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
				catch(NullPointerException e){
					throw new C8oException(C8oExceptionMessage.noServerOnEndPoint(),e);
				}
			}
			
			// Remove all parameters (clear ?)
			parameters = new HashMap<String, Object>();
			// Add the encoded parameter
			parameters.put(C8o.ENGINE_PARAMETER_ENCODED, encoded.toString());
		}
		
		c8o.log.logC8oCall(url, parameters);
		
		// Set parameters of the POST request
		try {
            List<NameValuePair> params = new LinkedList<NameValuePair>();
            for (Entry<String, Object> parameter : parameters.entrySet()) {
				Object value = parameter.getValue();
				if (value != null && value.getClass().isArray()) {
					value = Arrays.asList((Object[]) value);
				}
				if (value instanceof Collection) {
					for (Object v: (Collection) value) {
						params.add(new BasicNameValuePair(parameter.getKey(), "" + v));
					}
				} else {
					params.add(new BasicNameValuePair(parameter.getKey(), "" + value));
				}
            }
			request.setEntity(new UrlEncodedFormEntity(params));
		} catch (UnsupportedEncodingException e) {
			throw new C8oException(C8oExceptionMessage.urlEncode(), e);
		}

		return handleRequest(request);
	}
	
	public HttpResponse handleRequest(HttpPost request) throws C8oHttpRequestException {
		try {
            synchronized (firstCall) {
                if (firstCall[0]) {
                    HttpResponse response = httpClient.execute(request, httpContext);
                    firstCall[0] = false;
                    return response;
                }
            }
            return httpClient.execute(request, httpContext);
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
		synchronized(cipher) {
			if (cipher[0] == null) {
				// build the URL to get the public key
				String publicKeyUrl = this.c8o.getEndpointConvertigo() + HttpInterface.RSA_PUBLIC_KEY_PATH;
				
				// Build the request with the url
		    	HttpPost request = new HttpPost(publicKeyUrl);
				// Do the http request to get the public key
				HttpResponse response = null;
				try {
					response = handleRequest(request);
				} catch (C8oHttpRequestException e) {
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
			cookie.setDomain(c8o.getEndpointHost());
			cookie.setSecure(c8o.getEndpointIsSecure());
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
