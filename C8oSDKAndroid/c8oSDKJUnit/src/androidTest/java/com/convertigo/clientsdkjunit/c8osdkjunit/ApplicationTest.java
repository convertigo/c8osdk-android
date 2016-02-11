package com.convertigo.clientsdkjunit.c8osdkjunit;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;

import com.convertigo.clientsdk.C8o;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathFactory;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith(AndroidJUnit4.class)
public class ApplicationTest extends ActivityInstrumentationTestCase2<MainActivity> {
    public ApplicationTest() {
        super(MainActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
    }

    @Test
    public void C8oDefault() throws Throwable {
        final C8o c8o = new C8o(getActivity(), "http://pulse.twinsoft.fr:18080/convertigo/projects/Sample05");
        Document doc = c8o.callXml(".sample05.GetServerInfo").sync();
        assertNotNull(doc);
        String uuid = XPathFactory.newInstance().newXPath().evaluate("//uuid/text()", doc);
        assertNotNull(uuid);
    }

    @Test
    public void toto() throws Exception {
        assertEquals(6, 3 * 2);
    }
}