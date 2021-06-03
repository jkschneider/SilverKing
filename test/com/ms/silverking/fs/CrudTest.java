package com.ms.silverking.fs;

import static com.ms.silverking.fs.CrudTest.LoopType.*;
import static com.ms.silverking.fs.TestUtil.*;
import static com.ms.silverking.io.FileUtil.copyDirectories;
import static com.ms.silverking.process.ProcessExecutor.*;
import static org.junit.jupiter.api.Assertions.*;

import com.ms.silverking.testing.Util;
import com.ms.silverking.testing.annotations.SkfsSmall;
import java.io.File;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * - Each @Test needs to cleanup what they generate or else it could cause the next @Test to fail
 * - If a @Test does actually fail, and the junit run reports multiple fails of @Tests, you want to look at the first fail first. The first fail could be causing the other fails. 
 *         e.g. if you have something like 2 fails reported below: 
 *                 testCopy_Directory(com.ms.silverking.fs.CrudTest)
                    org.junit.ComparisonFailure: expected:<1617524310 67[]> but was:<1617524310 67[
                ]>
                at org.junit.Assert.assertEquals(Assert.java:115)
                
                and then have
                
                testRenameLoop_Directory(com.ms.silverking.fs.CrudTest)
                java.lang.AssertionError: expected:<0> but was:<1>
                        at org.junit.Assert.fail(Assert.java:88)
                        at org.junit.Assert.failNotEquals(Assert.java:834)
                        at org.junit.Assert.assertEquals(Assert.java:645)
                        at org.junit.Assert.assertEquals(Assert.java:631)
                        at com.ms.silverking.fs.CrudTest.checkDirIsEmpty(CrudTest.java:147)
                        at com.ms.silverking.fs.CrudTest.rename_PreChecks_Directory(CrudTest.java:231)
                        at com.ms.silverking.fs.CrudTest.testRename_Directory(CrudTest.java:225)
                        at com.ms.silverking.fs.CrudTest.testLoop(CrudTest.java:310)
                        at com.ms.silverking.fs.CrudTest.testRenameLoop_Directory(CrudTest.java:270)
                        
                the first fail testCopy_Directory() is causing the testRenameLoop_Directory() fail b/c since the method failed on the assert, it wasn't able to clean up, so then when the next test case ran it failed it's pre-checks.
 * @author holstben
 *
 */
@SkfsSmall
public class CrudTest {

    public enum LoopType {
        RENAME_DIR,
        RENAME_FILE,
        DOUBLE_RENAME_DIR,
        DOUBLE_RENAME_FILE,
        COPY_DIR,
        COPY_FILE
    }

    private static final int NUMBER_OF_TIMES_TO_RENAME = 400;

    private static String testsDirPath;

    static {
        testsDirPath = TestUtil.getTestsDir();
    }

    private static final String crudDirName    = "crud";
    private static final String crudDirPath    = testsDirPath + separator + crudDirName;

    private static final String parentDirName  = "parentDir";
    private static final String parentDirPath  = crudDirPath + separator + parentDirName;

    private static final String parentFileName = "parentFile.txt";
    private static final String parentFilePath = crudDirPath + separator + parentFileName;

    private static final File crudDir   = new File(testsDirPath, crudDirName);

    private final File parentDir        = new File(crudDirPath, parentDirName);
    private final File parentDirRename  = new File(crudDirPath, parentDirName+"Rename");

    private final File parentFile       = new File(crudDirPath, parentFileName);
    private final File parentFileRename = new File(crudDirPath, parentFileName+"Rename");

    // https://www.ascii-code.com/
    // https://stackoverflow.com/questions/17874584/java-string-and-hex-character-representation
//    private static final String unicode_0xc2a0_0x0a = "_comMktData" + new String(new byte[] { 0x31, 0x32, 0x33, 0x0a });
    private static final String unicode_delete_nonBreakingSpace_latinCapitalLetterAWithCircumflex      = "_comMktData" + (char)'\u007f' + (char)'\u00a0' + (char)'\u00c2';
    private static final String unicode_delete_nonBreakingSpace_latinCapitalLetterAWithCircumflex_null = unicode_delete_nonBreakingSpace_latinCapitalLetterAWithCircumflex + (char)'\u0000';    // this works with every other ascii control character 1-31, 0 makes it fail
    private final File goodDirName      = new File(crudDirPath, parentDirName  + unicode_delete_nonBreakingSpace_latinCapitalLetterAWithCircumflex);
    private final File goodFileName     = new File(crudDirPath, parentFileName + unicode_delete_nonBreakingSpace_latinCapitalLetterAWithCircumflex);
    private final File badDirName       = new File(crudDirPath, parentDirName  + unicode_delete_nonBreakingSpace_latinCapitalLetterAWithCircumflex_null);
    private final File badFileName      = new File(crudDirPath, parentFileName + unicode_delete_nonBreakingSpace_latinCapitalLetterAWithCircumflex_null);

