package se.kth.kthfsdashboard.util;

import se.kth.kthfsdashboard.utils.FormatUtils;
import junit.framework.TestCase;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class FormatterTest extends TestCase {

    final Long K = 1024L;
    final Long M = K * K;
    final Long G = M * K;
    final Long T = G * K;

    public FormatterTest(String testName) {
        super(testName);
    }

    public void testParseDouble() throws Exception {
        assertEquals("1 TB", FormatUtils.storage(T));
        assertEquals("1.1 TB", FormatUtils.storage(T + 99*G));
    }
}
