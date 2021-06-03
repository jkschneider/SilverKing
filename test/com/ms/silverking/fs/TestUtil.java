package com.ms.silverking.fs;

import static com.ms.silverking.process.ProcessExecutor.*;
import static org.junit.jupiter.api.Assertions.*;

import com.google.common.io.Files;
import com.ms.silverking.io.FileUtil;
import com.ms.silverking.process.ProcessExecutor;
import com.ms.silverking.testing.Util;
import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import org.apache.commons.io.FileUtils;

public class TestUtil {

    static final String testsFolderName = "tests";

    public static String getTestsDir() {
        return getTestsDir( getDefaultSkfsRootPath() );
    }

    public static String getTestsDir(String dir) {
        return dir + separator + testsFolderName;
    }

    static String getDefaultSkfsRootPath() {
        String user = System.getProperty("user.name");
        String os   = System.getProperty("os.name").toLowerCase();

        String path;
        if (os.contains("windows")) {
            path = "C:\\Users\\" + user + "\\AppData\\Local\\Temp";
        }
        else if (os.equals("linux")) {
            String folderName = getSkFolderName();
            path = "/var/tmp/" + folderName + "/skfs/skfs_mnt/skfs";
        }
        else {
            path = "/var/tmp/silverking/data";
        }

        return path;
    }

    private static String getSkFolderName() {
        return Util.getEnvVariable("SK_FOLDER_NAME");
    }

    public static void setupAndCheckTestsDirectory(File testsDir) {
//        Util.printName("setupAndCheckTestsDirectory");
        File allTestsDir =    testsDir.getParentFile();
        File root        = allTestsDir.getParentFile();

        checkExists(root);
        checkIsDir(root);
        if ( !allTestsDir.exists() )
            createAndCheckDir(allTestsDir);

//        printDirContents("before delete", root);
        deleteRecursive(testsDir);
//        printDirContents("after delete", root);
        checkDoesntExist(testsDir);
        createAndCheckDir(testsDir);
//        printDirContents("after create", root);
    }

    public static void printDirContents(String header, File file) {
        System.out.println("  === " + header);
        long millis = System.currentTimeMillis();
        Date d = new Date(millis);
        SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss:SS MM/dd/yyyy");
        String result = df.format(millis);
        System.out.println(result);
        System.out.println( runCmd(new String[]{"/bin/sh", "-c", "ls -lR " + file.getAbsolutePath() }) );
    }

    static void printInfo(File f) {
        System.out.println("path Sep  : " + f.pathSeparator);
        System.out.println("path Sep c: " + f.pathSeparatorChar);
        System.out.println("sep       : " + f.separator);
        System.out.println("sep C     : " + f.separatorChar);
        System.out.println("abs f: " + f.getAbsoluteFile());
        System.out.println("abs d: " + f.getAbsolutePath());
        try {
            System.out.println("can f: " + f.getCanonicalFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            System.out.println("can d: " + f.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(f.getName());
        System.out.println(f.getPath());
        System.out.println(f.getParent());
    }

    /////////////////////////////////////////////
    
    static void checkExists(File f) {
      assertTrue(f.exists(), "" + f);
    }

    static void checkDoesntExist(File f) {
      assertFalse(f.exists(), "" + f);
    }

    static void checkIsDir(File f) {
      assertTrue(f.isDirectory(), "" + f);
    }

    static void checkIsFile(File f) {
      assertTrue(f.isFile(), "" + f);
    }

    static void createAndCheckDir(File f) {
      assertTrue(f.mkdir(), "" + f);
    }

    static void createAndCheckDirFail(File f) {
      assertFalse(f.mkdir(), "" + f);
    }

    static void deleteRecursive(File dir) {
        if (dir.exists()) {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static void createAndCheckFile(File f) {
        try {
          assertTrue(f.createNewFile(), "" + f);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    static void createAndCheckFileFail(File f) {
        try {
          assertFalse(f.createNewFile(), "" + f);
        } catch (IOException e) {
            com.ms.silverking.testing.Assert.assertPass(e.getMessage());
        }
    }

    static void deleteAndCheck(File f) {
      assertTrue(f.delete(), "" + f);
    }

    static void checkRead(File f, String expected) {
        try {
            assertEquals(expected, FileUtil.readFileAsString(f));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    static void checkReadIsEmpty(File f) {
        try {
            assertEquals(0, FileUtil.readFileAsBytes(f).length);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }


    static void checkWrite(File f, String contents) {
        try {
            FileUtil.writeToFile(f, contents);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    static void checkCopy(File from, File to) {
        try {
            Files.copy(from, to);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    static void checkRename(File oldFile, File newFile) {
        checkDoesntExist(newFile);
        checkExists(oldFile);

        assertTrue(oldFile.renameTo(newFile));

        checkDoesntExist(oldFile);
        checkExists(newFile);
    }

    // since assertEquals(from, to); compares file names and not contents, creating my own...
    static void checkEquals(File from, File to) {
        checkExists(from);
        checkExists(to);

        checkIsFile(from);
        checkIsFile(to);

        assertEquals(from.length(), to.length());
    }

    static void checkChecksum(File f, String expected) {
        String output = runCmd("cksum", f);
        checkChecksum(output, expected);
    }

    static void checkChecksumDir(File dir, String expected) {
        String output = runDirSumCmd("cksum", dir);
        checkChecksum(output, expected);
    }

    private static void checkChecksum(String output, String expected) {
        String[] fields = output.split(" ");

        String actual = fields[0] + " " + fields[1];
        assertEquals(expected, actual);
    }

    static void checkMd5sum(File f, String expected) {
        String output = runCmd("md5sum", f);
        checkMd5sum(output, expected);
    }

    static void checkMd5sumDir(File f, String expected) {
        String output = runDirSumCmd("md5sum", f);
        checkMd5sum(output, expected);
    }

    private static void checkMd5sum(String output, String expected) {
        String[] fields = output.split(" ");

        String actual = fields[0];
        if (actual.startsWith("\\")) // windows weirdness
            actual = actual.substring(1);
        assertEquals(expected, actual);
    }

    public static void testExecutionWasGood(ProcessExecutor pe) {
        testExecuteGood(pe);
        testDidntTimeout(pe);
        testExitCodeGood(pe);
    }

    private static void testExecuteGood(ProcessExecutor pe) {
        try {
            pe.execute();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private static void testDidntTimeout(ProcessExecutor pe) {
        assertFalse(pe.timedOut());
    }

    private static void testExitCodeGood(ProcessExecutor pe) {
        int expected = 0;
        int actual = pe.getExitCode();
      assertEquals(expected, actual, "expecting exit code == " + expected + ", was " + actual);
    }
}
