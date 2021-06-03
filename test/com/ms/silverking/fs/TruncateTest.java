package com.ms.silverking.fs;

import static com.ms.silverking.fs.TestUtil.setupAndCheckTestsDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ms.silverking.fs.TestUtil;
import com.ms.silverking.process.ProcessExecutor;
import com.ms.silverking.testing.Util;
import com.ms.silverking.testing.annotations.SkfsLarge;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Test;

@SkfsLarge
public class TruncateTest {

    private static String testsDirPath;

    static {
        testsDirPath = TestUtil.getTestsDir();
    }

    private static final String truncateDirName = "truncate";
    private static final File   truncateDir     = new File(testsDirPath, truncateDirName);

    private ProcessExecutor pe;

    private static String truncateBin;
    private static String server2;

    @BeforeAll
    public static void setUpBeforeClass() {
        if (!Util.isSetSkipMultiMachineTests())
            setup();
    }

    private static void setup() {
        setupAndCheckTestsDirectory(truncateDir);

        truncateBin = Util.getEnvVariable("TRUNCATE_BIN");
        server2     = Util.getServer2();
    }

    @Test(timeout=20_000)
    public void testTruncate() {
        if (!Util.isSetSkipMultiMachineTests())
            _testTruncate();
    }

    private void _testTruncate() {
        String fileName = "file1.txt";
        String cdToDir  = "cd " + truncateDir.getAbsolutePath();

        TestUtil.testExecutionWasGood( ProcessExecutor.bashExecutor(        cdToDir + "; /bin/echo 11111111111111111111111111111111111111 > " + fileName) );
        TestUtil.testExecutionWasGood( ProcessExecutor.sshExecutor(server2, cdToDir + "; " + truncateBin + " 4 " + fileName) );
        pe =                           ProcessExecutor.bashExecutor(        cdToDir + "; /bin/cat " + fileName);
        TestUtil.testExecutionWasGood(pe);
        testOutput();
    }

    private void testOutput() {
        assertEquals("1111", pe.getTrimmedOutput());
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 1)
            testsDirPath = TestUtil.getTestsDir( args[0] );

        Util.println("Running tests in: " + testsDirPath);
        Util.runTests(TruncateTest.class);
    }
}
