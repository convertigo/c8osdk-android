package com.convertigo.clientsdkjunit;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.convertigo.clientsdk.C8o;
import com.convertigo.clientsdk.C8oOnFail;
import com.convertigo.clientsdk.C8oOnProgress;
import com.convertigo.clientsdk.C8oOnResponse;
import com.convertigo.clientsdk.C8oProgress;
import com.convertigo.clientsdk.C8oPromise;
import com.convertigo.clientsdk.C8oSettings;
import com.convertigo.clientsdk.exception.C8oCouchbaseLiteException;
import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oHttpRequestException;
import com.convertigo.clientsdk.exception.C8oRessourceNotFoundException;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith(AndroidJUnit4.class)
public class ApplicationTest extends ActivityInstrumentationTestCase2<MainActivity> {
    static final String HOST = "buildus.twinsoft.fr";
    static final String PROJECT_PATH = "/convertigo/projects/ClientSDKtesting";

    static final XPath xpath = XPathFactory.newInstance().newXPath();

    static Context context;

    enum Stuff {
        C8O {
            @Override
            Object get() throws Throwable {
                C8o c8o = new C8o(context, "http://" + HOST + ":28080" + PROJECT_PATH);
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
                C8o c8o = new C8o(context, "http://" + HOST + ":28080" + PROJECT_PATH, new C8oSettings()
                    .setDefaultDatabaseName("clientsdktesting")
                );
                c8o.setLogRemote(false);
                c8o.setLogLevelLocal(Log.ERROR);
                return c8o;
            }
        },
        C8O_FS_REMOTE {
            @Override
            Object get() throws Throwable {
                C8o c8o = new C8o(context, "http://" + HOST + ":28080" + PROJECT_PATH, new C8oSettings()
                        .setDefaultDatabaseName("client_sdk_testing_fullsync")
                );
                c8o.setLogRemote(false);
                c8o.setLogLevelLocal(Log.ERROR);
                //JSONObject json = c8o.callJson(".InitFS").sync();
                //assertTrue(json.getJSONObject("document").getBoolean("ok"));
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
        new C8o(context, "http://" + HOST + ":28080");
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
    public void C8oDefaultPingOneSingleValue() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        Document doc = c8o.callXml(".Ping", "var1", "value one").sync();
        String value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals("value one", value);
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

    public void CheckLogRemoteHelper(C8o c8o, String lvl, String msg) throws Throwable {
        Document doc = c8o.callXml(".GetLogs").sync();
        JSONArray line = new JSONArray(xpath.evaluate("/document/line/text()", doc));
        assertEquals(lvl, line.getString(2));
        String newMsg = line.getString(4);
        newMsg = newMsg.substring(newMsg.indexOf("logID="));
        assertEquals(msg, newMsg);
    }

    @Test
    public void CheckLogRemote() throws Throwable {
        C8o c8o = new C8o(context, "http://" + HOST + ":28080" + PROJECT_PATH);
        c8o.setLogC8o(false);
        String id = "logID=" + System.currentTimeMillis();
        c8o.callXml(".GetLogs", "init", id).sync();
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
        final Throwable[] xfail = new Throwable[3];
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

    @Test
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
            String expectedJson = new JSONObject(
                "{\"a\":1,\"i\":[\"5\",6,7.1,null],\"b\":-2,\"c\":{\"d\":3,\"i-j\":\"great\",\"f\":{\"j\":\"good\",\"g\":true,\"h\":[true,false,\"three\",\"four\"]},\"e\":\"four\"}}"
            ).toString();
            String sJson = json.toString();
            assertEquals(expectedJson, sJson);
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
        C8o c8o = get(Stuff.C8O_FS_REMOTE);
        synchronized (c8o) {
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
            json = c8o.callJson(".LoginTesting").sync();
            value = json.getJSONObject("document").getString("authenticatedUserID");
            assertEquals("testing_user", value);
            json = c8o.callJson("fs://.replicate_pull").sync();
            c8o.callJson(".LogoutTesting").sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://.get", "docid", "456").sync();
            value = json.getString("data");
            assertEquals("456", value);
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsReplicatePullProgress() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS_REMOTE);
        synchronized (c8o) {
            JSONObject json = c8o.callJson("fs://.reset").sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson(".LoginTesting").sync();
            Object value = json.getJSONObject("document").getString("authenticatedUserID");
            assertEquals("testing_user", value);
            final int count[] = {0};
            final String first[] = {null};
            final String last[] = {null};
            final boolean uiThread[] = {false};
            json = c8o.callJson("fs://.replicate_pull").progress(new C8oOnProgress() {
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
            c8o.callJson(".LogoutTesting").sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://.get", "docid", "456").sync();
            value = json.getString("data");
            assertEquals("456", value);
            assertFalse("uiThread must be False", uiThread[0]);
            assertEquals("pull: 0/0 (running)", first[0]);
            assertEquals("pull: 8/8 (done)", last[0]);
            assertTrue("count > 5", count[0] > 5);
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsReplicatePullProgressUI() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS_REMOTE);
        synchronized (c8o) {
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
            c8o.callJson(".LogoutTesting").sync();
            assertTrue(json.getBoolean("ok"));
            json = c8o.callJson("fs://.get", "docid", "456").sync();
            value = json.getString("data");
            assertEquals("456", value);
            assertTrue("uiThread must be True", uiThread[0]);
            assertEquals("pull: 0/0 (running)", first[0]);
            assertEquals("pull: 8/8 (done)", last[0]);
            assertTrue("count > 5", count[0] > 5);
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void C8oFsReplicatePullAnoAndAuthView() throws Throwable {
        C8o c8o = get(Stuff.C8O_FS_REMOTE);
        synchronized (c8o) {
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
            json = c8o.callJson(".LoginTesting").sync();
            value = json.getJSONObject("document").getString("authenticatedUserID");
            assertEquals("testing_user", value);
            json = c8o.callJson("fs://.replicate_pull").sync();
            c8o.callJson(".LogoutTesting").sync();
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
        C8o c8o = new C8o(context, "http://" + HOST + ":28080" + PROJECT_PATH, new C8oSettings().setTimeout(1000));
        Document doc = c8o.callXml(".Sleep2sec").sync();
        String value = xpath.evaluate("/document/element/text()", doc);
        assertEquals("ok", value);
    }
}