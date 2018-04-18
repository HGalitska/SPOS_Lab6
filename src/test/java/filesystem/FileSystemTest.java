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

            fileSystem.create("fil1");
            fileSystem.create("fil2");
            fileSystem.create("fil3");
            fileSystem.create("fil4");
            fileSystem.create("fil5");
            fileSystem.create("fil6");
            fileSystem.create("fil7");
            fileSystem.create("fil8");
            fileSystem.create("fil9");
            fileSystem.create("fi10");
            fileSystem.create("fi11");
            fileSystem.create("fi12");
            fileSystem.create("fi13");
            fileSystem.create("fi14");
            fileSystem.create("fi15");
            fileSystem.create("fi16");


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

            fileSystem.create("fil1");
            fileSystem.create("fil2");
            fileSystem.create("fil3");
            fileSystem.create("fil1");
            fileSystem.create("fil4");
            fileSystem.create("fil2");
            fileSystem.create("fil3");


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

            fileSystem.create("fil1");
            fileSystem.create("fil2");
            fileSystem.create("file4");
            fileSystem.create("fil5");

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

            fileSystem.create("fil1");
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

            fileSystem.create("fil1");
            fileSystem.close(fileSystem.open("fil1"));
            fileSystem.destroy("fil1");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}