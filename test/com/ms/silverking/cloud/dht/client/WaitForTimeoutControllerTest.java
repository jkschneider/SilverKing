package com.ms.silverking.cloud.dht.client;

import static com.ms.silverking.cloud.dht.client.TestUtil.*;
import static com.ms.silverking.cloud.dht.client.WaitForTimeoutController.defaultExclusionChangeInternalRetryIntervalSeconds;
import static com.ms.silverking.cloud.dht.client.WaitForTimeoutController.defaultInternalRetryIntervalSeconds;
import static com.ms.silverking.testing.AssertFunction.*;
import static com.ms.silverking.testing.Util.int_maxVal;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ms.silverking.cloud.dht.client.gen.OmitGeneration;
import org.junit.jupiter.api.Test;

@OmitGeneration
public class WaitForTimeoutControllerTest {

    private static final int irisCopy = 20;
    private static final int irisDiff = 21;

    public  static final WaitForTimeoutController defaultController     =     WaitForTimeoutController.template;
    private static final WaitForTimeoutController defaultControllerCopy = new WaitForTimeoutController(irisCopy);
    private static final WaitForTimeoutController defaultControllerDiff = new WaitForTimeoutController(irisDiff);

    // FUTURE:bph: comments
    
    @Test
    public void testGetters() {
        Object[][] testCases = {
            {int_maxVal,                                                    getMaxAttempts_Null(defaultController)},
            {defaultInternalRetryIntervalSeconds*1000,                      getRelativeTimeoutMillisForAttempt_Null(defaultController)},
            {(long)defaultExclusionChangeInternalRetryIntervalSeconds*1000, getRelativeExclusionChangeRetryMillisForAttempt_Null(defaultController)},
//            {defaultMaxRelativeTimeoutMillis,                                 getMaxRelativeTimeoutMillis_Null(defaultController)},        // NPE if AsyncOperation param is null, testing with null b/c it's too much work to create an actual AsyncOperation...
            {int_maxVal,                                                    getMaxAttempts_Null(defaultControllerDiff)},
            {irisDiff*1000,                                                 getRelativeTimeoutMillisForAttempt_Null(defaultControllerDiff)},
            {(long)defaultExclusionChangeInternalRetryIntervalSeconds*1000, getRelativeExclusionChangeRetryMillisForAttempt_Null(defaultControllerDiff)}
//            {defaultMaxRelativeTimeoutMillis,                                 getMaxRelativeTimeoutMillis_Null(defaultControllerDiff)},    // NPE if AsyncOperation param is null, testing with null b/c it's too much work to create an actual AsyncOperation...
        };

        test_Getters(testCases);
    }

    @Test
    public void testHashCode() {
        checkHashCodeEquals(   defaultController, defaultController);
        checkHashCodeEquals(   defaultController, defaultControllerCopy);
        checkHashCodeNotEquals(defaultController, defaultControllerDiff);
    }

    @Test
    public void testEqualsObject() {
        WaitForTimeoutController[][] testCases = {
            {defaultController,     defaultController,     defaultControllerDiff},
            {defaultControllerCopy, defaultController,     defaultControllerDiff},
            {defaultControllerDiff, defaultControllerDiff, defaultController}
        };
        test_FirstEqualsSecond_FirstNotEqualsThird(testCases);

        test_NotEquals(new Object[][]{
            {defaultController, OpSizeBasedTimeoutController.template},
            {defaultController,      SimpleTimeoutController.template}
        });
    }

    @Test
    public void testToStringAndParse() {
        WaitForTimeoutController[] testCases = {
            defaultController,
            defaultControllerCopy,
            defaultControllerDiff
        };

        for (WaitForTimeoutController testCase : testCases)
            checkStringAndParse(testCase);
    }

    private void checkStringAndParse(WaitForTimeoutController controller) {
        assertEquals(controller, WaitForTimeoutController.parse( controller.toString() ));
    }
}
