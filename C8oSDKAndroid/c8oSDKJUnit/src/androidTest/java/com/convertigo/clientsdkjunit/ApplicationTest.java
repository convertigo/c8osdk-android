package com.convertigo.clientsdkjunit;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.convertigo.clientsdk.C8o;
import com.convertigo.clientsdk.exception.C8oException;

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
        context = getActivity();
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
}