    private static final String testFilesDirName = "testFiles";

    private final File testFilesDir = Util.getFile(getClass(), testFilesDirName, "");
    private final File singleLine   = Util.getFile(getClass(), testFilesDirName, "singleLineFile.txt");
    private final File multipleLine = Util.getFile(getClass(), testFilesDirName, "multipleLineFile.txt");

    private final File copy1 = new File(parentDirPath, "copy1");
    private final File copy2 = new File(parentDirPath, "copy2");

    @BeforeAll
    public static void setUpBeforeClass() {
        setupAndCheckTestsDirectory(crudDir);
    }

    @AfterAll
    public static void tearDownAfterClass() {
//        deleteTestFolder();
    }

//    @Before
//    public void setUp() throws Exception {
//        createDir();
//        createFile();
//    }
//
//    @After
//    public void tearDown() throws Exception {
//    }
    
    private void createAndCheckDir() {
        TestUtil.createAndCheckDir(parentDir);
    }

    private void deleteAndCheckDir() {
        TestUtil.deleteAndCheck(parentDir);
    }

    private void createAndCheckFile() {
        TestUtil.createAndCheckFile(parentFile);
    }

    private void deleteAndCheckFile() {
        TestUtil.deleteAndCheck(parentFile);
    }

    private void checkRead(String contents) {
        TestUtil.checkRead( parentFile, contents);
    }

    private void checkWrite(String contents) {
        TestUtil.checkWrite(parentFile, contents);
    }

    @Test
    public void testCreateAndDelete_Directory() {
//        printName("testCreateAndDelete_Directory");
        createAndCheckDir();
        checkDir(true);

        deleteAndCheckDir();
        checkDir(false);
    }

    private void checkDir(boolean expected) {
        assertEquals(expected,      parentDir.exists());
        assertEquals(expected,      parentDir.isDirectory());
        assertEquals(crudDirPath,   parentDir.getParent());
        assertEquals(parentDirPath, parentDir.getPath());
        assertEquals(parentDirName, parentDir.getName());

        if (expected)
            checkDirIsEmpty(parentDir);
        else
            checkDirIsNull(parentDir);
    }

    private void checkDirIsEmpty(File dir) {
        assertEquals(0, dir.listFiles().length);
    }

    private void checkDirIsNull(File dir) {
        assertNull(dir.listFiles());
    }

    @Test
    public void testCreateAndDelete_File() {
//        printName("testCreateAndDelete_File");
        createAndCheckFile();
        checkFile(true);

        deleteAndCheckFile();
        checkFile(false);
    }

    private void checkFile(boolean expected) {
        assertEquals(expected,       parentFile.exists());
        assertEquals(expected,       parentFile.isFile());
        assertEquals(expected,       parentFile.canRead());
        assertEquals(expected,       parentFile.canWrite());
        assertEquals(0,              parentFile.length());
//        assertTrue(0,                parentFile.lastModified());
        assertEquals(crudDirPath,    parentFile.getParent());
        assertEquals(parentFilePath, parentFile.getPath());
        assertEquals(parentFileName, parentFile.getName());
    }

    @Test
    public void testCreateDirectoryWithGoodUnicodeCharactersInName() {
        TestUtil.createAndCheckDir(goodDirName);
        TestUtil.deleteAndCheck(goodDirName);
    }

    @Test
    public void testCreateFileWithGoodUnicodeCharactersInName() {
        TestUtil.createAndCheckFile(goodFileName);
        TestUtil.deleteAndCheck(goodFileName);
    }

    @Test
    public void testCreateDirectoryWithBadUnicodeCharactersInName() {
        TestUtil.createAndCheckDirFail(badDirName);
    }

    @Test
    public void testCreateFileWithBadUnicodeCharactersInName() {
        TestUtil.createAndCheckFileFail(badFileName);
    }

    @Test
    public void testRead() {
//        printName("testRead");
        createAndCheckFile();
        checkReadIsEmpty(parentFile);
        deleteAndCheckFile();
    }

    @Test
    public void testWrite() {
//        printName("testWrite");
        createAndCheckFile();

        String contents = "aba cadaba";
        checkWrite(contents);
        checkRead( contents);

        contents = "this is a first line?!#!@#?!@?# !?#?!@$?!@$2-=49159-0 285"+newline+"this is a second newline";
        checkWrite(contents);
        checkRead( contents);

        deleteAndCheckFile();
    }

    @Test
    public void testRename_Directory() {
//        printName("testRename_Directory");
        rename_PreChecks_Directory();
        checkRename(parentDir, parentDirRename);
        rename_PostChecks_Directory(parentDirRename);
    }

