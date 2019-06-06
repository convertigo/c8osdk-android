package com.convertigo.clientsdkjunit;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.convertigo.clientsdk.C8o;
import com.convertigo.clientsdk.C8oExceptionMessage;
import com.convertigo.clientsdk.C8oFileTransfer;
import com.convertigo.clientsdk.C8oFileTransferSettings;
import com.convertigo.clientsdk.C8oFileTransferStatus;
import com.convertigo.clientsdk.C8oFullSyncChangeListener;
import com.convertigo.clientsdk.C8oLocalCache;
import com.convertigo.clientsdk.C8oOnFail;
import com.convertigo.clientsdk.C8oOnProgress;
import com.convertigo.clientsdk.C8oOnResponse;
import com.convertigo.clientsdk.C8oProgress;
import com.convertigo.clientsdk.C8oPromise;
import com.convertigo.clientsdk.C8oSettings;
import com.convertigo.clientsdk.C8oUtils;
import com.convertigo.clientsdk.EventHandler;
import com.convertigo.clientsdk.exception.C8oCouchbaseLiteException;
import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oRessourceNotFoundException;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith(AndroidJUnit4.class)
public class ApplicationTest extends ActivityInstrumentationTestCase2<MainActivity> {

    static final String HOST = "c8o-dev.convertigo.net";
    static final String PORT = "80";
    static final String PROJECT_PATH = "/cems/projects/ClientSDKtesting";


  /*  static final String HOST = "192.168.100.230";
    static final String PORT = "18080";
    static final String PROJECT_PATH = "/convertigo/projects/ClientSDKtesting";
*/


    static final XPath xpath = XPathFactory.newInstance().newXPath();

    static Context context;

    enum Stuff {
        C8O {
            @Override
            Object get() throws Throwable {
                C8o c8o = new C8o(context, "http://" + HOST + ":" + PORT  + PROJECT_PATH);
                c8o.setLogRemote(false);
                c8o.setLogLevelLocal(Log.ERROR);
                return c8o;
            }
        },
        C8O_BIS {
            @Override
            Object get() throws Throwable {
                return C8O.get();
            }
        },
        C8O_FS {
            @Override
            Object get() throws Throwable {
                C8o c8o = new C8o(context, "http://" + HOST + ":" + PORT  + PROJECT_PATH, new C8oSettings()
                    .setDefaultDatabaseName("clientsdktesting")
                );
                c8o.setLogRemote(false);
                c8o.setLogLevelLocal(Log.ERROR);
                return c8o;
            }
        },
        C8O_FS_PULL {
            @Override
            Object get() throws Throwable {
                C8o c8o = new C8o(context, "http://" + HOST + ":" + PORT   + PROJECT_PATH, new C8oSettings()
                        .setDefaultDatabaseName("qa_fs_pull")
                );
                c8o.setLogRemote(false);
                c8o.setLogLevelLocal(Log.ERROR);
                JSONObject json = c8o.callJson(".InitFsPull").sync();
                assertTrue(json.getJSONObject("document").getBoolean("ok"));
                return c8o;
            }
        },
        C8O_FS_PUSH {
            @Override
            Object get() throws Throwable {
                C8o c8o = new C8o(context, "http://" + HOST + ":" + PORT  + PROJECT_PATH, new C8oSettings()
                        .setDefaultDatabaseName("qa_fs_push")
                );
                c8o.setLogRemote(false);
                c8o.setLogLevelLocal(Log.ERROR);
                JSONObject json = c8o.callJson(".InitFsPush").sync();
                assertTrue(json.getJSONObject("document").getBoolean("ok"));
                return c8o;
            }
        },
        C8O_LC {
            @Override
            Object get() throws Throwable {
                C8o c8o = new C8o(context, "http://" + HOST + ":" + PORT  + PROJECT_PATH);
                c8o.setLogRemote(false);
                c8o.setLogLevelLocal(Log.ERROR);
                return c8o;
            }
        },
        SetGetInSession {
            @Override
            Object get() throws Throwable {
                C8o c8o = get(C8O_BIS);
                String ts = "" + System.currentTimeMillis();
                Document doc = c8o.callXml(".SetInSession", "ts", ts).sync();
                String newTs = xpath.evaluate("/document/pong/ts/text()", doc);
                assertEquals(ts, newTs);
                doc = c8o.callXml(".GetFromSession").sync();
                newTs = xpath.evaluate("/document/session/expression/text()", doc);
                assertEquals(ts, newTs);
                return new Object();
            }
        };

        static Map<Stuff, Object> stuffs = Collections.synchronizedMap(new HashMap<Stuff, Object>());

        abstract Object get() throws Throwable;

        static public <T> T get(Stuff stuff) throws Throwable {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (stuff) {
                Object res = stuffs.get(stuff);
                if (res == null) {
                    try {
                        res = stuff.get();
                    } catch (Throwable e) {
                        res = e;
                    }
                    stuffs.put(stuff, res);
                }
                if (res instanceof Throwable) {
                    throw (Throwable) res;
                }

                @SuppressWarnings("unchecked")
                T t = (T) res;

                return t;
            }
        }

    }

    public ApplicationTest() {
        super(MainActivity.class);
    }

    public <T> T get(Stuff stuff) throws Throwable {
        return Stuff.get(stuff);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        context = getActivity().getApplicationContext();
    }

    @Test
    public void testWTF() throws Exception {
        assertTrue(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void C8oBadEndpoint() throws C8oException {
        new C8o(context, "http://" + HOST + ":" + PORT);
    }

    @Test
    public void C8oDefault() throws Throwable {
        get(Stuff.C8O);
    }

    @Test
    public void C8oDefaultPing() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        Document doc = c8o.callXml(".Ping").sync();
        Element pong = (Element) xpath.evaluate("/document/pong", doc, XPathConstants.NODE);
        assertNotNull(pong);
    }


    @Test
    public void C8oDefaultPingWait() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        C8oPromise<Document> promise = c8o.callXml(".Ping");
        Thread.sleep(500);
        Document doc = promise.sync();
        Element pong = (Element) xpath.evaluate("/document/pong", doc, XPathConstants.NODE);
        assertNotNull(pong);
    }

