package filesystem;

import disk.LDisk;
import iosystem.IOSystem;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;


public class FileSystemTest {
    @Test
    public void createMoreThan15() {
        System.out.println("\n\n\n============================    create more than 15 files");
        int actualResult = 0;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            for (int i = 0; i < 18; i++) {
                String fileName = "fil";
                if (i > 9) {
                    fileName = "fi";
                }

                fileName = fileName.concat(Integer.toString(i));
                actualResult = fileSystem.create(fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertEquals(-3, actualResult);
    }

    @Test
    public void createWithSameName() {
        System.out.println("\n\n\n============================    create files with same name");
        int actualResult = 0;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            for (int i = 0; i < 5; i++) {
                String fileName = "fil";
                fileName = fileName.concat(Integer.toString(i));
                actualResult = fileSystem.create(fileName);
            }

            actualResult = fileSystem.create("fil3");

        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertEquals(-3, actualResult);
    }

    @Test
    public void createWithNameLengthNot4() {
        System.out.println("\n\n\n============================    create file with name length != 4");

        int actualResult = 0;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            actualResult = fileSystem.create("file4");

        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertEquals(-3, actualResult);
    }

    @Test
    public void closeNotOpened() {
        System.out.println("\n\n\n============================    try to close file that is not opened");
        int actualResult = 0;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            actualResult = fileSystem.close(2);

        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertEquals(-3, actualResult);
    }

    @Test
    public void openOpened() {
        System.out.println("\n\n\n============================    try to open file that is opened");
        int actualIndex = -1;
        int oftindex = -1;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            fileSystem.create("fil1");
            oftindex = fileSystem.open("fil1");
            actualIndex = fileSystem.open("fil1");

        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertEquals(oftindex, actualIndex);
    }

    @Test
    public void seekBeginEmptyFile() {
        System.out.println("\n\n\n============================    try to seek to 0 in empty file");
        int actualPos = -1;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            fileSystem.create("fil1");
            int oftindex = fileSystem.open("fil1");

            fileSystem.lseek(1, 0);
            actualPos = fileSystem.OFT.entries[oftindex].currentPosition;

        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertEquals(0, actualPos);
    }

    @Test
    public void seekToEnd() {
        System.out.println("\n\n\n============================    write 30 bytes, seek 64");
        int actualPos = -1;
        int actualWritten = -1;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            fileSystem.create("fil1");
            int oftindex = fileSystem.open("fil1");
            byte[] memArea = new byte[125];
            for (int i = 0; i < memArea.length; i++) {
                memArea[i] = (byte) i;
            }
            actualWritten = fileSystem.write(1, memArea, 30);

            fileSystem.lseek(1, 64);
            actualPos = fileSystem.OFT.entries[oftindex].currentPosition;

        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertEquals(64, actualPos);
        Assert.assertEquals(30, actualWritten);
    }

    @Test
    public void writeFile() {
        System.out.println("\n\n\n============================    try to write to file and read from it");
        int actualWritten = 0;
        int actualRead = 0;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            byte[] memArea = new byte[125];
            for (int i = 0; i < memArea.length; i++) {
                memArea[i] = (byte) i;
            }

            fileSystem.create("fil1");
            int oftindex = fileSystem.open("fil1");
            actualWritten += fileSystem.write(oftindex, memArea, 100);
            actualWritten += fileSystem.write(oftindex, memArea, 5);

            fileSystem.lseek(oftindex, 23);
            ByteBuffer readBuffer = ByteBuffer.allocate(70);
            actualRead = fileSystem.read(oftindex, readBuffer, 70);
            fileSystem.close(oftindex);


        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(105, actualWritten);
        Assert.assertEquals(70, actualRead);

    }

    @Test
    public void rewriteFile() {
        System.out.println("\n\n\n============================    try to rewrite file");
        int actualWritten = 0;
        int actualRewritten = 0;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            byte[] memArea = new byte[125];
            for (int i = 0; i < memArea.length; i++) {
                memArea[i] = (byte) i;
            }

            fileSystem.create("fil1");
            int oftindex = fileSystem.open("fil1");
            actualWritten += fileSystem.write(oftindex, memArea, 100);
            actualWritten += fileSystem.write(oftindex, memArea, 5);

            fileSystem.lseek(oftindex, 23);
            actualRewritten = fileSystem.write(oftindex, memArea, 70);
            fileSystem.close(oftindex);


        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(105, actualWritten);
        Assert.assertEquals(70, actualRewritten);

    }

    @Test
    public void writeSeekBackWrite() {
        System.out.println("\n\n\n============================    try to write 2 blocks seek to 1 rewrite");
        int actualWritten = 0;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            byte[] memArea1 = new byte[100];
            for (int i = 0; i < memArea1.length; i++) {
                memArea1[i] = (byte) i;
            }

            byte[] memArea2 = new byte[10];
            for (int i = 0; i < memArea2.length; i++) {
                memArea2[i] = (byte) 'e';
            }

            fileSystem.create("fil1");
            int oftindex = fileSystem.open("fil1");
            actualWritten += fileSystem.write(oftindex, memArea1, 100);
            fileSystem.lseek(oftindex, 23);
            actualWritten += fileSystem.write(oftindex, memArea2, 5);

            fileSystem.close(oftindex);


        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(105, actualWritten);

    }

    @Test
    public void writeToEndOfFile() {
        System.out.println("\n\n\n============================    try to write to end of file");
        int actualWritten = 0;
        int actualWrittenToEnd = 0;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            byte[] memArea = new byte[125];
            for (int i = 0; i < memArea.length; i++) {
                memArea[i] = (byte) i;
            }

            fileSystem.create("fil1");
            int oftindex = fileSystem.open("fil1");
            actualWritten += fileSystem.write(oftindex, memArea, 100);
            fileSystem.close(oftindex);

            oftindex = fileSystem.open("fil1");

            actualWrittenToEnd = fileSystem.write(oftindex, memArea, 70);
            fileSystem.close(oftindex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(100, actualWritten);
        Assert.assertEquals(70, actualWrittenToEnd);

    }

    @Test
    public void writeMoreToFile() {
        System.out.println("\n\n\n============================    try to write more than 192 bytes to file");
        int actualWritten = 0;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            byte[] memArea = new byte[200];
            for (int i = 0; i < memArea.length; i++) {
                memArea[i] = (byte) i;
            }

            fileSystem.create("fil1");
            int oftindex = fileSystem.open("fil1");
            actualWritten += fileSystem.write(oftindex, memArea, 200);


        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(192, actualWritten);

    }

    //******************************************************************************************/

    //************************************ OTHER TESTS ************************************//


    @Test
    public void closeDirectory() {
        System.out.println("\n\n\n============================    try to close the directory");
        int actualResult = 0;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            actualResult = fileSystem.close(0);

        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertEquals(-3, actualResult);
    }

    @Test
    public void closeNotCreated() {
        System.out.println("\n\n\n============================    try to close file that's not exist");
        int actualResult = 0;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            fileSystem.create("fil1");
            actualResult = fileSystem.close(2);

        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(-3, actualResult);
    }

    @Test
    public void destroyFile() {
        System.out.println("\n\n\n============================    try to destroy file");
        Exception exception = null;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem);

            fileSystem.create("fil1");
            fileSystem.open("fil1");
            fileSystem.destroy("fil1");

        } catch (Exception e) {
            exception = e;
        }
        Assert.assertNull(exception);
    }
}