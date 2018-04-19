package filesystem;

import disk.LDisk;
import iosystem.IOSystem;
import org.junit.Assert;
import org.junit.Test;


public class FileSystemTest {
    @Test
    public void createMoreThan15() {
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
    public void createWithNameLengthNot4() {
        Exception exception = null;
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            fileSystem.create("f1");
            fileSystem.create("f2");
            fileSystem.create("file4");
            fileSystem.create("f5");

        } catch (Exception e) {
            exception = e;
        }
        Assert.assertNotNull(exception);
    }

    @Test
    public void closeNotOpened() {
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
        try {
            LDisk lDisk = new LDisk();
            IOSystem ioSystem = new IOSystem(lDisk);

            FileSystem fileSystem = new FileSystem(ioSystem, true);

            fileSystem.create("f1");
            fileSystem.open("f1");
            fileSystem.destroy("f1");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}