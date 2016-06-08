package com.convertigo.clientsdkjunit;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.convertigo.clientsdk.C8o;
import com.convertigo.clientsdk.C8oLocalCache;
import com.convertigo.clientsdk.C8oOnFail;
import com.convertigo.clientsdk.C8oOnProgress;
import com.convertigo.clientsdk.C8oOnResponse;
import com.convertigo.clientsdk.C8oProgress;
import com.convertigo.clientsdk.C8oPromise;
import com.convertigo.clientsdk.C8oSettings;
import com.convertigo.clientsdk.exception.C8oCouchbaseLiteException;
import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.exception.C8oRessourceNotFoundException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
        C8O_FS_PULL {
            @Override
            Object get() throws Throwable {
                C8o c8o = new C8o(context, "http://" + HOST + ":28080" + PROJECT_PATH, new C8oSettings()
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
                C8o c8o = new C8o(context, "http://" + HOST + ":28080" + PROJECT_PATH, new C8oSettings()
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
                C8o c8o = new C8o(context, "http://" + HOST + ":28080" + PROJECT_PATH);
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
        C8o c8o = new C8o(context, "http://" + HOST + "ee:28080" + PROJECT_PATH, new C8oSettings()
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
        assertEquals(C8oException.class, exception.getClass());
        exception = exception.getCause();
        assertEquals(com.convertigo.clientsdk.exception.C8oHttpRequestException.class, exception.getClass());
        exception = exception.getCause();
        assertEquals(java.net.UnknownHostException.class, exception.getClass());
        assertNotNull(exceptionLog[0]);
        assertEquals(C8oException.class, exceptionLog[0].getClass());
        exceptionLog[0] = exceptionLog[0].getCause();
        assertEquals(com.convertigo.clientsdk.exception.C8oHttpRequestException.class, exceptionLog[0].getClass());
        exceptionLog[0] = exceptionLog[0].getCause();
        assertEquals(java.net.UnknownHostException.class, exceptionLog[0].getClass());
    }

    @Test
    public void C8oUnknownHostCallWait() throws Throwable {
        Throwable exception = null;
        C8o c8o = new C8o(context, "http://" + HOST + "ee:28080" + PROJECT_PATH);
        try {
            C8oPromise<Document> promise = c8o.callXml(".Ping");
            Thread.sleep(500);
            promise.sync();
        } catch (Exception ex) {
            exception = ex;
        }
        assertNotNull(exception);
        assertEquals(C8oException.class, exception.getClass());
        exception = exception.getCause();
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
        Thread.sleep(250);
        Document doc = c8o.callXml(".GetLogs").sync();
        String sLine = xpath.evaluate("/document/line/text()", doc);
        assertTrue("sLine='" + sLine +"'", sLine != null && !sLine.isEmpty());
        JSONArray line = new JSONArray(sLine);
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
            String myId = "C8oFsPostExistingPolicyMergeSub-" + System.currentTimeMillis();

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

            c8o.callJson("fs://.post",
                    "_id", myId,
                    "a obj", plainObjectA
            ).sync();
            assertTrue(json.getBoolean("ok"));
            plainObjectA.bObjects.get(1).name = "plain B 2 bis";

            c8o.callJson("fs://.post",
                    C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                    "_id", myId,
                    "a obj.bObjects", plainObjectA.bObjects
            ).sync();
            assertTrue(json.getBoolean("ok"));

            plainObjectA.bObject = new PlainObjectB();
            plainObjectA.bObject.name = "plain B -666";
            plainObjectA.bObject.num = -666;
            plainObjectA.bObject.enabled = false;

            c8o.callJson("fs://.post",
                    C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                    "_id", myId,
                    "a obj.bObject", plainObjectA.bObject
            ).sync();
            assertTrue(json.getBoolean("ok"));

            c8o.callJson("fs://.post",
                    C8o.FS_POLICY, C8o.FS_POLICY_MERGE,
                    "_id", myId,
                    "a obj.bObject.enabled", true
            ).sync();
            assertTrue(json.getBoolean("ok"));

            json = c8o.callJson("fs://.get", "docid", myId).sync();
            json.remove("_rev");
            assertEquals(myId, json.remove("_id"));
            String expectedJson = new JSONObject(
                    "{\"a obj\":{\"name\":\"plain A\",\"bObjects\":[{\"enabled\":true,\"name\":\"plain B 1\",\"num\":1},{\"enabled\":false,\"name\":\"plain B 2 bis\",\"num\":2}],\"bObject\":{\"name\":\"plain B -666\",\"enabled\":true,\"num\":-666}}}"
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
                assertTrue(json.getBoolean("ok"));
                json = c8o.callJson("fs://.get", "docid", "456").sync();
                value = json.getString("data");
                assertEquals("456", value);
                assertFalse("uiThread must be False", uiThread[0]);
                assertEquals("pull: 0/0 (running)", first[0]);
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
        C8o c8o = new C8o(context, "http://192.168.100.95:18080/convertigo/projects/xsd", new C8oSettings()
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
                assertFalse(json.getJSONArray("rows").getJSONObject(5).has("doc"));
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
                assertFalse(json.getJSONArray("rows").getJSONObject(1).has("doc"));
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
                value = json.getJSONObject("document").getJSONObject("couchdb_output").getString("_c8oAcl");
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
                    .getJSONObject("rows")
                    .getJSONArray("item");
                assertEquals(10, array.length());
                for (int i = 0; i < 10; i++) {
                    value = array.getJSONObject(i).getJSONObject("doc").getString("_id");
                    assertEquals(id + "-" + i, value);
                    value = array.getJSONObject(i).getJSONObject("doc").getInt("index");
                    assertEquals(i, value);
                    value = array.getJSONObject(i).getJSONObject("doc").getString("_c8oAcl");
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
                    .getJSONObject("rows")
                    .getJSONArray("item");
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