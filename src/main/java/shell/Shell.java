package shell;

import filesystem.FileSystem;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

public class Shell {
    FileSystem fileSystem;
    Scanner scanner;

    public Shell(FileSystem fileSystem) {
        if (fileSystem == null) throw new IllegalArgumentException("FileSystem should NOT be null.");
        this.fileSystem = fileSystem;
        scanner = new Scanner(System.in);
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
                    if (input.length != 2) {
                        System.out.println("error");
                    } else {
                        create(input[1]);
                    }
                    break;
                }
                case "op": {
                    if (input.length != 2) {
                        System.out.println("error");
                    } else {
                        open(input[1]);
                    }
                    break;
                }
                case "wr": {
                    if (input[2].length() != 1 || input.length != 4) { // пишем, что неправильно используем.
                        System.out.println("error");
                    } else {
                        write(Integer.parseInt(input[1]), input[2].charAt(0), Integer.parseInt(input[3]));
                    }
                    break;
                }
                case "sk": {
                    if (input.length != 3) {
                        System.out.println("error");
                    } else {
                        seek(Integer.parseInt(input[1]), Integer.parseInt(input[2]));
                    }
                    break;
                }
                case "rd": {
                    if (input.length != 3) {
                        System.out.println("error");
                    } else {
                        read(Integer.parseInt(input[1]), Integer.parseInt(input[2]));
                    }
                    break;
                }
                case "cl": {
                    if (input.length != 2) {
                        System.out.println("error");
                    } else {
                        close(Integer.parseInt(input[1]));
                    }
                    break;
                }
                case "de": {
                    if (input.length != 2) {
                        System.out.println("error");
                    } else {
                        destroy(input[1]);
                    }
                    break;
                }
                case "dr": {
                    if (input.length != 1) {
                        System.out.println("error");
                    } else {
                        directory();
                    }
                    break;
                }

                default: {
                    System.out.println("Invalid input. Shell works with such operations: cr <file_name>, op <file_name>.");
                    break;
                }
            }
        }
    }

    private void create(String fileName) {
        if (fileSystem.create(fileName) == FileSystem.STATUS_ERROR) {
            System.out.println("error");
            return;
        }
        System.out.println(fileName + " created");
    }

    private void open(String fileName) {
        int index = fileSystem.open(fileName);
        if (index == FileSystem.STATUS_ERROR) {
            System.out.println("error");
            return;
        }
        System.out.println(fileName + " opened, index = " + index);
    }

    private void write(int index, char c, int count) {
        byte[] memArea = new byte[count];
        for (int i = 0; i < memArea.length; i++) {
            memArea[i] = (byte) c;
        }
        if (fileSystem.write(index, memArea, count) == FileSystem.STATUS_ERROR) System.out.println("error");
    }

    private void seek(int index, int pos) {
        if (fileSystem.lseek(index, pos) == FileSystem.STATUS_ERROR) {
            System.out.println("error");
        }
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
        if (fileSystem.close(index) == FileSystem.STATUS_ERROR) System.out.println("error");
        ;
    }

    private void destroy(String fileName) {
        if (fileSystem.destroy(fileName) == FileSystem.STATUS_ERROR) {
            System.out.println("error");
            return;
        }
        System.out.println("file " + fileName + " destroyed");
    }

    private void directory() {
        fileSystem.directory();
    }
}
