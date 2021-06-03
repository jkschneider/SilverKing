package com.ms.silverking.fs.test;

import static com.ms.silverking.fs.TestUtil.setupAndCheckTestsDirectory;
import static com.ms.silverking.testing.Assert.assertPass;
import static com.ms.silverking.testing.Util.createToString;
import static com.ms.silverking.testing.Util.getTestMessage;
import static org.junit.jupiter.api.Assertions.*;

import com.ms.silverking.fs.TestUtil;
import com.ms.silverking.testing.Util;
import com.ms.silverking.testing.annotations.SkfsLarge;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Test;

@SkfsLarge
public class FileWriterTest {

    private static long size;
    private static String testsDirPath;

    static {
        testsDirPath = TestUtil.getTestsDir();
    }

    private static final String fileWriterDirName = "file-writer";
    private static final File   fileWriterDir     = new File(testsDirPath, fileWriterDirName);

    @BeforeAll
    public static void setUpBeforeClass() {
        setupAndCheckTestsDirectory(fileWriterDir);

        size = Long.parseLong( Util.getEnvVariable("SK_FILE_WRITER_FILE_SIZE").replace("_", "") );
    }

    @Test(timeout=60_000)
    public void testWriteRead() {
        FileWriter fw = new FileWriter(new File(fileWriterDir, "fw.out"), size);

        try {
            fw.write();
            assertPass("Write completed");

            List<byte[]> readBuffers = fw.read();
            assertPass("Read completed");

            long expectedSize = size / fw.getBufferSize();
            if (size % fw.getBufferSize() != 0)
                expectedSize++;
            assertEquals(expectedSize, readBuffers.size());

            for (byte[] readBuffer : readBuffers) {
                int length = readBuffer.length;
                byte[] expected = new byte[length];
                FileWriter.fillBuffer(expected);

//                if (Arrays.hashCode(expected) != Arrays.hashCode(readBuffer)) {    // for speed. actually hurts speed. compare to Arrays.deepHashCode and expected.hashCode()
                    assertArrayEquals(expected, readBuffer);
//                    String expectedStr = createToString(expected);
//                    String actual      = createToString(readBuffer);
//                    assertEquals(getTestMessage("testReadWrite", expectedStr, actual), expectedStr, actual);
//                }
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }

//        StringBuffer expected = new StringBuffer();
//        long totalBytes = 0;
//        int bufferSize = fw.getBufferSize();
//        while (totalBytes < size) {
//            int    bytesToFill = (int)Math.min(size - totalBytes, bufferSize);
//            byte[] expectedBytes = new byte[bytesToFill];
//            FileWriter.fillBuffer(expectedBytes);
//            expected.append( createToString(expectedBytes) );
//            totalBytes += bytesToFill;
//        }
//        
//        try {
//            fw.write();
//            StringBuffer readBytes = fw.read2();
//            assertEquals(expected.toString(), readBytes.toString());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 1)
            testsDirPath = TestUtil.getTestsDir( args[0] );

        Util.println("Running tests in: " + testsDirPath);
        Util.runTests(FileWriterTest.class);
    }

}