    private void rename_PreChecks_Directory() {
        checkDirIsEmpty(crudDir);
        checkDoesntExist(parentDir);
        checkDoesntExist(parentDirRename);

        createAndCheckDir();
    }

    private void rename_PostChecks_Directory(File renamed) {
        deleteAndCheck(renamed);

        checkDoesntExist(parentDir);
        checkDoesntExist(parentDirRename);
    }

    @Test
    public void testRename_File() {
//        printName("testRename_File");
        rename_PreChecks_File();
        checkRename(parentFile, parentFileRename);
        rename_PostChecks_File(parentFileRename);
    }

    private void rename_PreChecks_File() {
        checkDirIsEmpty(crudDir);
        checkDoesntExist(parentFile);
        checkDoesntExist(parentFileRename);

        createAndCheckFile();
    }

    private void rename_PostChecks_File(File renamed) {
        deleteAndCheck(renamed);

        checkDoesntExist(parentFile);
        checkDoesntExist(parentFileRename);
    }

    @Test
    public void testRenameLoop_Directory() {
        testLoop(RENAME_DIR);
    }

    @Test
    public void testRenameLoop_File() {
        testLoop(RENAME_FILE);
    }

    @Test
    public void testDoubleRename_Directory() {
//        printName("testDoubleRename_Directory");
        rename_PreChecks_Directory();
        checkRename(parentDir,       parentDirRename);
        checkRename(parentDirRename, parentDir);
        rename_PostChecks_Directory(parentDir);
    }

    @Test
    public void testDoubleRename_File() {
//        printName("testDoubleRename_File");
        rename_PreChecks_File();
        checkRename(parentFile,       parentFileRename);
        checkRename(parentFileRename, parentFile);
        rename_PostChecks_File(parentFile);
    }

    @Test
    public void testDoubleRenameLoop_Directory() {
        testLoop(DOUBLE_RENAME_DIR);
    }

    @Test
    public void testDoubleRenameLoop_File() {
        testLoop(DOUBLE_RENAME_FILE);
    }

    @Test
    public void testCopy_Directory() {
//        printName("testCopy_Directory");
//        printDirContents("before check", crudDir);
        createAndCheckDir();
        copyDirectories(testFilesDir, parentDir);
        checkChecksumDir(parentDir, "1617524310 67");
        checkMd5sumDir(  parentDir, "c61fd2d58a878faa10fca21a4313c355");
        assertFalse(parentDir.delete());    // equivalent to rmdir, and "rmdir of a non-empty directory should return an error"
        deleteRecursive(parentDir);
    }

    @Test
    public void testCopy_File() {
//        printName("testCopy_File");
        createAndCheckDir();
        checkCopyAndRead(singleLine,   copy1, "this is a test file with one line",                                                                                         "3402096896 33", "ffbe0d65deeba5b3abe46b0369372336");
        checkCopyAndRead(multipleLine, copy2, "this is a test file with multiple lines"+newline+"and I'm spanning"+newline+"multiple lines"+newline+"great"+newline+"EOF", "286419593 81",  "efee80a1e02ee54237b3484ab1657d50");
        deleteAndCheckDir();
    }

    private void checkCopyAndRead(File from, File to, String expectedContents, String expectedChecksum, String expectedMd5sum) {
        checkCopy(from, to);
        checkEquals(from, to);
        TestUtil.checkRead(to, expectedContents);
        checkChecksum(to, expectedChecksum);
        checkMd5sum(  to, expectedMd5sum);
        deleteAndCheck(to);
    }

    @Test
    public void testCopyLoop_Directory() {
        testLoop(COPY_DIR);
    }

    @Test
    public void testCopyLoop_File() {
        testLoop(COPY_FILE);
    }

    private void testLoop(LoopType lt) {
        for (int i = 0; i < NUMBER_OF_TIMES_TO_RENAME; i++)
            switch (lt) {
                case RENAME_DIR:
                    testRename_Directory();
                    break;
                case RENAME_FILE:
                    testRename_File();
                    break;
                case DOUBLE_RENAME_DIR:
                    testDoubleRename_Directory();
                    break;
                case DOUBLE_RENAME_FILE:
                    testDoubleRename_File();
                    break;
                case COPY_DIR:
                    testCopy_Directory();
                    break;
                case COPY_FILE:
                    testCopy_File();
                    break;
                default:
                    throw new RuntimeException("unknown loop case: " + lt);
            }
    }

    public static void main(String[] args) {
        if (args.length == 1)
            testsDirPath = TestUtil.getTestsDir( args[0] );

        Util.println("Running tests in: " + testsDirPath);
        Util.runTests(CrudTest.class);
    }
}