    @Test
    public void C8oCallInAsyncTask() throws Throwable {
        final Document[] doc = {null};
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    C8o c8o = ApplicationTest.this.get(Stuff.C8O);
                    doc[0] = c8o.callXml(".Ping").sync();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
                return null;
            }
        }.execute().get();
        Element pong = (Element) xpath.evaluate("/document/pong", doc[0], XPathConstants.NODE);
        assertNotNull(pong);
    }

    @Test
    public void C8oUnknownHostCallAndLog() throws Throwable {
        Throwable exception = null;
        final Throwable[] exceptionLog = {null};
        C8o c8o = new C8o(context, "http://" + HOST + "ee:" + PORT   + PROJECT_PATH, new C8oSettings()
            .setLogOnFail(new C8oOnFail() {
                @Override
                public void run(Throwable throwable, Map<String, Object> parameters) {
                    exceptionLog[0] = throwable;
                }
            })
        );
        c8o.log.warn("must fail log");
        Thread.sleep(250);
        try {
           c8o.callXml(".Ping").sync();
        } catch (Exception ex) {
            exception = ex;
        }
        assertNotNull(exception);
//        assertEquals(C8oException.class, exception.getClass());
//        exception = exception.getCause();
        assertEquals(com.convertigo.clientsdk.exception.C8oHttpRequestException.class, exception.getClass());
        exception = exception.getCause();
        assertEquals(java.net.UnknownHostException.class, exception.getClass());
        assertNotNull(exceptionLog[0]);
//        assertEquals(C8oException.class, exceptionLog[0].getClass());
//        exceptionLog[0] = exceptionLog[0].getCause();
        assertEquals(com.convertigo.clientsdk.exception.C8oHttpRequestException.class, exceptionLog[0].getClass());
        exceptionLog[0] = exceptionLog[0].getCause();
        assertEquals(java.net.UnknownHostException.class, exceptionLog[0].getClass());
    }

    @Test
    public void C8oUnknownHostCallWait() throws Throwable {
        Throwable exception = null;
        C8o c8o = new C8o(context, "http://" + HOST + "ee:" + PORT   + PROJECT_PATH);
        try {
            C8oPromise<Document> promise = c8o.callXml(".Ping");
            Thread.sleep(500);
            promise.sync();
        } catch (Exception ex) {
            exception = ex;
        }
        assertNotNull(exception);
//        assertEquals(C8oException.class, exception.getClass());
//        exception = exception.getCause();
        assertEquals(com.convertigo.clientsdk.exception.C8oHttpRequestException.class, exception.getClass());
        exception = exception.getCause();
        assertEquals(java.net.UnknownHostException.class, exception.getClass());
    }

    @Test
    public void C8oDefaultPingOneSingleValue() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        Document doc = c8o.callXml(".Ping", "var1", "value one").sync();
        String value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals("value one", value);
    }

    @Test
    public void C8oDefaultPingOneSingleValueNull() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        JSONObject json = c8o.callJson(".Ping",
                "var1", null, "var2", "mavar2"
        ).sync();
        json = json.getJSONObject("document");
        JSONObject pong = json.getJSONObject("pong");
        try{
            Object value = pong.getString("var1");
        }
        catch(Exception e)
        {
            assertEquals(e != null, true);
        }
        Object value = pong.getString("var2");
        assertEquals( value, "mavar2");
    }

    @Test
    public void C8oDefaultPingOneSingleValueUTF8() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        Document doc = c8o.callXml(".Ping", "var1", "é@à &%µ").sync();
        String value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals("é@à &%µ", value);
    }

    @Test
    public void C8oDefaultPingTwoSingleValues() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        Document doc = c8o.callXml(".Ping",
                "var1", "value one",
                "var2", "value two"
        ).sync();
        String value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals("value one", value);
        value = xpath.evaluate("/document/pong/var2/text()", doc);
        assertEquals("value two", value);
    }

    @Test
    public void C8oDefaultPingTwoSingleValuesOneMulti() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        Document doc = c8o.callXml(".Ping",
                "var1", "value one",
                "var2", "value two",
                "mvar1", new String[]{"mvalue one", "mvalue two", "mvalue three"}
        ).sync();
        Object value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals("value one", value);
        value = xpath.evaluate("/document/pong/var2/text()", doc);
        assertEquals("value two", value);
        value = xpath.evaluate("/document/pong/mvar1[1]/text()", doc);
        assertEquals("mvalue one", value);
        value = xpath.evaluate("/document/pong/mvar1[2]/text()", doc);
        assertEquals("mvalue two", value);
        value = xpath.evaluate("/document/pong/mvar1[3]/text()", doc);
        assertEquals("mvalue three", value);
        value = xpath.evaluate("count(/document/pong/mvar1)", doc, XPathConstants.NUMBER);
        assertEquals(3.0, value);
    }

    @Test
    public void C8oDefaultPingTwoSingleValuesTwoMulti() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        Document doc = c8o.callXml(".Ping",
                "var1", "value one",
                "var2", "value two",
                "mvar1", new String[]{"mvalue one", "mvalue two", "mvalue three"},
                "mvar2", new String[]{"mvalue2 one"}
        ).sync();
        Object value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals("value one", value);
        value = xpath.evaluate("/document/pong/var2/text()", doc);
        assertEquals("value two", value);
        value = xpath.evaluate("/document/pong/mvar1[1]/text()", doc);
        assertEquals("mvalue one", value);
        value = xpath.evaluate("/document/pong/mvar1[2]/text()", doc);
        assertEquals("mvalue two", value);
        value = xpath.evaluate("/document/pong/mvar1[3]/text()", doc);
        assertEquals("mvalue three", value);
        value = xpath.evaluate("count(/document/pong/mvar1)", doc, XPathConstants.NUMBER);
        assertEquals(3.0, value);
        value = xpath.evaluate("/document/pong/mvar2[1]/text()", doc);
        assertEquals("mvalue2 one", value);
        value = xpath.evaluate("count(/document/pong/mvar2)", doc, XPathConstants.NUMBER);
        assertEquals(1.0, value);
    }

    @Test
    public void C8oCheckJsonTypes() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        JSONObject json = c8o.callJson(".JsonTypes",
                "var1", "value one",
                "mvar1", new String[]{"mvalue one", "mvalue two", "mvalue three"}
        ).sync();
        json = json.getJSONObject("document");
        JSONObject pong = json.getJSONObject("pong");
        Object value = pong.getString("var1");
        assertEquals("value one", value);
        JSONArray mvar1 = pong.getJSONArray("mvar1");
        value = mvar1.getString(0);
        assertEquals("mvalue one", value);
        value = mvar1.getString(1);
        assertEquals("mvalue two", value);
        value = mvar1.getString(2);
        assertEquals("mvalue three", value);
        value = mvar1.length();
        assertEquals(3, value);
        JSONObject complex = json.getJSONObject("complex");
        boolean isBool = complex.isNull("isNull");
        assertTrue(isBool);
        value = complex.getInt("isInt3615");
        assertEquals(3615, value);
        value = complex.getString("isStringWhere");
        assertEquals("where is my string?!", value);
        value = complex.getDouble("isDoublePI");
        assertEquals(3.141592653589793, value);
        isBool = complex.getBoolean("isBoolTrue");
        assertTrue(isBool);
        value = complex.getString("ÉlŸz@-node");
        assertEquals("that's ÉlŸz@", value);
    }

    @Test
    public void C8oExceptionMessages() throws Throwable {
        C8oRessourceNotFoundException c8oRes = new C8oRessourceNotFoundException("a", new Throwable("abc"));
        C8oExceptionMessage exceptionMsg = new C8oExceptionMessage();
        C8oUtils c8oUtils = new C8oUtils();

        assertEquals(exceptionMsg.notImplementedFullSyncInterface(), "You are using the default FullSyncInterface which is not implemented");
        assertEquals(exceptionMsg.invalidParameterValue("params", "details"),"The parameter 'params' is invalid, details");
        assertEquals(exceptionMsg.missingValue("val"), "The val is missing");
        assertEquals(exceptionMsg.unknownValue("valName", "val"), "The valName value val is unknown");
        assertEquals(exceptionMsg.unknownType("valName", "val"),"The valName type " + c8oUtils.getObjectClassName("val") + "is unknown");
        assertEquals(exceptionMsg.ressourceNotFound("ress"), "The ress was not found");
        assertEquals(exceptionMsg.illegalArgumentInvalidFullSyncDatabaseUrl("http://fakeurl.com"), "The fullSync database url 'http://fakeurl.com' is not a valid url");
        assertEquals(exceptionMsg.fullSyncDatabaseInitFailed("dbname"), "Failed to initialize the FullSync database 'dbname'");
        assertEquals(exceptionMsg.illegalArgumentMissParameter("paramName"), "The parameter 'paramName' is missing");
        assertEquals(exceptionMsg.illegalArgumentInvalidURL("http://fakeurl.com"), "'http://fakeurl.com' is not a valid URL");
        assertEquals(exceptionMsg.unknownFullSyncPolicy("POLICY"), "Unknown the FullSync policy 'POLICY'");
        assertEquals(exceptionMsg.illegalArgumentInvalidEndpoint("endpoint"), "'endpoint' is not a valid Convertigo endpoint");
        assertEquals(exceptionMsg.illegalArgumentInvalidRequestable("endpoint"), "'endpoint' is not a valid requestable.");
        assertEquals(exceptionMsg.illegalArgumentInvalidParameterType("a", "b", "c"), "The parameter 'a' must be of type 'b' and not 'c'");
        assertEquals(exceptionMsg.illegalArgumentIncompatibleListener("a", "b"), "The listener type 'a' is incompatible with the response type 'b'");
        assertEquals(exceptionMsg.illegalArgumentNullParameter("a"), "a must be not null");
        assertEquals(exceptionMsg.initError(), "Unable to initialize");
        assertEquals(exceptionMsg.initRsainternalKey(), "Unable to initialize the RSA internal key");
        assertEquals(exceptionMsg.initCouchManager(), "Unable to initialize the fullSync databases manager");
        assertEquals(exceptionMsg.initSslSocketFactory(), "Unable to initialize the ssl socket factory");
        assertEquals(exceptionMsg.initDocumentBuilder(), "Unable to initialize the XML document builder");
        assertEquals(exceptionMsg.parseStreamToJson(), "Unable to parse the input stream to a json document");
        assertEquals(exceptionMsg.parseStreamToXml(), "Unable to parse the input stream to an xml document");
        assertEquals(exceptionMsg.parseInputStreamToString(), "Unable to parse the input stream to a string");
        assertEquals(exceptionMsg.parseXmlToString(),"Unable to parse the xml document to a string");
        assertEquals(exceptionMsg.parseRsainternalKey(), "Unable to parse the RSA internal key");
        assertEquals(exceptionMsg.parseQueryEnumeratorToJson(), "Unable to parse the query to a json document");
        assertEquals(exceptionMsg.parseLogsToJson(), "Unable to parse logs to a json document");
        assertEquals(exceptionMsg.parseLogsStreamToJson(), "Unable to parse stream containing logs response to a json document");
        assertEquals(exceptionMsg.parseC8oReplicationResultToJson(), "Unable to parse the replication result to a json document");
        assertEquals(exceptionMsg.parseFullSyncDefaultResponseToJson(), "Unable to parse the default fullSync result to a json document");
        assertEquals(exceptionMsg.parseFullSyncPostDocumentResponseToJson(), "Unable to parse the post document fullSync result to a json document");
        assertEquals(exceptionMsg.parseStringToJson(), "Unable to parse the string to a JSON document");
        assertEquals(exceptionMsg.parseStringToObject("a"), "Unable to parse the string (JSON):string to an object of type a");
        assertEquals(exceptionMsg.stringToJson("a"), "Unable to deserialize the JSON object from the following String : 'a'");
        assertEquals(exceptionMsg.retrieveRsainternalKey(), "Error during http request to get the RSA internal key");
        assertEquals(exceptionMsg.httpLogs(), "Error during http request to send logs to the Convertigo server");
        assertEquals(exceptionMsg.couchRequestGetView(), "Unable to run the view query");
        assertEquals(exceptionMsg.couchRequestAllDocuments(), "Unable to run the all query");
        assertEquals(exceptionMsg.couchRequestResetDatabase(), "Unable to run the reset query");
        assertEquals(exceptionMsg.couchRequestInvalidRevision(), "The revision is invalid");
        assertEquals(exceptionMsg.couchRequestPostDocument(), "Unable to run the post document query");
        assertEquals(exceptionMsg.unableToGetFullSyncDatabase("a"), "Unable to get the fullSync database 'a' from the manager");
        assertEquals(exceptionMsg.couchNullResult(), "An error occured during the fullSync request, its result is null");
        assertEquals(exceptionMsg.couchFullSyncNotActive(), "Unable to use fullSync because it was not activated at the initialization");
        exceptionMsg.couchDeleteFailed();
        exceptionMsg.fullSyncGetOrCreateDatabase("db");
        exceptionMsg.fullSyncHandleResponse();
        exceptionMsg.loadKeyStore();
        exceptionMsg.trustAllCertificates();
        exceptionMsg.serverKeyStore();
        exceptionMsg.illegalArgumentNotFoundFullSyncView("a", "b");
        exceptionMsg.unhandledResponseType("a");
        exceptionMsg.unhandledListenerType("a");
        exceptionMsg.unhandledFullSyncRequestable("a");
        exceptionMsg.wrongResult("a");
        exceptionMsg.closeInputStream();
        exceptionMsg.deserializeJsonObjectFromString("a");
        exceptionMsg.postDocument();
        exceptionMsg.getNameValuePairObjectValue("a");
        exceptionMsg.queryEnumeratorToJSON();
        exceptionMsg.queryEnumeratorToXML();
        exceptionMsg.addparametersToQuery();
        exceptionMsg.putJson();
        exceptionMsg.changeEventToJson();
        exceptionMsg.initC8oSslSocketFactory();
        exceptionMsg.createSslContext();
        exceptionMsg.keyManagerFactoryInstance();
        exceptionMsg.initKeyManagerFactory();
        exceptionMsg.initHttpInterface();
        exceptionMsg.trustManagerFactoryInstance();
        exceptionMsg.initSslContext();
        exceptionMsg.initCipher();
        exceptionMsg.urlEncode();
        exceptionMsg.getParametersStringBytes();
        exceptionMsg.encodeParameters();
        exceptionMsg.runHttpRequest();
        exceptionMsg.generateRsainternalKey();
        exceptionMsg.keyFactoryInstance();
        exceptionMsg.getCipherInstance();
        exceptionMsg.entryNotFound("a");
        exceptionMsg.c8oCallRequestToJson();
        exceptionMsg.getJsonKey("a");
        exceptionMsg.jsonValueToXML();
        exceptionMsg.inputStreamToXML();
        exceptionMsg.inputStreamReaderEncoding();
        exceptionMsg.readLineFromBufferReader();
        exceptionMsg.getLocalCacheParameters();
        exceptionMsg.getLocalCachePolicy("a");
        exceptionMsg.fullSyncJsonToXML();
        exceptionMsg.takeLog();
        exceptionMsg.remoteLogHttpRequest();
        exceptionMsg.getInputStreamFromHttpResponse();
        exceptionMsg.inputStreamToJSON();
        exceptionMsg.httpInterfaceInstance();
        exceptionMsg.fullSyncInterfaceInstance();
        exceptionMsg.getDocumentFromDatabase("a");
        exceptionMsg.fullSyncReplicationFail("a", "b");
        exceptionMsg.localCachePolicyIsDisable();
        exceptionMsg.illegalArgumentInvalidLocalCachePolicy("a");
        exceptionMsg.timeToLiveExpired();
        exceptionMsg.invalidLocalCacheResponseInformation();
        exceptionMsg.overrideDocument();
        exceptionMsg.handleFullSyncRequest();
        exceptionMsg.serializeC8oCallRequest();
        exceptionMsg.getResponseFromLocalCache();
        exceptionMsg.getResponseFromLocalCacheDocument();
        exceptionMsg.saveResponseToLocalCache();
        exceptionMsg.missingLocalCacheResponseDocument();
        exceptionMsg.clientKeyStore();
    }

    @Test
    public void SetGetInSession() throws Throwable {
        get(Stuff.SetGetInSession);
    }

    @Test
    public void CheckNoMixSession() throws Throwable {
        get(Stuff.SetGetInSession);
        C8o c8o = get(Stuff.C8O);
        Document doc = c8o.callXml(".GetFromSession").sync();
        Object expression = xpath.evaluate("/document/session/expression", doc, XPathConstants.NODE);
        assertNull(expression);
    }

    @Test
    public void CheckParams() throws Throwable {
        C8oSettings settings = new C8oSettings();
        settings.setLogRemote(false);
        settings.setLogLevelLocal(Log.ERROR);
        try {
            C8o c8o = new C8o(context, "https://" + HOST + ":" + PORT  + PROJECT_PATH, settings);
            c8o.setLogC8o(true);
            assertEquals(true, c8o.isLogC8o());
            c8o.setLogLevelLocal(Log.ERROR);
            assertEquals(c8o.getLogLevelLocal(), Log.ERROR);
            assertEquals(c8o.getEndpoint(), "https://" + HOST + ":" + PORT  + PROJECT_PATH);
            assertEquals(true, c8o.getEndpointIsSecure());
            assertEquals(HOST, c8o.getEndpointHost());
            assertEquals(":"+PORT, c8o.getEndpointPort());
            try {
                JSONObject json = c8o.callJson(null).sync();
                Assert.fail("it is not supposed to happend");
            } catch (Exception e) {
                C8oExceptionMessage exceptionMsg = new C8oExceptionMessage();
                assertEquals(exceptionMsg.illegalArgumentNullParameter("requestable"), e.getMessage());
                Log.d("CheckParams requestable", e.getMessage());
            }
            settings.setTimeout(200);
            settings.setTimeout(0);
            settings.setTrustAllCertificates(true);
            settings.addCookie("myCookie", "aRandomCookie");
            settings.setAuthenticationCookieValue("authenticationCookieValue");
            settings.setFullSyncServerUrl("fullSyncServerUrl");
            settings.setFullSyncUsername("fullSyncUsername");
            settings.setFullSyncPassword("fullSyncPassword");
            settings.setFullSyncLocalSuffix("fullSyncLocalSuffix");
            settings.setUseEncryption(true);
            C8o c8ob = new C8o(context, "http://" + HOST + ":" + PORT  + PROJECT_PATH, settings);
            try {
                assertEquals(c8ob.getFullSyncServerUrl(), "fullSyncServerUrl");
                assertEquals(c8ob.getFullSyncUsername(), "fullSyncUsername");
                assertEquals(c8ob.getFullSyncPassword(), "fullSyncPassword");

//                c8ob.setEndpoint("htdrdr:fake.com");
                C8o c8oc = new C8o(context, "htdrdr:fake.com");
                Assert.fail("Endpoint should not be ok");
            } catch (Exception e) {
                C8oExceptionMessage exceptionMsg = new C8oExceptionMessage();
                assertEquals(exceptionMsg.illegalArgumentInvalidURL("htdrdr:fake.com"), e.getMessage());

                C8oProgress progress = new C8oProgress();
                progress.setRaw("justanexample");
                progress.setChanged(true);
                Log.d("checkparams progress changed", Boolean.toString(progress.isChanged()));
                C8oProgress progress2 = new C8oProgress(progress);
                assertEquals(progress2.getRaw(), progress.getRaw());
                assertEquals(progress2.isChanged(), progress.isChanged());
                try {
                    C8oProgress progressb = new C8oProgress(null);
                    Assert.fail("C8oProgress cannot be null");
                } catch (Exception ex) {
                    Assert.assertNotNull(ex.getMessage(), ex);
                }

            }


        } catch (Exception e) {
            Log.d("CheckParams", e.getMessage());
        }

    }

    public void CheckLogRemoteHelper(C8o c8o, String lvl, String msg) throws Throwable {
        Thread.sleep(333);
        Document doc = c8o.callXml(".GetLogs").sync();
        String sLine = xpath.evaluate("/document/line/text()", doc);
        assertTrue("[" + lvl + "] sLine='" + sLine +"'", sLine != null && !sLine.isEmpty());
        JSONArray line = new JSONArray(sLine);
        assertEquals(lvl, line.getString(2));
        String newMsg = line.getString(4);
        newMsg = newMsg.substring(newMsg.indexOf("logID="));
        assertEquals(msg, newMsg);
    }

    @Test
    public void CheckLogRemote() throws Throwable {
        C8o c8o = new C8o(context, "http://" + HOST + ":" + PORT  + PROJECT_PATH);
        c8o.setLogC8o(false);
        String id = "logID=" + System.currentTimeMillis();
        c8o.callXml(".GetLogs", "init", id).sync();
        Thread.sleep(333);
        c8o.log.error(id);
        CheckLogRemoteHelper(c8o, "ERROR", id);
        c8o.log.error(id, new C8oException("for test"));
        CheckLogRemoteHelper(c8o, "ERROR", id + "\ncom.convertigo.clientsdk.exception.C8oException: for test");
        c8o.log.warn(id);
        CheckLogRemoteHelper(c8o, "WARN", id);
        c8o.log.info(id);
        CheckLogRemoteHelper(c8o, "INFO", id);
        c8o.log.debug(id);
        CheckLogRemoteHelper(c8o, "DEBUG", id);
        c8o.log.trace(id);
        CheckLogRemoteHelper(c8o, "TRACE", id);
        c8o.log.fatal(id);
        CheckLogRemoteHelper(c8o, "FATAL", id);
        c8o.setLogRemote(false);
        c8o.log.info(id);
        Thread.sleep(333);
        Document doc = c8o.callXml(".GetLogs").sync();
        Object value = xpath.evaluate("/document/line", doc, XPathConstants.NODE);
        assertNull(value);
    }

    @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter"})
    @Test
    public void C8oDefaultPromiseXmlOne() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        final Document[] xdoc = new Document[1];
        final Thread[] xthread = new Thread[1];
        final Map<String, Object>[] xparam = new Map[1];

        synchronized (xdoc) {
            c8o.callXml(".Ping", "var1", "step 1").then(new C8oOnResponse<Document>() {
                @Override
                public C8oPromise<Document> run(Document doc, Map<String, Object> param) throws Throwable {
                    xdoc[0] = doc;
                    xthread[0] = Thread.currentThread();
                    xparam[0] = param;

                    synchronized (xdoc) {
                        xdoc.notify();
                    }
                    return null;
                }
            });
            xdoc.wait(5000);
        }

        Object value = xpath.evaluate("/document/pong/var1/text()", xdoc[0]);
        assertEquals("step 1", value);
        assertNotSame(Thread.currentThread(), xthread[0]);
        assertEquals("step 1", xparam[0].get("var1"));
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oDefaultPromiseJsonThree() throws Throwable {
        final C8o c8o = get(Stuff.C8O);
        final JSONObject[] xjson = new JSONObject[3];

        synchronized (xjson) {
            c8o.callJson(".Ping", "var1", "step 1").then(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[0] = json;
                    return c8o.callJson(".Ping", "var1", "step 2");
                }
            }).then(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[1] = json;
                    return c8o.callJson(".Ping", "var1", "step 3");
                }
            }).then(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[2] = json;
                    synchronized (xjson) {
                        xjson.notify();
                    }
                    return null;
                }
            });
            xjson.wait(5000);
        }

        Object value = xjson[0].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 1", value);
        value = xjson[1].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 2", value);
        value = xjson[2].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 3", value);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oDefaultPromiseUI() throws Throwable {
        final C8o c8o = get(Stuff.C8O);
        final JSONObject[] xjson = new JSONObject[3];
        final Looper[] xthread = new Looper[3];

        synchronized (xjson) {
            c8o.callJson(".Ping", "var1", "step 1").thenUI(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[0] = json;
                    xthread[0] = Looper.myLooper();
                    return c8o.callJson(".Ping", "var1", "step 2");
                }
            }).then(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[1] = json;
                    xthread[1] = Looper.myLooper();
                    return c8o.callJson(".Ping", "var1", "step 3");
                }
            }).thenUI(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[2] = json;
                    xthread[2] = Looper.myLooper();
                    synchronized (xjson) {
                        xjson.notify();
                    }
                    return null;
                }
            });
            xjson.wait(5000);
        }

        Object value = xjson[0].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 1", value);
        value = xjson[1].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 2", value);
        value = xjson[2].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 3", value);
        assertEquals(Looper.getMainLooper(), xthread[0]);
        assertNotSame(Looper.getMainLooper(), xthread[1]);
        assertEquals(Looper.getMainLooper(), xthread[2]);
    }

    @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter"})
    @Test
    public void C8oDefaultPromiseFail() throws Throwable {
        final C8o c8o = get(Stuff.C8O);
        final JSONObject[] xjson = new JSONObject[3];
        final Throwable[] xfail = new Throwable[3];
        final Map<String, Object>[] xparam = new Map[1];

        synchronized (xjson) {
            c8o.callJson(".Ping", "var1", "step 1").then(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[0] = json;
                    return c8o.callJson(".Ping", "var1", "step 2");
                }
            }).then(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[1] = json;
                    if (json != null) {
                        throw new C8oException("random failure");
                    }
                    return c8o.callJson(".Ping", "var1", "step 3");
                }
            }).then(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[2] = json;
                    synchronized (xjson) {
                        xjson.notify();
                    }
                    return null;
                }
            }).fail(new C8oOnFail() {
                @Override
                public void run(Throwable throwable, Map<String, Object> param) {
                    xfail[0] = throwable;
                    xparam[0] = param;
                    synchronized (xjson) {
                        xjson.notify();
                    }
                }
            });
            xjson.wait(5000);
        }

        Object value = xjson[0].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 1", value);
        value = xjson[1].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 2", value);
        assertNull(xjson[2]);
        assertEquals("random failure", xfail[0].getMessage());
        assertEquals("step 2", xparam[0].get("var1"));
    }

    @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter"})
    @Test
    public void C8oDefaultPromiseFailUI() throws Throwable {
        final C8o c8o = get(Stuff.C8O);
        final JSONObject[] xjson = new JSONObject[3];
        final Throwable[] xfail = new Throwable[1];
        final Map<String, Object>[] xparam = new Map[1];
        final Looper[] xthread = new Looper[1];

        synchronized (xjson) {
            c8o.callJson(".Ping", "var1", "step 1").then(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[0] = json;
                    return c8o.callJson(".Ping", "var1", "step 2");
                }
            }).then(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[1] = json;
                    if (json != null) {
                        throw new C8oException("random failure");
                    }
                    return c8o.callJson(".Ping", "var1", "step 3");
                }
            }).then(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[2] = json;
                    synchronized (xjson) {
                        xjson.notify();
                    }
                    return null;
                }
            }).failUI(new C8oOnFail() {
                @Override
                public void run(Throwable throwable, Map<String, Object> param) {
                    xfail[0] = throwable;
                    xparam[0] = param;
                    xthread[0] = Looper.myLooper();
                    synchronized (xjson) {
                        xjson.notify();
                    }
                }
            });
            xjson.wait(5000);
        }

        Object value = xjson[0].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 1", value);
        value = xjson[1].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 2", value);
        assertNull(xjson[2]);
        assertEquals("random failure", xfail[0].getMessage());
        assertEquals("step 2", xparam[0].get("var1"));
        assertEquals(Looper.getMainLooper(), xthread[0]);
    }

    @Test
    public void C8oDefaultPromiseSync() throws Throwable {
        final C8o c8o = get(Stuff.C8O);
        final JSONObject[] xjson = new JSONObject[2];
        xjson[1] = c8o.callJson(".Ping", "var1", "step 1").then(new C8oOnResponse<JSONObject>() {
            @Override
            public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                xjson[0] = json;
                return c8o.callJson(".Ping", "var1", "step 2");
            }
        }).sync();
        Object value = xjson[0].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 1", value);
        value = xjson[1].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 2", value);
    }

    @Test
    public void C8oDefaultPromiseSyncFail() throws Throwable {
        final C8o c8o = get(Stuff.C8O);
        final JSONObject[] xjson = new JSONObject[2];
        Exception exception = null;
        try {
            xjson[1] = c8o.callJson(".Ping", "var1", "step 1").then(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[0] = json;
                    if (json != null) {
                        throw new C8oException("random failure");
                    }
                    return c8o.callJson(".Ping", "var1", "step 2");
                }
            }).sync();
        } catch (Exception ex) {
            exception = ex;
        }
        Object value = xjson[0].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 1", value);
        assertNull(xjson[1]);
        assertNotNull(exception);
        assertEquals("random failure", exception.getMessage());
    }

    @Test
    public void C8oDefaultPromiseNested() throws Throwable {
        final C8o c8o = get(Stuff.C8O);
        final JSONObject[] xjson = new JSONObject[6];
        xjson[5] = c8o.callJson(".Ping", "var1", "step 1").then(new C8oOnResponse<JSONObject>() {
            @Override
            public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                xjson[0] = json;
                return c8o.callJson(".Ping", "var1", "step 2").then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject json2, Map<String, Object> param) throws Throwable {
                        xjson[1] = json2;
                        return c8o.callJson(".Ping", "var1", "step 3").then(new C8oOnResponse<JSONObject>() {
                            @Override
                            public C8oPromise<JSONObject> run(JSONObject json3, Map<String, Object> param) throws Throwable {
                                xjson[2] = json3;
                                return c8o.callJson(".Ping", "var1", "step 4");
                            }
                        });
                    }
                });
            }
        }).then(new C8oOnResponse<JSONObject>() {
            @Override
            public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                xjson[3] = json;
                return c8o.callJson(".Ping", "var1", "step 5").then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject json2, Map<String, Object> param) throws Throwable {
                        xjson[4] = json2;
                        return null;
                    }
                });
            }
        }).sync();
        Object value = xjson[0].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 1", value);
        value = xjson[1].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 2", value);
        value = xjson[2].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 3", value);
        value = xjson[3].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 4", value);
        value = xjson[4].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 5", value);
        value = xjson[5].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 5", value);
    }

    @Test
    public void C8oDefaultPromiseNestedFail() throws Throwable {
        final C8o c8o = get(Stuff.C8O);
        final JSONObject[] xjson = new JSONObject[6];
        final Throwable[] xfail = new Throwable[2];
        try {
            xjson[5] = c8o.callJson(".Ping", "var1", "step 1").then(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[0] = json;
                    return c8o.callJson(".Ping", "var1", "step 2").then(new C8oOnResponse<JSONObject>() {
                        @Override
                        public C8oPromise<JSONObject> run(JSONObject json2, Map<String, Object> param) throws Throwable {
                            xjson[1] = json2;
                            return c8o.callJson(".Ping", "var1", "step 3").then(new C8oOnResponse<JSONObject>() {
                                @Override
                                public C8oPromise<JSONObject> run(JSONObject json3, Map<String, Object> param) throws Throwable {
                                    xjson[2] = json3;
                                    throw new C8oException("random failure");
                                }
                            });
                        }
                    });
                }
            }).then(new C8oOnResponse<JSONObject>() {
                @Override
                public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                    xjson[3] = json;
                    return c8o.callJson(".Ping", "var1", "step 5").then(new C8oOnResponse<JSONObject>() {
                        @Override
                        public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                            xjson[0] = json;
                            return null;
                        }
                    });
                }
            }).fail(new C8oOnFail() {
                @Override
                public void run(Throwable throwable, Map<String, Object> parameters) {
                    xfail[0] = throwable;
                }
            }).sync();
        } catch (Throwable t) {
            xfail[1] = t;
        }
        Object value = xjson[0].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 1", value);
        value = xjson[1].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 2", value);
        value = xjson[2].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 3", value);
        value = xjson[3];
        assertNull(value);
        value = xjson[4];
        assertNull(value);
        value = xjson[5];
        assertNull(value);
        assertEquals("random failure", xfail[0].getMessage());
        assertEquals(xfail[0], xfail[1]);
    }

    @Test
    public void C8oDefaultPromiseInVar() throws Throwable {
        final C8o c8o = get(Stuff.C8O);
        final JSONObject[] xjson = new JSONObject[3];
        C8oPromise<JSONObject> promise = c8o.callJson(".Ping", "var1", "step 1");
        promise.then(new C8oOnResponse<JSONObject>() {
            @Override
            public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                xjson[0] = json;
                return c8o.callJson(".Ping", "var1", "step 2");
            }
        });
        promise.then(new C8oOnResponse<JSONObject>() {
            @Override
            public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                xjson[1] = json;
                return c8o.callJson(".Ping", "var1", "step 3");
            }
        });
        xjson[2] = promise.sync();
        Object value = xjson[0].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 1", value);
        value = xjson[1].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 2", value);
        value = xjson[2].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 3", value);
    }

    @Test
    public void C8oDefaultPromiseInVarSleep() throws Throwable {
        final C8o c8o = get(Stuff.C8O);
        final JSONObject[] xjson = new JSONObject[3];
        C8oPromise<JSONObject> promise = c8o.callJson(".Ping", "var1", "step 1");
        Thread.sleep(500);
        promise.then(new C8oOnResponse<JSONObject>() {
            @Override
            public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                xjson[0] = json;
                return c8o.callJson(".Ping", "var1", "step 2");
            }
        });
        Thread.sleep(500);
        promise.then(new C8oOnResponse<JSONObject>() {
            @Override
            public C8oPromise<JSONObject> run(JSONObject json, Map<String, Object> param) throws Throwable {
                xjson[1] = json;
                return c8o.callJson(".Ping", "var1", "step 3");
            }
        });
        Thread.sleep(500);
        xjson[2] = promise.sync();
        Object value = xjson[0].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 1", value);
        value = xjson[1].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 2", value);
        value = xjson[2].getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals("step 3", value);
    }

    //@Test
    public void C8o0Ssl1TrustFail() throws Throwable {
        Throwable exception = null;
        try {
            C8o c8o = new C8o(context, "https://" + HOST + ":443" + PROJECT_PATH);
            Document doc = c8o.callXml(".Ping", "var1", "value one").sync();
            String value = xpath.evaluate("/document/pong/var1/text()", doc);
            assertTrue("not possible", false);
        } catch (Exception ex) {
            exception = ex;
        }
        assertNotNull(exception);
        assertEquals(C8oException.class, exception.getClass());
        exception = exception.getCause();
        assertEquals(com.convertigo.clientsdk.exception.C8oHttpRequestException.class, exception.getClass());
        exception = exception.getCause();
        assertEquals(javax.net.ssl.SSLHandshakeException.class, exception.getClass());
        exception = exception.getCause();
        assertEquals(java.security.cert.CertificateException.class, exception.getClass());
        exception = exception.getCause();
        assertEquals(java.security.cert.CertPathValidatorException.class, exception.getClass());
    }

    //@Test
    public void C8oSsl2TrustAll() throws Throwable {
        C8o c8o = new C8o(context, "https://" + HOST + ":443" + PROJECT_PATH, new C8oSettings().setTrustAllCertificates(true));
        Document doc = c8o.callXml(".Ping", "var1", "value one").sync();
        String value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals("value one", value);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsPostGetDelete() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS);
        synchronized (c8o) {
            JSONObject json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            String myId = "C8oFsPostGetDelete-" + System.currentTimeMillis();
            json = c8o.callJson("fs://.post", "_id", myId).sync();
            assertTrue(json.getBoolean("ok"));
            String id = json.getString("id");
            assertEquals(myId, id);
            json = c8o.callJson("fs://.get", "docid", id).sync();
            id = json.getString("_id");
            assertEquals(myId, id);
            json = c8o.callJson("fs://.delete", "docid", id).sync();
            assertTrue(json.getBoolean("ok"));
            try {
                c8o.callJson("fs://.get", "docid", id).sync();
                assertTrue("not possible", false);
            } catch (Exception e) {
                assertEquals(C8oRessourceNotFoundException.class, e.getClass());
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsPostGetDeleteRev() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS);
        synchronized (c8o) {
            JSONObject json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            String id = "C8oFsPostGetDeleteRev-" + System.currentTimeMillis();
            json = c8o.callJson("fs://.post", "_id", id).sync();
            assertTrue(json.getBoolean("ok"));
            String rev = json.getString("rev");
            try {
                c8o.callJson("fs://.delete", "docid", id, "rev", "1-123456").sync();
                assertTrue("not possible", false);
            } catch (Exception e) {
                assertEquals(C8oRessourceNotFoundException.class, e.getClass());
            }
            json = c8o.callJson("fs://.delete", "docid", id, "rev", rev).sync();
            assertTrue(json.getBoolean("ok"));
            try {
                c8o.callJson("fs://.get", "docid", id).sync();
                assertTrue("not possible", false);
            } catch (Exception e) {
                assertEquals(C8oRessourceNotFoundException.class, e.getClass());
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsPostGetDestroyCreate() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS);
        synchronized (c8o) {
            JSONObject json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            String ts = "ts=" + System.currentTimeMillis();
            String ts2 = ts + "@test";
            json = c8o.callJson("fs://.post", "ts", ts).sync();
            assertTrue(json.getBoolean("ok"));
            String id = json.getString("id");
            String rev = json.getString("rev");
            json = c8o.callJson("fs://.post",
                "_id", id,
                "_rev", rev,
                "ts", ts,
                "ts2", ts2
            ).sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://.get", "docid", id).sync();
            assertEquals(ts, json.getString("ts"));
            assertEquals(ts2, json.getString("ts2"));
            json = c8o.callJson("fs://.destroy").sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://.create").sync();
            assertTrue(json.getBoolean("ok"));
            try {
                c8o.callJson("fs://.get", "docid", id).sync();
                assertTrue("not possible", false);
            } catch (Exception e) {
                assertEquals(C8oRessourceNotFoundException.class, e.getClass());
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsPostReset() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS);
        synchronized (c8o) {
            JSONObject json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://.post").sync();
            assertTrue(json.getBoolean("ok"));
            String id = json.getString("id");
            json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            try {
                c8o.callJson("fs://.get", "docid", id).sync();
                assertTrue("not possible", false);
            } catch (Exception e) {
                assertEquals(C8oRessourceNotFoundException.class, e.getClass());
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsPostExisting() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS);
        synchronized (c8o) {
            JSONObject json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://.post").sync();
            assertTrue(json.getBoolean("ok"));
            String id = json.getString("id");
            try {
                c8o.callJson("fs://.post", "_id", id).sync();
                assertTrue("not possible", false);
            } catch (Exception e) {
                assertEquals(C8oCouchbaseLiteException.class, e.getClass());
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsPostExistingPolicyNone() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS);
        synchronized (c8o) {
            JSONObject json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://.post", C8o.FS_POLICY, C8o.FS_POLICY_NONE).sync();
            assertTrue(json.getBoolean("ok"));
            String id = json.getString("id");
            try {
                c8o.callJson("fs://.post",
                    C8o.FS_POLICY, C8o.FS_POLICY_NONE,
                     "_id", id
                ).sync();
                assertTrue("not possible", false);
            } catch (Exception e) {
                assertEquals(C8oCouchbaseLiteException.class, e.getClass());
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsPostExistingPolicyCreate() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS);
        synchronized (c8o) {
            JSONObject json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            String myId = "C8oFsPostExistingPolicyCreate-" + System.currentTimeMillis();
            json = c8o.callJson("fs://.post", "_id", myId).sync();
            assertTrue(json.getBoolean("ok"));
            String id = json.getString("id");
            assertEquals(myId, id);
            json = c8o.callJson("fs://.post",
                C8o.FS_POLICY, C8o.FS_POLICY_CREATE,
                "_id", id
            ).sync();
            assertTrue(json.getBoolean("ok"));
            id = json.getString("id");
            assertNotSame(myId, id);
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsPostExistingPolicyOverride() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS);
        synchronized (c8o) {
            JSONObject json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            String myId = "C8oFsPostExistingPolicyOverride-" + System.currentTimeMillis();
            json = c8o.callJson("fs://.post",
                C8o.FS_POLICY, C8o.FS_POLICY_OVERRIDE,
                "_id", myId,
                "a", 1,
                "b", 2
            ).sync();
            assertTrue(json.getBoolean("ok"));
            String id = json.getString("id");
            assertEquals(myId, id);
            json = c8o.callJson("fs://.post",
                C8o.FS_POLICY, C8o.FS_POLICY_OVERRIDE,
                "_id", myId,
                "a", 3,
                "c", 4
            ).sync();
            assertTrue(json.getBoolean("ok"));
            id = json.getString("id");
            assertEquals(myId, id);
            json = c8o.callJson("fs://.get", "docid", myId).sync();
            assertEquals(3, json.getInt("a"));
            assertFalse(json.has("b"));
            assertEquals(4, json.getInt("c"));
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsPostExistingPolicyMerge() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS);
        synchronized (c8o) {
            JSONObject json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            String myId = "C8oFsPostExistingPolicyMerge-" + System.currentTimeMillis();
            json = c8o.callJson("fs://.post",
                C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                "_id", myId,
                "a", 1,
                "b", 2
            ).sync();
            assertTrue(json.getBoolean("ok"));
            String id = json.getString("id");
            assertEquals(myId, id);
            json = c8o.callJson("fs://.post",
                C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                "_id", myId,
                "a", 3,
                "c", 4
            ).sync();
            assertTrue(json.getBoolean("ok"));
            id = json.getString("id");
            assertEquals(myId, id);
            json = c8o.callJson("fs://.get", "docid", myId).sync();
            assertEquals(3, json.getInt("a"));
            assertEquals(2, json.getInt("b"));
            assertEquals(4, json.getInt("c"));
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsPostExistingPolicyMergeSub() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS);
        synchronized (c8o) {
            JSONObject json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            String myId = "C8oFsPostExistingPolicyMergeSub-" + System.currentTimeMillis();
            JSONObject sub_c = new JSONObject();
            JSONObject sub_f = new JSONObject();
            sub_c.put("d", 3);
            sub_c.put("e", "four");
            sub_c.put("f", sub_f);
            sub_f.put("g", true);
            sub_f.put("h", new JSONArray().put("one").put("two").put("three").put("four"));
            json = c8o.callJson("fs://.post",
                    "_id", myId,
                    "a", 1,
                    "b", -2,
                    "c", sub_c
            ).sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://.post",
                    C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                    "_id", myId,
                    "i", new JSONArray().put("5").put(6).put(7.1).put(null),
                    "c.f.j", "good",
                    "c.f.h", new JSONArray().put(true).put(false)
            ).sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://.post",
                    C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                    C8o.FS_SUBKEY_SEPARATOR, "<>",
                    "_id", myId,
                    "c<>i-j", "great"
            ).sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://.get", "docid", myId).sync();
            json.remove("_rev");
            assertEquals(myId, json.remove("_id"));
            JSONObject expectedJson = new JSONObject(
                "{\"a\":1,\"i\":[\"5\",6,7.1,null],\"b\":-2,\"c\":{\"d\":3,\"i-j\":\"great\",\"f\":{\"j\":\"good\",\"g\":true,\"h\":[true,false,\"three\",\"four\"]},\"e\":\"four\"}}"
            );
            assertEquals(expectedJson, json);
        }
    }

    public class PlainObjectA {
        public String name;
        public List<PlainObjectB> bObjects;
        public PlainObjectB bObject;
    }

    public class PlainObjectB {
        public String name;
        public int num;
        public boolean enabled;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsMergeObject() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS);
        synchronized (c8o) {
            JSONObject json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            String myId = "C8oFsMergeObject-" + System.currentTimeMillis();

            PlainObjectA plainObjectA = new PlainObjectA();
            plainObjectA.name = "plain A";
            plainObjectA.bObjects = new LinkedList<>();

            plainObjectA.bObject = new PlainObjectB();
            plainObjectA.bObject.name = "plain B 1";
            plainObjectA.bObject.num = 1;
            plainObjectA.bObject.enabled = true;
            plainObjectA.bObjects.add(plainObjectA.bObject);

            plainObjectA.bObject = new PlainObjectB();
            plainObjectA.bObject.name = "plain B 2";
            plainObjectA.bObject.num = 2;
            plainObjectA.bObject.enabled = false;
            plainObjectA.bObjects.add(plainObjectA.bObject);

            plainObjectA.bObject = new PlainObjectB();
            plainObjectA.bObject.name = "plain B -777";
            plainObjectA.bObject.num = -777;
            plainObjectA.bObject.enabled = true;

            json = c8o.callJson("fs://.post",
                    "_id", myId,
                    "a obj", plainObjectA
            ).sync();
            assertTrue(json.getBoolean("ok"));
            plainObjectA.bObjects.get(1).name = "plain B 2 bis";

            json = c8o.callJson("fs://.post",
                    C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                    "_id", myId,
                    "a obj.bObjects", plainObjectA.bObjects
            ).sync();
            assertTrue(json.getBoolean("ok"));

            plainObjectA.bObject = new PlainObjectB();
            plainObjectA.bObject.name = "plain B -666";
            plainObjectA.bObject.num = -666;
            plainObjectA.bObject.enabled = false;

            json = c8o.callJson("fs://.post",
                    C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                    "_id", myId,
                    "a obj.bObject", plainObjectA.bObject
            ).sync();
            assertTrue(json.getBoolean("ok"));

            json = c8o.callJson("fs://.post",
                    C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                    "_id", myId,
                    "a obj.bObject.enabled", true
            ).sync();
            assertTrue(json.getBoolean("ok"));

            json = c8o.callJson("fs://.get", "docid", myId).sync();
            json.remove("_rev");
            assertEquals(myId, json.remove("_id"));
            JSONObject expectedJson = new JSONObject(
                    "{\"a obj\":{\"name\":\"plain A\",\"bObjects\":[{\"enabled\":true,\"name\":\"plain B 1\",\"num\":1},{\"enabled\":false,\"name\":\"plain B 2 bis\",\"num\":2}],\"bObject\":{\"name\":\"plain B -666\",\"enabled\":true,\"num\":-666}}}"
            );
            assertEquals(expectedJson, json);
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsPostGetMultibase() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS);
        synchronized (c8o) {
            JSONObject json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://notdefault.reset").sync();
            assertTrue(json.getBoolean("ok"));
            String myId = "C8oFsPostGetMultibase-" + System.currentTimeMillis();
            json = c8o.callJson("fs://.post", "_id", myId).sync();
            assertTrue(json.getBoolean("ok"));
            try {
                c8o.callJson("fs://notdefault.get", "docid", myId).sync();
                assertTrue("not possible", false);
            } catch (Exception e) {
                assertEquals(C8oRessourceNotFoundException.class, e.getClass());
            }
            json = c8o.callJson("fs://notdefault.post", "_id", myId).sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://notdefault.get", "docid", myId).sync();
            String id = json.getString("_id");
            assertEquals(myId, id);
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsReplicateAnoAndAuth() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS_PULL);
        synchronized (c8o) {
            try {
                JSONObject json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));
                try {
                    c8o.callJson("fs://.get", "docid", "258").sync();
                    assertTrue("not possible", false);
                } catch (Exception e) {
                    assertEquals(C8oRessourceNotFoundException.class, e.getClass());
                }
                json = c8o.callJson("fs://.replicate_pull").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson("fs://.get", "docid", "258").sync();
                String value = json.getString("data");
                assertEquals("258", value);
                try {
                    c8o.callJson("fs://.get", "docid", "456").sync();
                    assertTrue("not possible", false);
                } catch (Exception e) {
                    assertEquals(C8oRessourceNotFoundException.class, e.getClass());
                }
                json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson(".LoginTesting").sync();
                value = json.getJSONObject("document").getString("authenticatedUserID");
                assertEquals("testing_user", value);
                json = c8o.callJson("fs://.replicate_pull").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson("fs://.get", "docid", "456").sync();
                value = json.getString("data");
                assertEquals("456", value);
            } finally {
                c8o.callJson(".LogoutTesting").sync();
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsReplicatePullProgress() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS_PULL);
        synchronized (c8o) {
            try {
                JSONObject json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson(".LoginTesting").sync();
                Object value = json.getJSONObject("document").getString("authenticatedUserID");
                assertEquals("testing_user", value);
                final int count[] = {0};
                final String first[] = {null};
                final String last[] = {null};
                final boolean uiThread[] = {false};
                Document doc = c8o.callXml("fs://.replicate_pull").progress(new C8oOnProgress() {
                    @Override
                    public void run(C8oProgress progress) {
                        count[0]++;
                        uiThread[0] |= Looper.getMainLooper() == Looper.myLooper();
                        if (first[0] == null) {
                            first[0] = progress.toString();
                        }
                        last[0] = progress.toString();
                    }
                }).sync();
                assertEquals("true", xpath.evaluate("/document/couchdb_output/ok", doc));
                json = c8o.callJson("fs://.get", "docid", "456").sync();
                value = json.getString("data");
                assertEquals("456", value);
                assertFalse("uiThread must be False", uiThread[0]);
                assertEquals("pull: 0/0 (running)", first[0]);
                Thread.sleep(1000);
                assertEquals("pull: 8/8 (done)", last[0]);
                assertTrue("count > 5", count[0] > 5);
            } finally {
                c8o.callJson(".LogoutTesting").sync();
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsReplicatePullProgressUI() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS_PULL);
        synchronized (c8o) {
            try {
                JSONObject json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson(".LoginTesting").sync();
                Object value = json.getJSONObject("document").getString("authenticatedUserID");
                assertEquals("testing_user", value);
                final int count[] = {0};
                final String first[] = {null};
                final String last[] = {null};
                final boolean uiThread[] = {true};
                json = c8o.callJson("fs://.replicate_pull").progressUI(new C8oOnProgress() {
                    @Override
                    public void run(C8oProgress progress) {
                        count[0]++;
                        uiThread[0] &= Looper.getMainLooper() == Looper.myLooper();
                        if (first[0] == null) {
                            first[0] = progress.toString();
                        }
                        last[0] = progress.toString();
                    }
                }).sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson("fs://.get", "docid", "456").sync();
                value = json.getString("data");
                assertEquals("456", value);
                assertTrue("uiThread must be True", uiThread[0]);
                assertEquals("pull: 0/0 (running)", first[0]);
                assertEquals("pull: 8/8 (done)", last[0]);
                assertTrue("count > 5", count[0] > 5);
            } finally {
                c8o.callJson(".LogoutTesting").sync();
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    //@Test
    public void Testing() throws Throwable {
        C8o c8o = new C8o(context, "http://192.168.100.95:" + PORT  +"/convertigo/projects/xsd", new C8oSettings()
                .setDefaultDatabaseName("hardkey_fullsync")
        );
        c8o.setLogRemote(false);
        c8o.setLogLevelLocal(Log.ERROR);
        synchronized (c8o) {
            try {
                JSONObject json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson("fs://.replicate_pull").sync();
                assertTrue(json.getBoolean("ok"));

                json = c8o.callJson("fs://.view",
                        "ddoc", "design",
                        "view", "doublekey",
                        "keys", Arrays.asList(Arrays.asList("490615","ENCOURSREALS"),Arrays.asList("490615","VALIDE"))
                ).sync();

                String sJson = json.toString(2);
                sJson.toString();
            } finally {
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsReplicatePullAnoAndAuthView() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS_PULL);
        synchronized (c8o) {
            try {
                JSONObject json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson("fs://.replicate_pull").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson("fs://.view",
                    "ddoc", "design",
                    "view", "reverse"
                ).sync();
                Object value = json.getJSONArray("rows").getJSONObject(0).getDouble("value");
                assertEquals(774.0, value);
                json = c8o.callJson("fs://.view",
                    "ddoc", "design",
                    "view", "reverse",
                    "reduce", false
                ).sync();
                value = json.getInt("count");
                assertEquals(3, value);
                value = json.getJSONArray("rows").getJSONObject(1).getString("key");
                assertEquals("852", value);
                json = c8o.callJson("fs://.view",
                    "ddoc", "design",
                    "view", "reverse",
                    "startkey", "0",
                    "endkey", "9"
                ).sync();
                value = json.getJSONArray("rows").getJSONObject(0).getDouble("value");
                assertEquals(405.0, value);
                json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson(".LoginTesting").sync();
                value = json.getJSONObject("document").getString("authenticatedUserID");
                assertEquals("testing_user", value);
                json = c8o.callJson("fs://.replicate_pull").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson("fs://.view",
                    "ddoc", "design",
                    "view", "reverse"
                ).sync();
                value = json.getJSONArray("rows").getJSONObject(0).getDouble("value");
                assertEquals(2142.0, value);
                json = c8o.callJson("fs://.view",
                    "ddoc", "design",
                    "view", "reverse",
                    "reduce", false
                ).sync();
                value = json.getInt("count");
                assertEquals(6, value);
                value = json.getJSONArray("rows").getJSONObject(1).getString("key");
                assertEquals("654", value);
                json = c8o.callJson("fs://.post", "_id", "111", "data", "16").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson("fs://.view",
                    "ddoc", "design",
                    "view", "reverse",
                    "startkey", "0",
                    "endkey", "9"
                ).sync();
                value = json.getJSONArray("rows").getJSONObject(0).getDouble("value");
                assertEquals(1000.0, value);
            } finally {
                c8o.callJson(".LogoutTesting").sync();
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsViewArrayKey() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS_PULL);
        synchronized (c8o) {
            try {
                JSONObject json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson(".LoginTesting").sync();
                Object value = json.getJSONObject("document").getString("authenticatedUserID");
                assertEquals("testing_user", value);
                json = c8o.callJson("fs://.replicate_pull").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson("fs://.view",
                        "ddoc", "design",
                        "view", "array",
                        "startkey", "[\"1\"]"
                ).sync();
            } finally {
                c8o.callJson(".LogoutTesting").sync();
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsReplicatePullGetAll() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS_PULL);
        synchronized (c8o) {
            try {
                JSONObject json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson(".LoginTesting").sync();
                Object value = json.getJSONObject("document").getString("authenticatedUserID");
                assertEquals("testing_user", value);
                json = c8o.callJson("fs://.replicate_pull").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson("fs://.all").sync();
                assertEquals(8, json.getInt("count"));
                assertEquals(8, json.getJSONArray("rows").length());
                assertEquals("789", json.getJSONArray("rows").getJSONObject(5).getString("key"));
                assertTrue(json.getJSONArray("rows").getJSONObject(5).isNull("doc"));
                json = c8o.callJson("fs://.all",
                    "include_docs", true
                ).sync();
                assertEquals(8, json.getInt("count"));
                assertEquals(8, json.getJSONArray("rows").length());
                assertEquals("789", json.getJSONArray("rows").getJSONObject(5).getString("key"));
                assertEquals("testing_user", json.getJSONArray("rows").getJSONObject(5).getJSONObject("doc").getString("~c8oAcl"));
                json = c8o.callJson("fs://.all",
                    "limit", 2
                ).sync();
                assertEquals(2, json.getInt("count"));
                assertEquals(2, json.getJSONArray("rows").length());
                assertEquals("147", json.getJSONArray("rows").getJSONObject(1).getString("key"));
                assertTrue(json.getJSONArray("rows").getJSONObject(1).isNull("doc"));
                json = c8o.callJson("fs://.all",
                    "include_docs", true,
                    "limit", 3,
                    "skip", 2
                ).sync();
                assertEquals(3, json.getInt("count"));
                assertEquals(3, json.getJSONArray("rows").length());
                assertEquals("369", json.getJSONArray("rows").getJSONObject(1).getString("key"));
                assertEquals("doc", json.getJSONArray("rows").getJSONObject(1).getJSONObject("doc").getString("type"));
            } finally {
                c8o.callJson(".LogoutTesting").sync();
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsPutAttachment() throws Throwable {
        final C8o c8o = get(Stuff.C8O_FS_PUSH);
        synchronized (c8o) {
            try {
                // First reset
                JSONObject json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));

                // Loging testing
                json = c8o.callJson(".LoginTesting").sync();
                Object value = json.getJSONObject("document").getString("authenticatedUserID");
                assertEquals("testing_user", value);

                // Sync continuous
                c8o.callJson("fs://.sync",
                        "continuous", true
                ).then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                        Log.d("Logs perso: response du sync", response.toString());
                        return null;
                    }
                })
                .progress(new C8oOnProgress() {
                    @Override
                    public void run(C8oProgress c8oProgress) {
                        Log.d("Logs perso: progress du sync", c8oProgress.toString());
                    }
                })
                .fail(new C8oOnFail() {
                    @Override
                    public void run(Throwable throwable, Map<String, Object> parameters) {
                        Log.d("Logs perso: erreur du sync", throwable.toString());
                        assertNotNull(null);
                    }
                }).sync();

                // Post de l'objet
                String id = "monidpasunique";
                c8o.callJson("fs://.post",
                        "_id", id,
                        "data", "777",
                        "bool", true,
                        "int", 777
                )
                .then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                        Log.d("Logs perso: response du post", response.toString());
                        return null;
                    }
                })
                .fail(new C8oOnFail() {
                    @Override
                    public void run(Throwable throwable, Map<String, Object> parameters) {
                        Log.d("Logs perso: erreur du post", throwable.toString());
                    }
                }).sync();

                // Put attachment du txt
                c8o.callJson("fs://.put_attachment",
                        "docid", id,
                        "name", "text2.txt",
                        "content_type", "text/plain",
                        "content", "U2FsdXQgIQo="
                )
                .then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                        Log.d("Logs perso: response du put attachment du txt", response.toString());
                        return null;
                    }
                })
                .fail(new C8oOnFail() {
                    @Override
                    public void run(Throwable throwable, Map<String, Object> parameters) {
                        Log.d("Logs perso: erreur du put attachment du txt", throwable.toString());
                    }
                }).sync();

                c8o.callJson("fs://.put_attachment",
                        "docid", id,
                        "name", "img.jpeg",
                        "content_type", "image/jpeg",
                        "content", "R0lGODlhPQBEAPeoAJosM//AwO/AwHVYZ/z595kzAP/s7P+goOXMv8+fhw/v739/f+8PD98fH/8mJl+fn/9ZWb8/PzWlwv///6wWGbImAPgTEMImIN9gUFCEm/gDALULDN8PAD6atYdCTX9gUNKlj8wZAKUsAOzZz+UMAOsJAP/Z2ccMDA8PD/95eX5NWvsJCOVNQPtfX/8zM8+QePLl38MGBr8JCP+zs9myn/8GBqwpAP/GxgwJCPny78lzYLgjAJ8vAP9fX/+MjMUcAN8zM/9wcM8ZGcATEL+QePdZWf/29uc/P9cmJu9MTDImIN+/r7+/vz8/P8VNQGNugV8AAF9fX8swMNgTAFlDOICAgPNSUnNWSMQ5MBAQEJE3QPIGAM9AQMqGcG9vb6MhJsEdGM8vLx8fH98AANIWAMuQeL8fABkTEPPQ0OM5OSYdGFl5jo+Pj/+pqcsTE78wMFNGQLYmID4dGPvd3UBAQJmTkP+8vH9QUK+vr8ZWSHpzcJMmILdwcLOGcHRQUHxwcK9PT9DQ0O/v70w5MLypoG8wKOuwsP/g4P/Q0IcwKEswKMl8aJ9fX2xjdOtGRs/Pz+Dg4GImIP8gIH0sKEAwKKmTiKZ8aB/f39Wsl+LFt8dgUE9PT5x5aHBwcP+AgP+WltdgYMyZfyywz78AAAAAAAD///8AAP9mZv///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACH5BAEAAKgALAAAAAA9AEQAAAj/AFEJHEiwoMGDCBMqXMiwocAbBww4nEhxoYkUpzJGrMixogkfGUNqlNixJEIDB0SqHGmyJSojM1bKZOmyop0gM3Oe2liTISKMOoPy7GnwY9CjIYcSRYm0aVKSLmE6nfq05QycVLPuhDrxBlCtYJUqNAq2bNWEBj6ZXRuyxZyDRtqwnXvkhACDV+euTeJm1Ki7A73qNWtFiF+/gA95Gly2CJLDhwEHMOUAAuOpLYDEgBxZ4GRTlC1fDnpkM+fOqD6DDj1aZpITp0dtGCDhr+fVuCu3zlg49ijaokTZTo27uG7Gjn2P+hI8+PDPERoUB318bWbfAJ5sUNFcuGRTYUqV/3ogfXp1rWlMc6awJjiAAd2fm4ogXjz56aypOoIde4OE5u/F9x199dlXnnGiHZWEYbGpsAEA3QXYnHwEFliKAgswgJ8LPeiUXGwedCAKABACCN+EA1pYIIYaFlcDhytd51sGAJbo3onOpajiihlO92KHGaUXGwWjUBChjSPiWJuOO/LYIm4v1tXfE6J4gCSJEZ7YgRYUNrkji9P55sF/ogxw5ZkSqIDaZBV6aSGYq/lGZplndkckZ98xoICbTcIJGQAZcNmdmUc210hs35nCyJ58fgmIKX5RQGOZowxaZwYA+JaoKQwswGijBV4C6SiTUmpphMspJx9unX4KaimjDv9aaXOEBteBqmuuxgEHoLX6Kqx+yXqqBANsgCtit4FWQAEkrNbpq7HSOmtwag5w57GrmlJBASEU18ADjUYb3ADTinIttsgSB1oJFfA63bduimuqKB1keqwUhoCSK374wbujvOSu4QG6UvxBRydcpKsav++Ca6G8A6Pr1x2kVMyHwsVxUALDq/krnrhPSOzXG1lUTIoffqGR7Goi2MAxbv6O2kEG56I7CSlRsEFKFVyovDJoIRTg7sugNRDGqCJzJgcKE0ywc0ELm6KBCCJo8DIPFeCWNGcyqNFE06ToAfV0HBRgxsvLThHn1oddQMrXj5DyAQgjEHSAJMWZwS3HPxT/QMbabI/iBCliMLEJKX2EEkomBAUCxRi42VDADxyTYDVogV+wSChqmKxEKCDAYFDFj4OmwbY7bDGdBhtrnTQYOigeChUmc1K3QTnAUfEgGFgAWt88hKA6aCRIXhxnQ1yg3BCayK44EWdkUQcBByEQChFXfCB776aQsG0BIlQgQgE8qO26X1h8cEUep8ngRBnOy74E9QgRgEAC8SvOfQkh7FDBDmS43PmGoIiKUUEGkMEC/PJHgxw0xH74yx/3XnaYRJgMB8obxQW6kL9QYEJ0FIFgByfIL7/IQAlvQwEpnAC7DtLNJCKUoO/w45c44GwCXiAFB/OXAATQryUxdN4LfFiwgjCNYg+kYMIEFkCKDs6PKAIJouyGWMS1FSKJOMRB/BoIxYJIUXFUxNwoIkEKPAgCBZSQHQ1A2EWDfDEUVLyADj5AChSIQW6gu10bE/JG2VnCZGfo4R4d0sdQoBAHhPjhIB94v/wRoRKQWGRHgrhGSQJxCS+0pCZbEhAAOw=="
                ).then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                        Log.d("Logs perso: response du put attachment de l'image", response.toString());
                        return null;
                    }
                })
                .fail(new C8oOnFail() {
                    @Override
                    public void run(Throwable throwable, Map<String, Object> parameters) {
                        Log.d("Logs perso: erreur du put aattachment de l'image", throwable.toString());
                    }
                }).sync();

            } catch(Exception e){
                c8o.log.debug("error");
            }
            finally {
                Thread.sleep(10000);
                c8o.callJson(".LogoutTesting").sync();
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsPostGetDeleteAttachment() throws Throwable {
        final C8o c8o = get(Stuff.C8O_FS_PUSH);
        synchronized (c8o) {
            try {
                // First reset
                JSONObject json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));

                // Loging testing
                json = c8o.callJson(".LoginTesting").sync();
                Object value = json.getJSONObject("document").getString("authenticatedUserID");
                assertEquals("testing_user", value);

                // Sync continuous
                c8o.callJson("fs://.sync",
                        "continuous", true
                ).then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                        Log.d("Logs perso: response du sync", response.toString());
                        return null;
                    }
                })
                        .progress(new C8oOnProgress() {
                            @Override
                            public void run(C8oProgress c8oProgress) {
                                Log.d("Logs perso: progress du sync", c8oProgress.toString());
                            }
                        })
                        .fail(new C8oOnFail() {
                            @Override
                            public void run(Throwable throwable, Map<String, Object> parameters) {
                                Log.d("Logs perso: erreur du sync", throwable.toString());
                                assertNotNull(null);
                            }
                        }).sync();

                // Post de l'objet
                String id = "monidpasunique";
                c8o.callJson("fs://.post",
                        "_id", id,
                        "data", "777",
                        "bool", true,
                        "int", 777
                )
                .then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                        Log.d("Logs perso: response du post", response.toString());
                        return null;
                    }
                })
                .fail(new C8oOnFail() {
                    @Override
                    public void run(Throwable throwable, Map<String, Object> parameters) {
                        Log.d("Logs perso: erreur du post", throwable.toString());
                    }
                }).sync();

                // Put attachment du txt
                c8o.callJson("fs://.put_attachment",
                        "docid", id,
                        "name", "text2.txt",
                        "content_type", "text/plain",
                        "content", "U2FsdXQgIQo="
                )
                .then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                        Log.d("Logs perso: response du put attachment du txt", response.toString());
                        return null;
                    }
                })
                .fail(new C8oOnFail() {
                    @Override
                    public void run(Throwable throwable, Map<String, Object> parameters) {
                        Log.d("Logs perso: erreur du put attachment du txt", throwable.toString());
                    }
                }).sync();

                // get Attachment du texte
                c8o.callJson("fs://.get",
                        "docid", id
                        ).then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                        Log.d("Logs perso: response du get attachment de l'image", response.toString());
                        String Uri = response.getJSONObject("_attachments").getJSONObject("text2.txt").get("content_url").toString();
                        if(Uri.startsWith("file:/")){
                            Uri = Uri.substring(6);
                        }
                        File file = new File(Uri);
                        FileInputStream fileInputStream = new FileInputStream(file);

                        return null;
                    }
                })
                .fail(new C8oOnFail() {
                    @Override
                    public void run(Throwable throwable, Map<String, Object> parameters) {
                        Log.d("Logs perso: erreur du get aattachment de l'image", throwable.toString());
                    }
                }).sync();

                // Delete attachment
                c8o.callJson("fs://.delete_attachment",
                        "docid", id,
                        "name", "text2.txt"
                ).then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                        Log.d("Logs perso: response du delete attachment de l'image", response.toString());
                        return null;
                    }
                })
                        .fail(new C8oOnFail() {
                            @Override
                            public void run(Throwable throwable, Map<String, Object> parameters) {
                                Log.d("Logs perso: erreur du delete aattachment de l'image", throwable.toString());
                            }
                        }).sync();

            } catch(Exception e){
                c8o.log.debug("error");
            }
            finally {
                Thread.sleep(10000);
                c8o.callJson(".LogoutTesting").sync();
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsReplicatePushAuth() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS_PUSH);
        synchronized (c8o) {
            try {
                JSONObject json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));
                String id = "C8oFsReplicatePushAnoAndAuth-" + System.currentTimeMillis();
                json = c8o.callJson("fs://.post",
                    "_id", id,
                    "data", "777",
                    "bool", true,
                    "int", 777
                ).sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson(".LoginTesting").sync();
                Object value = json.getJSONObject("document").getString("authenticatedUserID");
                assertEquals("testing_user", value);
                json = c8o.callJson("fs://.replicate_push").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson(".qa_fs_push.GetDocument", "_use_docid", id).sync();
                value = json.getJSONObject("document").getJSONObject("couchdb_output").getString("data");
                assertEquals("777", value);
                value = json.getJSONObject("document").getJSONObject("couchdb_output").getInt("int");
                assertEquals(777, value);
                value = json.getJSONObject("document").getJSONObject("couchdb_output").getString("~c8oAcl");
                assertEquals("testing_user", value);
            } finally {
                c8o.callJson(".LogoutTesting").sync();
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsReplicatePushAuthProgress() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS_PUSH);
        synchronized (c8o) {
            try {
                JSONObject json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));
                String id = "C8oFsReplicatePushAuthProgress-" + System.currentTimeMillis();
                for (int i = 0; i < 10; i++) {
                    json = c8o.callJson("fs://.post",
                        "_id", id + "-" + i,
                        "index", i
                    ).sync();
                    assertTrue(json.getBoolean("ok"));
                }
                json = c8o.callJson(".LoginTesting").sync();
                Object value = json.getJSONObject("document").getString("authenticatedUserID");
                assertEquals("testing_user", value);
                final int count[] = {0};
                final String first[] = {null};
                final String last[] = {null};
                final boolean uiThread[] = {false};
                json = c8o.callJson("fs://.replicate_push").progress(new C8oOnProgress() {
                    @Override
                    public void run(C8oProgress progress) {
                        count[0]++;
                        uiThread[0] |= Looper.getMainLooper() == Looper.myLooper();
                        if (first[0] == null) {
                            first[0] = progress.toString();
                        }
                        last[0] = progress.toString();
                    }
                }).sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson(".qa_fs_push.AllDocs",
                        "startkey", id,
                        "endkey", id + "z"
                ).sync();
                JSONArray array = json
                    .getJSONObject("document")
                    .getJSONObject("couchdb_output")
                    .getJSONArray("rows");
                assertEquals(10, array.length());
                for (int i = 0; i < 10; i++) {
                    value = array.getJSONObject(i).getJSONObject("doc").getString("_id");
                    assertEquals(id + "-" + i, value);
                    value = array.getJSONObject(i).getJSONObject("doc").getInt("index");
                    assertEquals(i, value);
                    value = array.getJSONObject(i).getJSONObject("doc").getString("~c8oAcl");
                    assertEquals("testing_user", value);
                }
                assertFalse("uiThread must be False", uiThread[0]);
                assertEquals("push: 0/0 (running)", first[0]);
                assertEquals("push: 10/10 (done)", last[0]);
                assertTrue("count > 3", count[0] > 3);
            } finally {
                c8o.callJson(".LogoutTesting").sync();
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsReplicateSyncContinuousProgress() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS_PUSH);
        c8o.setLogLevelLocal(Log.VERBOSE);
        synchronized (c8o) {
            try {
                JSONObject json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));
                String id = "C8oFsReplicateSyncContinuousProgress-" + System.currentTimeMillis();
                for (int i = 0; i < 3; i++) {
                    json = c8o.callJson("fs://.post",
                        "_id", id + "-" + i,
                        "index", i
                    ).sync();
                    assertTrue(json.getBoolean("ok"));
                }
                json = c8o.callJson(".LoginTesting").sync();
                Object value = json.getJSONObject("document").getString("authenticatedUserID");
                assertEquals("testing_user", value);
                final String firstPush[] = {null};
                final String lastPush[] = {null};
                final String livePush[] = {null};
                final String firstPull[] = {null};
                final String lastPull[] = {null};
                final String livePull[] = {null};
                json = c8o.callJson("fs://.sync", "continuous", true).progress(new C8oOnProgress() {
                    @Override
                    public void run(C8oProgress progress) {
                        if (progress.isContinuous()) {
                            if (progress.isPush()) {
                                livePush[0] = progress.toString();
                            }
                            if (progress.isPull()) {
                                livePull[0] = progress.toString();
                            }
                        } else {
                            if (progress.isPush()) {
                                if (firstPush[0] == null) {
                                    firstPush[0] = progress.toString();
                                }
                                lastPush[0] = progress.toString();
                            }
                            if (progress.isPull()) {
                                if (firstPull[0] == null) {
                                    firstPull[0] = progress.toString();
                                }
                                lastPull[0] = progress.toString();
                            }
                        }
                    }
                }).sync();
                Log.i("FS", "C8oFsReplicateSyncContinuousProgress after sync");
                assertTrue(json.getBoolean("ok"));
                assertEquals("push: 0/0 (running)", firstPush[0]);
                assertTrue("push: \\d+/\\d+ \\(done\\) for " + lastPush[0], Pattern.matches("push: \\d+/\\d+ \\(done\\)", lastPush[0]));
                assertEquals("pull: 0/0 (running)", firstPull[0]);
                assertTrue("pull: \\d+/\\d+ \\(done\\) for " + lastPull[0], Pattern.matches("pull: \\d+/\\d+ \\(done\\)", lastPull[0]));
                json = c8o.callJson(".qa_fs_push.AllDocs",
                    "startkey", id,
                    "endkey", id + "z"
                ).sync();
                JSONArray array = json
                    .getJSONObject("document")
                    .getJSONObject("couchdb_output")
                    .getJSONArray("rows");
                assertEquals(3, array.length());
                for (int i = 0; i < 3; i++) {
                    value = array.getJSONObject(i).getJSONObject("doc").getString("_id");
                    assertEquals(id + "-" + i, value);
                }
                json = c8o.callJson("fs://.get", "docid", "def").sync();
                value = json.getString("_id");
                assertEquals("def", value);
                json.put("custom", id);
                json = c8o.callJson("fs://.post", json).sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson(".qa_fs_push.PostDocument", "_id", "ghi", "custom", id).sync();
                assertTrue(json.getJSONObject("document").getJSONObject("couchdb_output").getBoolean("ok"));
                Thread.sleep(2000);
                Log.i("FS", "C8oFsReplicateSyncContinuousProgress after sleep");
                json = c8o.callJson("fs://.get", "docid", "ghi").sync();
                value = json.getString("custom");
                assertEquals(id, value);
                json = c8o.callJson(".qa_fs_push.GetDocument", "_use_docid", "def").sync();
                value = json.getJSONObject("document").getJSONObject("couchdb_output").getString("custom");
                assertEquals(id, value);
                Log.i("FS", "C8oFsReplicateSyncContinuousProgress livePull[0]: " + livePull[0]);
                Log.i("FS", "C8oFsReplicateSyncContinuousProgress livePush[0]: " + livePush[0]);
                assertTrue("pull: \\d+/\\d+ \\(live\\) for " + livePull[0], Pattern.matches("pull: \\d+/\\d+ \\(live\\)", livePull[0]));
                assertTrue("push: \\d+/\\d+ \\(live\\) for " + livePush[0], Pattern.matches("push: \\d+/\\d+ \\(live\\)", livePush[0]));
            } finally {
                c8o.callJson(".LogoutTesting").sync();
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsReplicateCancel() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS);
        synchronized (c8o) {
            JSONObject json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://.replicate_push", "cancel", true).sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://.replicate_pull", "cancel", true).sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://.sync", "cancel", true).sync();
            assertTrue(json.getBoolean("ok"));
        }
    }

    @Test
    public void C8oLocalCacheXmlPriorityLocal() throws Throwable {
        C8o c8o = get(Stuff.C8O_LC);
        String id = "C8oLocalCacheXmlPriorityLocal-" + System.currentTimeMillis();
        Document doc = c8o.callXml(".Ping",
                C8oLocalCache.PARAM, new C8oLocalCache(C8oLocalCache.Priority.LOCAL, 3000),
                "var1", id
        ).sync();
        String value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals(id, value);
        String signature = xpath.evaluate("/document/@signature", doc);
        Thread.sleep(100);
        doc = c8o.callXml(".Ping",
                C8oLocalCache.PARAM, new C8oLocalCache(C8oLocalCache.Priority.LOCAL, 3000),
                "var1", id + "bis"
        ).sync();
        value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals(id + "bis", value);
        String signature2 = xpath.evaluate("/document/@signature", doc);
        assertNotSame(signature, signature2);
        Thread.sleep(100);
        doc = c8o.callXml(".Ping",
                C8oLocalCache.PARAM, new C8oLocalCache(C8oLocalCache.Priority.LOCAL, 3000),
                "var1", id
        ).sync();
        value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals(id, value);
        signature2 = xpath.evaluate("/document/@signature", doc);
        assertEquals(signature, signature2);
        Thread.sleep(2800);
        doc = c8o.callXml(".Ping",
                C8oLocalCache.PARAM, new C8oLocalCache(C8oLocalCache.Priority.LOCAL, 3000),
                "var1", id
        ).sync();
        value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals(id, value);
        signature2 = xpath.evaluate("/document/@signature", doc);
        assertNotSame(signature, signature2);
    }

    @Test
    public void C8oFileTransferDownloadSimple() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        synchronized (c8o) {
            C8oFileTransfer ft = new C8oFileTransfer(c8o, new C8oFileTransferSettings());
            c8o.callJson("fs://" + ft.getTaskDb() + ".destroy").sync();
            final C8oFileTransferStatus[] status = new C8oFileTransferStatus[]{null};
            final Throwable[] error = new Throwable[]{null};
            ft.raiseTransferStatus(new EventHandler<C8oFileTransfer, C8oFileTransferStatus>() {
                @Override
                public void on(C8oFileTransfer source, C8oFileTransferStatus event) {
                    if (event.getState() == C8oFileTransferStatus.C8oFileTransferState.Finished) {
                        synchronized (status) {
                            status[0] = event;
                            status.notify();
                        }
                    }
                }
            }).raiseException(new EventHandler<C8oFileTransfer, Throwable>() {
                @Override
                public void on(C8oFileTransfer source, Throwable event) {
                    synchronized (status) {
                        error[0] = event;
                        status.notify();
                    }
                }
            });
            ft.start();
            String uuid = xpath.evaluate("/document/uuid/text()", c8o.callXml(".PrepareDownload4M").sync());
            assertNotNull(uuid);
            File file = new File(context.getCacheDir(), "4m.jpg");
            file.delete();
            try {
                synchronized (status) {
                    ft.downloadFile(uuid, file.getAbsolutePath());
                    status.wait(60000);
                }
                if (error[0] != null) {
                    throw error[0];
                }
                assertNotNull(status[0]);
                assertTrue(file.exists());
                long length = file.length();
                assertEquals(4237409, length);
            } finally {
                file.delete();
            }
        }
    }

    @Test
    public void C8oFileTransferUploadSimple() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        synchronized (c8o) {
            C8oFileTransfer ft = new C8oFileTransfer(c8o, new C8oFileTransferSettings());
            c8o.callJson("fs://" + ft.getTaskDb() + ".destroy").sync();
            final C8oFileTransferStatus[] status = new C8oFileTransferStatus[]{null};
            final Throwable[] error = new Throwable[]{null};
            ft.raiseTransferStatus(new EventHandler<C8oFileTransfer, C8oFileTransferStatus>() {
                @Override
                public void on(C8oFileTransfer source, C8oFileTransferStatus event) {
                    if (event.getState() == C8oFileTransferStatus.C8oFileTransferState.Finished) {
                        synchronized (status) {
                            status[0] = event;
                            status.notify();
                        }
                    }
                }
            }).raiseException(new EventHandler<C8oFileTransfer, Throwable>() {
                @Override
                public void on(C8oFileTransfer source, Throwable event) {
                    synchronized (status) {
                        error[0] = event;
                        status.notify();
                    }
                }
            });
            ft.start();
            synchronized (status) {
                ft.uploadFile("4m.jpg", context.getAssets().open("4m.jpg"));
                status.wait(20000);
            }
            if (error[0] != null) {
                throw error[0];
            }
            assertNotNull(status[0]);
            String filepath = status[0].getServerFilePath();
            String length = xpath.evaluate("/document/length/text()", c8o.callXml(".GetSizeAndDelete", "filepath", filepath).sync());
            assertEquals("4237409", length);
        }
    }

    @Test
    public void C8oLocalCacheJsonPriorityLocal() throws Throwable {
        C8o c8o = get(Stuff.C8O_LC);
        String id = "C8oLocalCacheJsonPriorityLocal-" + System.currentTimeMillis();
        JSONObject json = c8o.callJson(".Ping",
                C8oLocalCache.PARAM, new C8oLocalCache(C8oLocalCache.Priority.LOCAL, 3000),
                "var1", id
        ).sync();
        String value = json.getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals(id, value);
        String signature = json.getJSONObject("document").getJSONObject("attr").getString("signature");
        Thread.sleep(100);
        json = c8o.callJson(".Ping",
                C8oLocalCache.PARAM, new C8oLocalCache(C8oLocalCache.Priority.LOCAL, 3000),
                "var1", id + "bis"
        ).sync();
        value = json.getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals(id + "bis", value);
        String signature2 = json.getJSONObject("document").getJSONObject("attr").getString("signature");
        assertNotSame(signature, signature2);
        Thread.sleep(100);
        json = c8o.callJson(".Ping",
                C8oLocalCache.PARAM, new C8oLocalCache(C8oLocalCache.Priority.LOCAL, 3000),
                "var1", id
        ).sync();
        value = json.getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals(id, value);
        signature2 = json.getJSONObject("document").getJSONObject("attr").getString("signature");
        assertEquals(signature, signature2);
        Thread.sleep(2800);
        json = c8o.callJson(".Ping",
                C8oLocalCache.PARAM, new C8oLocalCache(C8oLocalCache.Priority.LOCAL, 3000),
                "var1", id
        ).sync();
        value = json.getJSONObject("document").getJSONObject("pong").getString("var1");
        assertEquals(id, value);
        signature2 = json.getJSONObject("document").getJSONObject("attr").getString("signature");
        assertNotSame(signature, signature2);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsLiveChanges() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS_PUSH);
        final JSONObject[] lastChanges = new JSONObject[]{null};
        final CountDownLatch[] signal = new CountDownLatch[]{null};

        C8oFullSyncChangeListener changeListener = new C8oFullSyncChangeListener() {
            @Override
            public void onChange(JSONObject changes) {
                Log.i("SdkDebug", "C8oFullSyncChangeListener before");
        //        synchronized (lastChanges) {
        //            Log.i("SdkDebug", "C8oFullSyncChangeListener after");
                    lastChanges[0] = changes;
        //            lastChanges.notify();
        //        }
                signal[0].countDown();
            }
        };

        synchronized (c8o) {
            try {
                JSONObject json = c8o.callJson("fs://.reset").sync();
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson("fs://.replicate_pull", "continuous", true).sync();
                assertTrue(json.getBoolean("ok"));

                final int[] cptlive = new int[] {0};
                signal[0] = new CountDownLatch(2);

                c8o.callJson("fs://.get", "docid", "abc", C8o.FS_LIVE, "getabc").then(new C8oOnResponse<JSONObject>() {
                    @Override
                    public C8oPromise<JSONObject> run(JSONObject response, Map<String, Object> parameters) throws Throwable {
                        Log.i("SdkDebug", "fs://.get docid abc THEN");
                        if (response.getString("_id").equals("abc")) {
                            cptlive[0]++;
                            Log.i("SdkDebug", "fs://.get docid abc THEN cptlive[0]=" + cptlive[0]);
                        }
                        signal[0].countDown();
                        return null;
                    }
                }).sync();
                assertEquals(1, cptlive[0]);
                json = c8o.callJson(".qa_fs_push.PostDocument", "_id", "ghi").sync();
                Log.i("SdkDebug", ".qa_fs_push.PostDocument _id ghi");
                assertTrue(json.getJSONObject("document").getJSONObject("couchdb_output").getBoolean("ok"));
                signal[0].await(10, TimeUnit.SECONDS);
                Log.i("SdkDebug", "assertEquals(2, cptlive[0]) = " + cptlive[0]);
                assertEquals(2, cptlive[0]);
                signal[0] = new CountDownLatch(2);
                c8o.addFullSyncChangeListener("", changeListener);
                json = c8o.callJson(".qa_fs_push.PostDocument", "_id", "jkl").sync();
                Log.i("SdkDebug", ".qa_fs_push.PostDocument  _id jkl");
                assertTrue(json.getJSONObject("document").getJSONObject("couchdb_output").getBoolean("ok"));
                Log.i("SdkDebug", "signal[0].await(15, TimeUnit.SECONDS) cptlive[0]=" + cptlive[0]);
                signal[0].await(15, TimeUnit.SECONDS);
                Log.i("SdkDebug", "assertEquals(3, cptlive[0]); = " + cptlive[0]);
                assertEquals(3, cptlive[0]);
                assertNotNull(lastChanges[0]);
                assertEquals(1, lastChanges[0].getJSONArray("changes").length());
                assertEquals("jkl", lastChanges[0].getJSONArray("changes").getJSONObject(0).getString("id"));
                signal[0] = new CountDownLatch(1);
                c8o.cancelLive("getabc");
                json = c8o.callJson(".qa_fs_push.PostDocument", "_id", "mno").sync();
                Log.i("SdkDebug", ".qa_fs_push.PostDocument  _id mno");
                assertTrue(json.getJSONObject("document").getJSONObject("couchdb_output").getBoolean("ok"));
                Log.i("SdkDebug", "signal[0].await(15, TimeUnit.SECONDS) cptlive[0]=" + cptlive[0]);
                signal[0].await(15, TimeUnit.SECONDS);
                Log.i("SdkDebug", "assertEquals(3, cptlive[0]); = " + cptlive[0]);
                assertEquals(3, cptlive[0]);
                assertNotNull(lastChanges[0]);
                assertEquals(1, lastChanges[0].getJSONArray("changes").length());
                assertEquals("mno", lastChanges[0].getJSONArray("changes").getJSONObject(0).getString("id"));
            } finally {
                c8o.cancelLive("getabc");
                c8o.removeFullSyncChangeListener("", changeListener);
            }
        }
    }

    //@Test
    public void C8oSslValid() throws Throwable {
        C8o c8o = new C8o(context, "https://" + HOST + ":444" + PROJECT_PATH);
        Document doc = c8o.callXml(".Ping", "var1", "value one").sync();
        String value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals("value one", value);
    }

    //@Test
    public void C8oSslTrustAllClientFail() throws Throwable {
        C8o c8o = new C8o(context, "https://" + HOST + ":446" + PROJECT_PATH, new C8oSettings().setTrustAllCertificates(true));
        Document doc = c8o.callXml(".Ping", "var1", "value one").sync();
        String value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals("value one", value);
    }

    //@Test
    public void C8oSslTrustAllClientOk() throws Throwable {
        C8o c8o = new C8o(context, "https://" + HOST + ":446" + PROJECT_PATH, new C8oSettings()
                .setTrustAllCertificates(true)
                .setKeyStoreInputStream(null, "")
        );
        Document doc = c8o.callXml(".Ping", "var1", "value one").sync();
        String value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals("value one", value);
    }

    //@Test
    public void C8oWithTimeout() throws Throwable {
        C8o c8o = new C8o(context, "http://" + HOST + ":" + PORT  + PROJECT_PATH, new C8oSettings().setTimeout(1000));
        Document doc = c8o.callXml(".Sleep2sec").sync();
        String value = xpath.evaluate("/document/element/text()", doc);
        assertEquals("ok", value);
    }

    @Test
    public void BadRequest() throws Throwable {
        C8oSettings settings = new C8oSettings().setLogLevelLocal(Log.DEBUG);
        settings.setLogRemote(false);
        settings.setLogLevelLocal(Log.ERROR);

        try {
            C8o c8o = new C8o(context, "http://" + HOST + ":" + PORT  + PROJECT_PATH, settings);
            JSONObject json = c8o.callJson("badRequest").sync();
            Assert.fail("it's supposed to triggered an error");
        } catch (Exception e) {
            Assert.assertEquals("'badRequest' is not a valid requestable.", e.getMessage());
        }

    }

    private void assertEqualsJsonChild(Object expectedObject, Object actualObject) {
        if (expectedObject != null) {
            assertNotNull("must not be null", actualObject);
            assertEquals(expectedObject.getClass(), actualObject.getClass());
            if (expectedObject instanceof JSONObject) {
                assertEquals((JSONObject) expectedObject, (JSONObject) actualObject);
            } else if (expectedObject instanceof JSONArray) {
                assertEquals((JSONArray) expectedObject, (JSONArray) actualObject);
            } else {
                assertEquals(expectedObject, actualObject);
            }
        } else {
            assertNull("must be null", actualObject);
        }
    }

    private void assertEquals(JSONObject expected, JSONObject actual) {
        try {
            JSONArray expectedNames = expected.names();
            JSONArray actualNames = actual.names();
            assertEquals("missing keys: " + expectedNames + " and " + actualNames, expectedNames.length(), actualNames.length());

            for (Iterator<String> i = expected.keys(); i.hasNext(); ) {
                String expectedName = i.next();
                assertTrue("missing key: " + expectedName, actual.has(expectedName));
                assertEqualsJsonChild(expected.get(expectedName), actual.get(expectedName));
            }
        } catch (Throwable t) {
            assertTrue("exception: " + t, false);
        }
    }

    private void assertEquals(JSONArray expected, JSONArray actual) {
        try {
            assertEquals("missing entries", expected.length(), actual.length());

            for (int i = 0; i < expected.length(); i++) {
                assertEqualsJsonChild(expected.get(i), actual.get(i));
            }
        } catch (Throwable t) {
            assertTrue("exception: " + t, false);
        }
    }
}