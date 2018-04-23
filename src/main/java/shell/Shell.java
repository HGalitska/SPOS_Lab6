package shell;

import filesystem.FileSystem;
import iosystem.IOSystem;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

public class Shell {
    FileSystem fileSystem;
    Scanner scanner;
    boolean fsInitialized;
    IOSystem ioSystem;

    public Shell(IOSystem ioSystem) {
        fileSystem = null;
        this.ioSystem = ioSystem;
        scanner = new Scanner(System.in);
        fsInitialized = false;
    }

    public void start() {
        StringBuilder command = new StringBuilder();
        String[] input;
        System.out.print("\n\n\n========================================================================================================================================================\n\n\n");
        System.out.print(
                "=====                    =====      ==========          ==                     ===             ==               ===       ===           ==========\n" +
                        "  =====               =====         ==                  ==                 ===              ==    ==           ===  == ==  ===          ==\n" +
                        "    =====   ===    =====            =======             ==                ==               ==      ==         ===     ==    ===         ==========\n" +
                        "       ======  ======               ==                  ==                 ===              ==    ==         ===             ===        ==\n" +
                        "                                    ==========          ===========            ===             ==           ===               ===       ==========\n\n\n");
        System.out.print("========================================================================================================================================================\n\n");
        while (true) {
            System.out.print("\nsuperman$ ");
            input = scanner.nextLine().split(" ");
            command.delete(0, command.length());
            command.append(input[0]);
            switch (command.toString()) {
                case "cr": {
                    if (input.length != 2 || !fsInitialized) {
                        System.out.println("error");
                    } else {
                        create(input[1]);
                    }
                    break;
                }
                case "op": {
                    if (input.length != 2 || !fsInitialized) {
                        System.out.println("error");
                    } else {
                        open(input[1]);
                    }
                    break;
                }
                case "wr": {
                    if (input[2].length() != 1 || input.length != 4 || !fsInitialized) { // пишем, что неправильно используем.
                        System.out.println("error");
                    } else {
                        try {
                            write(Integer.parseInt(input[1]), input[2].charAt(0), Integer.parseInt(input[3]));
                        } catch (NumberFormatException e) {
                            System.out.println("error");
                        }
                    }
                    break;
                }
                case "sk": {
                    if (input.length != 3 || !fsInitialized) {
                        System.out.println("error");
                    } else {
                        try {
                            seek(Integer.parseInt(input[1]), Integer.parseInt(input[2]));
                        } catch (NumberFormatException e) {
                            System.out.println("error");
                        }
                    }
                    break;
                }
                case "rd": {
                    if (input.length != 3 || !fsInitialized) {
                        System.out.println("error");
                    } else {
                        try {
                            read(Integer.parseInt(input[1]), Integer.parseInt(input[2]));
                        } catch (NumberFormatException e) {
                            System.out.println("error");
                        }
                    }
                    break;
                }
                case "cl": {
                    if (input.length != 2 || !fsInitialized) {
                        System.out.println("error");
                    } else {
                        try {
                            close(Integer.parseInt(input[1]));
                        } catch (NumberFormatException e) {
                            System.out.println("error");
                        }
                    }
                    break;
                }
                case "de": {
                    if (input.length != 2 || !fsInitialized) {
                        System.out.println("error");
                    } else {
                        destroy(input[1]);
                    }
                    break;
                }
                case "dr": {
                    if (input.length != 1 || !fsInitialized) {
                        System.out.println("error");
                    } else {
                        directory();
                    }
                    break;
                }

                case "in": {
                    if (input.length > 2) {
                        System.out.println("error");
                    } else {
                        if (input.length == 2) {
                            init(input[1]);
                        } else {
                            init();
                        }
                    }
                    break;
                }

                case "sv": {
                    if (input.length != 2 || !fsInitialized) {
                        System.out.println("error");
                    } else {
                        save(input[1]);
                    }
                    break;
                }

                default: {
                    System.out.println("Invalid input. Shell works with such operations: cr <name>, de <name>, op <name>, cl <index>, " +
                            "rd <index> <count>, wr <index> <char> <count>, sk <index> <pos>, dr, in <disk_cont>, sv <disk_cont>");
                    break;
                }
            }
        }
    }

    /**
     * Create a new file with the name fileName.
     * Output: file <name> created.
     *
     * @param fileName name of the file.
     */
    private void create(String fileName) {
        if (fileSystem.create(fileName) == FileSystem.STATUS_ERROR) {
            System.out.println("error");
            return;
        }
        System.out.println("file \'" + fileName + "\' created");
    }

    private void open(String fileName) {
        int index = fileSystem.open(fileName);
        if (index == FileSystem.STATUS_ERROR) {
            System.out.println("error");
            return;
        }
        System.out.println("file \'" + fileName + "\' opened, index = " + index);
    }

    private void write(int index, char c, int count) {
        byte[] memArea = new byte[count];
        for (int i = 0; i < memArea.length; i++) {
            memArea[i] = (byte) c;
        }
        int written = fileSystem.write(index, memArea, count);
        if (written == FileSystem.STATUS_ERROR) {
            System.out.println("error");
            return;
        }
        System.out.println(written + " bytes written");
    }

    private void seek(int index, int pos) {
        if (fileSystem.lseek(index, pos) == FileSystem.STATUS_ERROR) {
            System.out.println("error");
            return;
        }
        System.out.println("current position is " + pos);
    }

    private void read(int index, int count) {
        if (count < 0) {
            System.out.println("error");
            return;
        }
        ByteBuffer readBuffer = ByteBuffer.allocate(count);
        int actualCount = fileSystem.read(index, readBuffer, count);
        if (actualCount == FileSystem.STATUS_ERROR) {
            System.out.println("error");
            return;
        }
        char[] symbols = new char[actualCount];
        for (int i = 0; i < actualCount; i++) {
//            symbols[i] = readBuffer.getChar();            // !!! WRONG!!! because it read 2 bytes, but we need only 1
            symbols[i] = (char) readBuffer.get();
        }
        System.out.println(actualCount + " bytes read: " + Arrays.toString(symbols));
    }

    private void close(int index) {
        if (fileSystem.close(index) == FileSystem.STATUS_ERROR) {
            System.out.println("error");
            return;
        }
        System.out.println("file " + index + " closed");
    }

    private void destroy(String fileName) {
        if (fileSystem.destroy(fileName) == FileSystem.STATUS_ERROR) {
            System.out.println("error");
            return;
        }
        System.out.println("file \'" + fileName + "\' destroyed");
    }

    private void directory() {
        fileSystem.directory();
    }

    private void init(String fileName) {
        File f = new File(fileName);
        if (f.exists()) {
            fileSystem = new FileSystem(ioSystem, fileName);
            System.out.println("disk restored");
        } else {
            fileSystem = new FileSystem(ioSystem);
            System.out.println("disk initialized");
        }
        fsInitialized = true;
    }

    private void init() {
        fileSystem = new FileSystem(ioSystem);
        System.out.println("disk initialized");
        fsInitialized = true;
    }

    private void save(String fileName) {
        fileSystem.saveFileSystemToFile(fileName);
        System.out.println("disk saved");
    }
}
