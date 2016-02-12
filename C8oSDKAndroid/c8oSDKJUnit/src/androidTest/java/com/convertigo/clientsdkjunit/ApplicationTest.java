package com.convertigo.clientsdkjunit;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;

import com.convertigo.clientsdk.C8o;
import com.convertigo.clientsdk.exception.C8oException;

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

    static Context context;

    enum Stuff {
        C8O {
            @Override
            Object get() throws Throwable {
                return new C8o(context, "http://" + HOST + ":28080" + PROJECT_PATH);
            }
        };

        abstract Object get() throws Throwable;
    }

    Map<Stuff, Object> objects = Collections.synchronizedMap(new HashMap<Stuff, Object>());
    XPath xpath = XPathFactory.newInstance().newXPath();

    public ApplicationTest() {
        super(MainActivity.class);
    }

    public <T> T get(Stuff stuff) throws Throwable {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (stuff) {
            Object res = objects.get(stuff);
            if (res == null) {
                try {
                    res = stuff.get();
                } catch (Throwable e) {
                    res = e;
                }
                objects.put(stuff, res);
            }
            if (res instanceof Throwable) {
                throw (Throwable) res;
            }

            @SuppressWarnings("unchecked")
            T t = (T) res;

            return t;
        }
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
    public void C8oDefaultPingTwoSingleValue() throws Throwable {
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
    public void C8oDefaultPingTwoSingleValueOneMulti() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        Document doc = c8o.callXml(".Ping",
                "var1", "value one",
                "var2", "value two",
                "mvar1", new String[]{"mvalue one", "mvalue two", "mvalue three"}
        ).sync();
        String value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals("value one", value);
        value = xpath.evaluate("/document/pong/var2/text()", doc);
        assertEquals("value two", value);
        value = xpath.evaluate("/document/pong/mvar1[1]/text()", doc);
        assertEquals("mvalue one", value);
        value = xpath.evaluate("/document/pong/mvar1[2]/text()", doc);
        assertEquals("mvalue two", value);
        value = xpath.evaluate("/document/pong/mvar1[3]/text()", doc);
        assertEquals("mvalue three", value);
        assertEquals(3.0, xpath.evaluate("count(/document/pong/mvar1)", doc, XPathConstants.NUMBER));
    }

    @Test
    public void C8oDefaultPingTwoSingleValueTwoMulti() throws Throwable {
        C8o c8o = get(Stuff.C8O);
        Document doc = c8o.callXml(".Ping",
                "var1", "value one",
                "var2", "value two",
                "mvar1", new String[]{"mvalue one", "mvalue two", "mvalue three"},
                "mvar2", new String[]{"mvalue2 one"}
        ).sync();
        String value = xpath.evaluate("/document/pong/var1/text()", doc);
        assertEquals("value one", value);
        value = xpath.evaluate("/document/pong/var2/text()", doc);
        assertEquals("value two", value);
        value = xpath.evaluate("/document/pong/mvar1[1]/text()", doc);
        assertEquals("mvalue one", value);
        value = xpath.evaluate("/document/pong/mvar1[2]/text()", doc);
        assertEquals("mvalue two", value);
        value = xpath.evaluate("/document/pong/mvar1[3]/text()", doc);
        assertEquals("mvalue three", value);
        assertEquals(3.0, xpath.evaluate("count(/document/pong/mvar1)", doc, XPathConstants.NUMBER));
        value = xpath.evaluate("/document/pong/mvar2[1]/text()", doc);
        assertEquals("mvalue2 one", value);
        assertEquals(1.0, xpath.evaluate("count(/document/pong/mvar2)", doc, XPathConstants.NUMBER));
    }
}