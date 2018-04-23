import disk.LDisk;
import iosystem.IOSystem;
import shell.Shell;

public class MainClass {
    public static void main(String[] args) {
        LDisk ldisk = new LDisk();
        IOSystem ioSystem = new IOSystem(ldisk);
        Shell shell = new Shell(ioSystem);
        shell.start();
    }
}