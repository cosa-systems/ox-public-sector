
package com.openexchange.mailauth.impersonate;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import com.openexchange.test.mock.AutoSuite;
import junit.framework.TestSuite;

@RunWith(AllTests.class)
public class UnitTests {
    public static TestSuite suite() {
        return AutoSuite.suite(UnitTests.class);
    }
}
