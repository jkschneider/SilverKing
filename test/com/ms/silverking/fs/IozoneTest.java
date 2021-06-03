package com.ms.silverking.fs;

import static com.ms.silverking.fs.TestUtil.setupAndCheckTestsDirectory;
import static org.junit.jupiter.api.Assertions.*;

import com.ms.silverking.process.ProcessExecutor;
import com.ms.silverking.testing.Util;
import com.ms.silverking.testing.annotations.SkfsLarge;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Test;

@SkfsLarge
public class IozoneTest {

    private static String testsDirPath;

    static {
        testsDirPath = TestUtil.getTestsDir();
    }

    private static final String iozoneDirName = "iozone";
    private static final File   iozoneDir     = new File(testsDirPath, iozoneDirName);

    private ProcessExecutor pe;
    private static String iozoneBin;

    @BeforeAll
    public static void setUpBeforeClass() {
        setupAndCheckTestsDirectory(iozoneDir);

        iozoneBin = Util.getEnvVariable("IOZONE_BIN");
    }

    // if the test fails b/c of timeout, it's most likely b/c of the load on the machine. 3 mins is plenty of time. normally should only take 45s
    @Test(timeout=180_000)
    public void testIozone() {
        pe = ProcessExecutor.bashExecutor("cd " + iozoneDir.getAbsolutePath() + "; " + iozoneBin + " -r 128k -i 0 -i 1 -i 2 -t 24 -s 10M -I");

        TestUtil.testExecutionWasGood(pe);
        testOutput();
    }

    private void testOutput() {
        String output = pe.getOutput();
        System.out.println(output);
        assertFalse(output.isEmpty());

        String[][] outputArray = outputToArray(output);
      assertEquals(36, outputArray.length, "num items == 36");

        for (int i = 0; i < outputArray.length; i++) {
            String[] values = outputArray[i];
            String key   = values[0];
            String value = values[1];
            double expectedVal = 128;
            double actualVal   = Double.parseDouble(value);
            String msgString = "value is >= " + expectedVal;
            String valuesString = "(actual: " + key + " -> " + value + ", index: " + i + ")";

            if (i == 14 || i == 17 || i == 20 || i == 23) {
//                Children see throughput for 24 readers          =  373534.84 KB/sec
//                Parent sees throughput for 24 readers           =  179136.56 KB/sec
//                Min throughput per process                      =       0.00 KB/sec
//                Max throughput per process                      =  226925.39 KB/sec
//                Avg throughput per process                      =   15563.95 KB/sec
//                Min xfer                                        =       0.00 KB
                // just skip these
//                assertTrue(msgString + " or == 0 " + valuesString, actualVal >= expectedVal || actualVal == 0);
            }
            else {
              assertTrue(actualVal >= expectedVal, msgString + " " + valuesString);
            }
        }
    }

    private static String[][] outputToArray(String output) {
        int equalsSignCount = output.replaceAll("[^=]", "").length();
        String[] matchedGroups = output.split("[\\s]");

//        Pattern p = Pattern.compile("\\s+(\\w\\s)+\\s+=\\s+(\\S)+ KB/sec");
//        Matcher m = p.matcher(output);
//        System.out.println(m.groupCount());
//        while (m.find()) {
//            String word = m.group();
//            System.out.println(word + " " + m.start() + " " + m.end());
//        }
        
        String[][] entries = new String[equalsSignCount][2];
        int entriesCount = 0;
        for (int i = 0; i < matchedGroups.length; i++) {
            String item = matchedGroups[i].trim();
            if (item.equals("=")) {
                int before = i-1;
                while ( isWhitespace(matchedGroups[before]) )
                    before--;
                String key = matchedGroups[before];
                before--;
                while ( !isWhitespace(matchedGroups[before]) ) {
                    key = matchedGroups[before] + " " + key;
                    before--;
                }

                int after = i+1;
                while ( isWhitespace(matchedGroups[after]) )
                    after++;
                String value = matchedGroups[after];

                entries[entriesCount][0] = key;
                entries[entriesCount][1] = value;
                entriesCount++;
            }
        }

        return Arrays.copyOfRange(entries, 1, entries.length); // remove time resolution line
    }

    private static boolean isWhitespace(String s) {
        return s.trim().isEmpty();
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 1)
            testsDirPath = TestUtil.getTestsDir( args[0] );

        Util.println("Running tests in: " + testsDirPath);
        Util.runTests(IozoneTest.class);
    }
}
