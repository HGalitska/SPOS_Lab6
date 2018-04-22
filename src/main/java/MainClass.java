import disk.LDisk;
import filesystem.FileSystem;
import iosystem.IOSystem;
import shell.Shell;

public class MainClass {
    public static void main(String[] args) {
        LDisk ldisk = new LDisk();
        IOSystem ioSystem = new IOSystem(ldisk);
        FileSystem fileSystem = new FileSystem(ioSystem, true);
        Shell shell = new Shell(fileSystem);
        shell.start();
    }
}