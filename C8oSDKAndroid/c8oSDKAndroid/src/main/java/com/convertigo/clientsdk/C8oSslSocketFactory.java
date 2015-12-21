package com.convertigo.clientsdk;

import com.convertigo.clientsdk.exception.C8oException;

import org.apache.http.conn.ssl.SSLSocketFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * C8oSslSocketFactory is an SSLSocketFactory allowing to use custom key and trust stores with HTTPS request 
 * but also to trust all certificates and to disable SSL protocols (only TLS).
 */
class C8oSslSocketFactory extends SSLSocketFactory {
	/**
	 * Contains key and trust managers.
	 */
    private SSLContext sslContext;
    /**
     * Indicates to disable SSL protocols (keep only TLS).
     */
    private boolean disableSSL;
    /**
     * List of allowed protocols in case that SSL protocols are disabled.
     */
    private String[] withoutSSLProtocols;
    /**
     * Only used to log.
     */
    private C8o c8o;
    
    //*** TAG Constructors ***//

    /**
     * Create a C8oSslSocketFactory with custom key and trust stores.
     * 
     * @param trustAllCertificates
     * @param disableSSL
     * @param keyStore
     * @param keyStorePassword
     * @param trustStore
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws IOException
     * @throws C8oException 
     */
    public C8oSslSocketFactory(boolean trustAllCertificates, boolean disableSSL, KeyStore keyStore, String keyStorePassword, KeyStore trustStore, C8o c8o) throws C8oException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
    	super(keyStore, keyStorePassword, newTrustStoretore(trustAllCertificates, trustStore));
    	try {
			this.init(trustAllCertificates, disableSSL, keyStore, keyStorePassword, trustStore, c8o);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.initC8oSslSocketFactory(), e);
		}
    }
    
    /**
     * Create a C8oSslSocketFactory with a custom key store and a default trust store.
     * 
     * @param trustAllCertificates
     * @param disableSSL
     * @param keyStore
     * @param keyStorePassword
     * @throws C8oException 
     * @throws KeyStoreException 
     * @throws NoSuchAlgorithmException 
     * @throws UnrecoverableKeyException 
     * @throws KeyManagementException 
     */
    public C8oSslSocketFactory(boolean trustAllCertificates, boolean disableSSL, KeyStore keyStore, String keyStorePassword, C8o c8o) throws C8oException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException{
        super(keyStore, keyStorePassword);
        try {
			this.init(trustAllCertificates, disableSSL, keyStore, keyStorePassword, null, c8o);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.initC8oSslSocketFactory(), e);
		}
    }
    
    /**
     * Create a C8oSslSocketFactory with a default key store and a custom trust store.
     * 
     * @param trustAllCertificates
     * @param disableSSL
     * @param trustStore
     * @throws C8oException 
     * @throws KeyStoreException 
     * @throws NoSuchAlgorithmException 
     * @throws UnrecoverableKeyException 
     * @throws KeyManagementException 
     */
    public C8oSslSocketFactory(boolean trustAllCertificates, boolean disableSSL, KeyStore trustStore, C8o c8o) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, C8oException {
        super(newTrustStoretore(trustAllCertificates, trustStore));
        try {
			this.init(trustAllCertificates, disableSSL, null, null, trustStore, c8o);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.initC8oSslSocketFactory(), e);
		}
    }
    
    /**
     * Create a C8oSslSocketFactory with  default key and trust stores.
     * 
     * @param trustAllCertificates
     * @param disableSSL
     * @throws C8oException 
     * @throws KeyStoreException 
     * @throws NoSuchAlgorithmException 
     * @throws UnrecoverableKeyException 
     * @throws KeyManagementException 
     */
    public C8oSslSocketFactory(boolean trustAllCertificates, boolean disableSSL, C8o c8o) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, C8oException{
        super(newTrustStoretore(trustAllCertificates, null));
        try {
			this.init(trustAllCertificates, disableSSL, null, null, null, c8o);
		} catch (C8oException e) {
			throw new C8oException(C8oExceptionMessage.initC8oSslSocketFactory(), e);
		}
    }
    
    //*** TAG Initialization ***//
    
    /**
     * Returns an empty TrustStore if trustAllCertificates is true, 
     * else returns the TrustStore parameter (maybe null).
     * 
     * @param trustAllCertificates
     * @param trustStore
     * @return
     * @throws C8oException 
     */
    private static KeyStore newTrustStoretore(boolean trustAllCertificates, KeyStore trustStore) throws C8oException {    	
    	if (trustAllCertificates) {
        	// Create a KeyStore default type
        	try {
				trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			} catch (KeyStoreException e) {
				throw new C8oException(C8oExceptionMessage.loadKeyStore(), e);
			}
	    	// Initialize it like an empty KeyStore
        	try {
				trustStore.load(null, null);
			} catch (NoSuchAlgorithmException e) {
				throw new C8oException(C8oExceptionMessage.loadKeyStore(), e);
			} catch (CertificateException e) {
				throw new C8oException(C8oExceptionMessage.loadKeyStore(), e);
			} catch (IOException e) {
				throw new C8oException(C8oExceptionMessage.loadKeyStore(), e);
			}
		}
    	return trustStore;
    }
    
    /**
     * Initialize the SSLContext with key and trust managers using key and trust stores (maybe null).
     * 
     * @param trustAllCertificates
     * @param disableSSL
     * @param keyStore
     * @param keyStorePassword
     * @param trustStore
     * @throws C8oException 
     */
    private void init(boolean trustAllCertificates, boolean disableSSL, KeyStore keyStore, String keyStorePassword, KeyStore trustStore, C8o c8o) throws C8oException {
    	this.disableSSL = disableSSL;
    	try {
			this.sslContext = SSLContext.getInstance("TLS");
		} catch (NoSuchAlgorithmException e) {
			throw new C8oException(C8oExceptionMessage.createSslContext(), e);
		}
    	this.c8o = c8o;
    	
    	KeyManager[] customKeyManagers = null;
    	TrustManager[] customTrustManagers = null;
    	
    	if (keyStore != null && keyStorePassword != null) {
    		KeyManagerFactory kmf;
			try {
				kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			} catch (NoSuchAlgorithmException e) {
				throw new C8oException(C8oExceptionMessage.keyManagerFactoryInstance(), e);
			}
        	try {
				kmf.init(keyStore, keyStorePassword.toCharArray());
			} catch (UnrecoverableKeyException e) {
				throw new C8oException(C8oExceptionMessage.initKeyManagerFactory(), e);
			} catch (KeyStoreException e) {
				throw new C8oException(C8oExceptionMessage.initKeyManagerFactory(), e);
			} catch (NoSuchAlgorithmException e) {
				throw new C8oException(C8oExceptionMessage.initKeyManagerFactory(), e);
			}
        	customKeyManagers = kmf.getKeyManagers();
    	}
    	
    	if (trustAllCertificates) {
    		customTrustManagers = new TrustManager[] { new X509TrustManager() {
        		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
        		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
        		public X509Certificate[] getAcceptedIssuers() {return null;}
        	}};
        	setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    	} else if (trustStore != null) {
    		TrustManagerFactory tmf;
			try {
				tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			} catch (NoSuchAlgorithmException e) {
				throw new C8oException(C8oExceptionMessage.trustManagerFactoryInstance(), e);
			}
        	try {
				tmf.init(trustStore);
			} catch (KeyStoreException e) {
				throw new C8oException(C8oExceptionMessage.initTrustManagerFactory(), e);
			}
        	customTrustManagers = tmf.getTrustManagers();
    	}
    	
    	try {
			sslContext.init(customKeyManagers, customTrustManagers, null);
		} catch (KeyManagementException e) {
			throw new C8oException(C8oExceptionMessage.initSslContext(), e);
		}
    }
    
    //*** TAG Override methods ***//
    
    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
    	Socket sock;
		try {
			sock = sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		} catch (IOException e) {
			// Cannot throw another exception because this is an override method
			throw e;
		}
    	if (sock instanceof SSLSocket) {
    		// Throws exceptions other than IOException 
    		try {
    			Method setHostnameMethod = sock.getClass().getMethod("setHostname", String.class);
    			setHostnameMethod.invoke(sock, host);
    		} catch (Exception e) {
				c8o.log._debug("SNI not useable");
    		}
    	}
    	
        return checkSocket(sock);
    }

    @Override
    public Socket createSocket() throws IOException {
    	Socket sock;
		try {
			sock = sslContext.getSocketFactory().createSocket();
		} catch (IOException e) {
			// Cannot throw another exception because this is an override method
			throw e;
		}
        return checkSocket(sock);
    }
    
    /**
     * Disable SSL protocols from the socket if SSL protocols are disabled.
     * 
     * @param socket
     * @return
     */
    private Socket checkSocket(Socket socket) {
    	if (disableSSL && socket instanceof SSLSocket) {
    		SSLSocket ssl = (SSLSocket) socket;
    		
    		if (withoutSSLProtocols == null) {
	    		String[] supportedProtocols = ssl.getSupportedProtocols();
	    		List<String> withoutSSLProtocols = new ArrayList<String>(supportedProtocols.length);
	    		
	    		for (String protocol: supportedProtocols) {
	    			if (!protocol.toLowerCase(Locale.getDefault()).startsWith("ssl")) {
	    				withoutSSLProtocols.add(protocol);
	    			}
	    		}
	    		this.withoutSSLProtocols = withoutSSLProtocols.toArray(new String[withoutSSLProtocols.size()]);
    		}
    		
    		ssl.setEnabledProtocols(withoutSSLProtocols);
    	}
    	return socket;
    }
}