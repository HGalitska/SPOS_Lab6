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
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            fileSystem.create("f1");
            fileSystem.create("f2");
            fileSystem.create("f3");
            fileSystem.create("f4");
            fileSystem.create("f5");
            fileSystem.create("f6");
            fileSystem.create("f7");
            fileSystem.create("f8");
            fileSystem.create("f9");
            fileSystem.create("fa");
            fileSystem.create("fb");
            fileSystem.create("fc");
            fileSystem.create("fd");
            fileSystem.create("fe");
            fileSystem.create("ff");
            fileSystem.create("fg");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createWithSameName() {
        System.out.println("\n\n\n============================    create files with same name");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            fileSystem.create("f1");
            fileSystem.create("f2");
            fileSystem.create("f3");
            fileSystem.create("f1");
            fileSystem.create("f4");
            fileSystem.create("f2");
            fileSystem.create("f3");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createWithNameLengthNot2() {
        System.out.println("\n\n\n============================    create file with name length != 2");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            fileSystem.create("f1");
            fileSystem.create("f2");
            fileSystem.create("file4");
            fileSystem.create("f5");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void closeNotOpened() {
        System.out.println("\n\n\n============================    try to close file that is not opened");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            fileSystem.close(2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void closeDirectory() {
        System.out.println("\n\n\n============================    try to close the directory");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            fileSystem.close(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void closeNotCreated() {
        System.out.println("\n\n\n============================    try to close file that's not exist");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            fileSystem.create("f1");
            fileSystem.close(2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void destroyFile() {
        System.out.println("\n\n\n============================    try to destroy file");
//        Exception exception = null;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            fileSystem.create("f1");
            fileSystem.open("f1");
            fileSystem.destroy("f1");

        } catch (Exception e) {
//           exception = e;
            e.printStackTrace();
        }
//        Assert.assertNotNull(exception); // writing to block # -1
    }

    @Test
    public void testInitialBitmap() {
        System.out.println("\n\n\n============================    test initial bitmap ");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);
            System.out.println(fileSystem.bitmap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void lseekEmptyFile() {
        System.out.println("\n\n\n============================    try to lseek empty file ");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            fileSystem.create("f1");
            int OFTIndex = fileSystem.open("f1");
            fileSystem.lseek(OFTIndex, 5);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void listDirectoryWithEmptyFiles() {
        System.out.println("\n\n\n============================    list directory with empty files");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            fileSystem.create("f1");
            fileSystem.create("f2");
            fileSystem.create("f3");
            fileSystem.create("f4");

            fileSystem.directory();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void listDirectoryWithData() {
        System.out.println("\n\n\n============================    list directory with not empty files ");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            fileSystem.create("f1");
            fileSystem.create("f2");
            fileSystem.create("f3");
            fileSystem.create("f4");

            byte[] bytes = new byte[67];
            for (int i = 0; i < 67; i++) bytes[i] = (byte) i;

            int oftIndex = fileSystem.open("f2");

            fileSystem.write(oftIndex, bytes , 67);

            ByteBuffer byteBuffer = ByteBuffer.allocate(67);

            fileSystem.lseek(oftIndex, 0);

            fileSystem.read(oftIndex, byteBuffer, 67);

            System.out.println("bytes after writing and reading back: \n" + Arrays.toString(byteBuffer.array()));

            fileSystem.directory();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void readEmptyFile() {
        System.out.println("\n\n\n============================    try to read an empty file");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            ByteBuffer memArea = ByteBuffer.allocate(100);
            fileSystem.create("f1");
            int result = fileSystem.read(fileSystem.open("f1"), memArea, 5);
            System.out.println("return of read() = " + result);
            Assert.assertEquals(-3, result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void writeFile() {
        System.out.println("\n\n\n============================    try to write to file");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            byte[] memArea = new byte[125];
            for (int i = 0; i < memArea.length; i++) {
                memArea[i] = (byte) i;
            }

            fileSystem.create("f1");
            int oftindex = fileSystem.open("f1");
            fileSystem.write(oftindex, memArea, 100);
            System.out.println("------------------------------");
            fileSystem.write(oftindex, memArea, 5);
            System.out.println("------------------------------");

            fileSystem.lseek(oftindex, 23);
            ByteBuffer readBuffer = ByteBuffer.allocate(70);
            fileSystem.read(oftindex, readBuffer, 70);
            System.out.println(Arrays.toString(readBuffer.array()));
            System.out.println("------------------------------");
            fileSystem.close(oftindex);
            System.out.println("------------------------------");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void writeMoreThanBuffer() {
        System.out.println("\n\n\n============================    try to write from buffer more than buffer size");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            byte[] memArea = new byte[125];
            for (int i = 0; i < memArea.length; i++) {
                memArea[i] = (byte) i;
            }

            fileSystem.create("f1");
            int oftindex = fileSystem.open("f1");
            fileSystem.write(oftindex, memArea, 130);
            System.out.println("------------------------------");
            fileSystem.write(oftindex, memArea, 5);
            System.out.println("------------------------------");

            fileSystem.lseek(oftindex, 23);
            ByteBuffer readBuffer = ByteBuffer.allocate(126);
            fileSystem.read(oftindex, readBuffer, 126);
            System.out.println(Arrays.toString(readBuffer.array()));
            System.out.println("------------------------------");
            fileSystem.close(oftindex);
            System.out.println("------------------------------");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void writeMoreThanFileMaxSize_andThenReadMoreThanFileAndTheBuffer() {
        System.out.println("\n\n\n============================    try to write more than max file size and then read more than file and buffer can store");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            byte[] memArea = new byte[200];
            for (int i = 0; i < memArea.length; i++) {
                memArea[i] = (byte) i;
            }

            fileSystem.create("f1");
            int oftindex = fileSystem.open("f1");
            fileSystem.write(oftindex, memArea, 250);
            System.out.println("------------------------------");

            fileSystem.lseek(oftindex, 0);
            ByteBuffer readBuffer = ByteBuffer.allocate(200);
            fileSystem.read(oftindex, readBuffer, 200);
            System.out.println(Arrays.toString(readBuffer.array()));
            System.out.println("------------------------------");
            fileSystem.close(oftindex);
            System.out.println("------------------------------");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void writeMoreThanFileSize() {
        System.out.println("\n\n\n============================    try to write more than file size (expand file)");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            byte[] memArea = new byte[200];
            for (int i = 0; i < memArea.length; i++) {
                memArea[i] = (byte) i;
            }

            fileSystem.create("f1");
            int oftindex = fileSystem.open("f1");
            fileSystem.write(oftindex, memArea, 64);
            System.out.println("------------------------------");

            fileSystem.lseek(oftindex, 0);
            ByteBuffer readBuffer = ByteBuffer.allocate(200);
            fileSystem.read(oftindex, readBuffer, 200);
            System.out.println(Arrays.toString(readBuffer.array()));
            System.out.println("------------------------------");

            fileSystem.write(oftindex, memArea, 64);
            readBuffer = ByteBuffer.allocate(200);
            System.out.println(fileSystem.read(oftindex, readBuffer, 200));
            System.out.println(Arrays.toString(readBuffer.array()));


            fileSystem.lseek(oftindex, 0);
            readBuffer = ByteBuffer.allocate(200);
            System.out.println(fileSystem.read(oftindex, readBuffer, 200));
            System.out.println(Arrays.toString(readBuffer.array()));

            fileSystem.close(oftindex);



            System.out.println("==============================");




            fileSystem.destroy("f1");
            fileSystem.create("f1");
            oftindex = fileSystem.open("f1");
            fileSystem.write(oftindex, memArea, 64);
            System.out.println("------------------------------");
            fileSystem.write(oftindex, memArea, 60);

            fileSystem.lseek(oftindex, 0);
            readBuffer = ByteBuffer.allocate(200);
            fileSystem.read(oftindex, readBuffer, 200);
            System.out.println(Arrays.toString(readBuffer.array()));
            System.out.println("------------------------------");


            System.out.println("================================");


            fileSystem.destroy("f1");
            fileSystem.create("f1");
            oftindex = fileSystem.open("f1");
            fileSystem.write(oftindex, memArea, 64);
            System.out.println("------------------------------");
            fileSystem.write(oftindex, memArea, 69);

            fileSystem.lseek(oftindex, 0);
            readBuffer = ByteBuffer.allocate(200);
            fileSystem.read(oftindex, readBuffer, 200);
            System.out.println(Arrays.toString(readBuffer.array()));
            System.out.println("------------------------------");



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void readMoreThanBuffer() {
        System.out.println("\n\n\n============================    try to read to buffer more than buffer size");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            byte[] memArea = new byte[125];
            for (int i = 0; i < memArea.length; i++) {
                memArea[i] = (byte) i;
            }

            fileSystem.create("f1");
            int oftindex = fileSystem.open("f1");
            fileSystem.write(oftindex, memArea, 125);
            System.out.println("------------------------------");

            fileSystem.lseek(oftindex, 0);

            ByteBuffer readBuffer = ByteBuffer.allocate(120);

            fileSystem.read(oftindex, readBuffer, 126);
            System.out.println(Arrays.toString(readBuffer.array()));
            System.out.println("------------------------------");
            fileSystem.close(oftindex);
            System.out.println("------------------------------");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void readMoreThanFileSize() {
        System.out.println("\n\n\n============================    try to read more than file size");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            byte[] memArea = new byte[62];
            for (int i = 0; i < memArea.length; i++) {
                memArea[i] = (byte) i;
            }

            fileSystem.create("f1");
            int oftindex = fileSystem.open("f1");
            fileSystem.write(oftindex, memArea, memArea.length);
            System.out.println("------------------------------");

            fileSystem.lseek(oftindex, 0);

            ByteBuffer readBuffer = ByteBuffer.allocate(120);

            fileSystem.read(oftindex, readBuffer, 120);
            System.out.println(Arrays.toString(readBuffer.array()));
            System.out.println("------------------------------");

            fileSystem.lseek(oftindex, 50);
            readBuffer = ByteBuffer.allocate(120);
            fileSystem.read(oftindex, readBuffer, 120);
            System.out.println(Arrays.toString(readBuffer.array()));

            fileSystem.close(oftindex);
            System.out.println("------------------------------");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void readMoreThanFileMaxSize() {
        System.out.println("\n\n\n============================    try to read more than file max size");
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            byte[] memArea = new byte[193];
            for (int i = 0; i < memArea.length; i++) {
                memArea[i] = (byte) i;
            }

            fileSystem.create("f1");
            int oftindex = fileSystem.open("f1");
            fileSystem.write(oftindex, memArea, 193);
            System.out.println("------------------------------");

            fileSystem.lseek(oftindex, 0);

            ByteBuffer readBuffer = ByteBuffer.allocate(193);

            fileSystem.read(oftindex, readBuffer, 193);
            System.out.println(Arrays.toString(readBuffer.array()));
            System.out.println("------------------------------");
            fileSystem.close(oftindex);
            System.out.println("------------------------------");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}