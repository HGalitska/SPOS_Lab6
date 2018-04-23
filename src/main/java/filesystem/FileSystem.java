package filesystem;

import disk.LDisk;
import iosystem.IOSystem;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.BitSet;

public class FileSystem {

    final static int NUMBER_OF_FILE_DESCRIPTORS = 16;
    public final static int STATUS_SUCCESS = 1;
    public final static int STATUS_ERROR = -3;
    private final static int END_OF_FILE = 3 * IOSystem.getBlockLengthInBytes();

    private IOSystem ioSystem;

    public OpenFileTable OFT;   // made package-public for testing purposes
    BitSet bitmap; // made package-public for testing purposes
    private Directory directory;
    private FileDescriptor[] fileDescriptors;

    public FileSystem(IOSystem ioSystem) {
        if (ioSystem == null) throw new IllegalArgumentException("IOSystem should NOT be NULL");
        OFT = new OpenFileTable();
        OFT.entries[0] = new OpenFileTable.OFTEntry();
        OFT.entries[0].FDIndex = 0;

        bitmap = new BitSet(64);
        bitmap.set(0, 8, true); // set 1 bit for bitmap + 4 for file descriptors + 3 for dir

        this.ioSystem = ioSystem;
        fileDescriptors = new FileDescriptor[NUMBER_OF_FILE_DESCRIPTORS];
        directory = new Directory();

        initEmptyDisk();
    }

    public FileSystem(IOSystem ioSystem, String fileName) {
        if (ioSystem == null) throw new IllegalArgumentException("IOSystem should NOT be NULL");
        OFT = new OpenFileTable();
        OFT.entries[0] = new OpenFileTable.OFTEntry();
        OFT.entries[0].FDIndex = 0;

        bitmap = new BitSet(64);
        bitmap.set(0, 8, true); // set 1 bit for bitmap + 4 for file descriptors + 3 for dir

        this.ioSystem = ioSystem;
        fileDescriptors = new FileDescriptor[NUMBER_OF_FILE_DESCRIPTORS];
        directory = new Directory();

        initFileSystemFromFile(fileName);
    }


    private void initEmptyDisk() {
//        bitmap.set(8, 64, false); // all data blocks are empty

        int fdsPerBlock = 16 * NUMBER_OF_FILE_DESCRIPTORS / IOSystem.getBlockLengthInBytes();
        // init directory and add it to OFT
        fileDescriptors[0] = new FileDescriptor(0, new int[]{fdsPerBlock + 1, fdsPerBlock + 2, fdsPerBlock + 3});
    }

    //*******************************************************************************************************/

    /**
     * Creates a new file with the specified name.
     *
     * @param symbolicFileName name of the file to be created.
     * @return int              status.
     */
    public int create(final String symbolicFileName) {
        if (symbolicFileName.length() != Directory.FILE_NAME_LENGTH) {
            System.out.println("Create: File name length != " + Directory.FILE_NAME_LENGTH + ". Choose another name for file");
            return STATUS_ERROR;
        } else if (directory.entries.size() == FileSystem.NUMBER_OF_FILE_DESCRIPTORS - 1) {
            System.out.println("Create: Number of files has reached the limit.");
            return STATUS_ERROR;
        }

        int FDIndex = getFreeDescriptorIndex();
        if (FDIndex == -1) {
            // never gets here
            System.out.println("\nCreate: No more space for files.\n");
            return STATUS_ERROR;
        }

        boolean doFileExist = false;
        for (Directory.DirEntry dirEntry : directory.entries) {
            if (dirEntry.file_name.equals(symbolicFileName)) doFileExist = true;
        }
        if (doFileExist) {
            System.out.println("\nCreate: File already exists.\n");
            return STATUS_ERROR;
        }

        try {
            directory.addEntry(symbolicFileName, FDIndex);
        } catch (Exception e) {
            // never gets here
            e.printStackTrace();
        }
        fileDescriptors[FDIndex] = new FileDescriptor();

        System.out.println("\nAfter creating new object:");
        directory.entries.forEach((o) -> System.out.println("FDIndex: " + o.FDIndex + "; File name: " + o.file_name));

        return STATUS_SUCCESS;
    }

    /**
     * Destroys the named file.
     *
     * @param symbolicFileName name of the file to be destroyed.
     * @return int              status.
     */
    public int destroy(String symbolicFileName) {
        int FDIndex = getFileDescriptorIndex(symbolicFileName);
        if (FDIndex == -1) {
            System.out.println("Destroy: File does NOT exist.");
            return STATUS_ERROR;
        }

        // close file if it is open
        int OFTEntryIndex = getOFTEntryIndex(FDIndex);
        if (OFTEntryIndex != -1) {
            close(OFTEntryIndex);
        }

        // clear file blocks on disk
        int[] fileBlocks = fileDescriptors[FDIndex].blockNumbers;
        for (int block : fileBlocks) {
            if (block != -1) {
                try {
                    ioSystem.write_block(block, new byte[64]);
                } catch (Exception e) {
                    // never gets here
                    e.printStackTrace();
                }
                bitmap.set(block, false); // clear bits for now empty blocks
            }
        }

        // remove file from directory
        int dirEntryIndex = getDirectoryEntryIndex(FDIndex);
        directory.entries.remove(dirEntryIndex);

        // clear file descriptor
        fileDescriptors[FDIndex] = null;

        System.out.println("Destroy: File " + symbolicFileName + " is destroyed.");
        return STATUS_SUCCESS;
    }

    /**
     * Opens the named file for reading and writing.
     *
     * @param symbolicFileName name of the file to be opened.
     * @return int                  OFT index.
     */
    public int open(String symbolicFileName) {

        int FDIndex = getFileDescriptorIndex(symbolicFileName);
        if (FDIndex == -1) {
            System.out.println("Open: File does NOT exist.");
            return STATUS_ERROR;
        }

        if (getOFTEntryIndex(FDIndex) != -1) {
            System.out.println("Open: File is already opened.");
            return getOFTEntryIndex(FDIndex);
        }

        int OFTEntryIndex = getFreeOFTEntryIndex();
        if (OFTEntryIndex == -1) {
            System.out.println("Open: Number of open files has reached the limit");
            return STATUS_ERROR;
        }


        OFT.entries[OFTEntryIndex] = new OpenFileTable.OFTEntry();
        OFT.entries[OFTEntryIndex].FDIndex = FDIndex;
        OFT.entries[OFTEntryIndex].currentPosition = 0;

        /**
         * if file is not empty - read first block of file to the buffer in OFT
         */
        if (fileDescriptors[FDIndex].fileLengthInBytes > 0) {
            ByteBuffer temp = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
            try {
                ioSystem.read_block(fileDescriptors[FDIndex].blockNumbers[0], temp);
            } catch (Exception e) {
                // never gets here
                e.printStackTrace();
            }
            OFT.entries[OFTEntryIndex].RWBuffer = temp.array();
        }

        return OFTEntryIndex;
    }

    /**
     * Closes the specified file.
     *
     * @param OFTEntryIndex index of file in OFT.
     * @return int    status.
     */
    public int close(int OFTEntryIndex) {

        if (OFTEntryIndex == 0) {
            System.out.println("Close: Directory can't be closed.");
            return STATUS_ERROR;
        }

        if (checkOFTIndex(OFTEntryIndex) == STATUS_ERROR) return STATUS_ERROR;

        if (fileDescriptors[OFT.entries[OFTEntryIndex].FDIndex].fileLengthInBytes > 0) {
            // write buffer to disk
            OpenFileTable.OFTEntry OFTEntry = OFT.entries[OFTEntryIndex];
            FileDescriptor fileDescriptor = fileDescriptors[OFTEntry.FDIndex];
            int currentFileBlock = OFTEntry.currentPosition / IOSystem.getBlockLengthInBytes();

            if (isPointedToByteAfterLastByte(OFTEntry.FDIndex)) {
                currentFileBlock--;
            }
            int currentDiskBlock = fileDescriptor.blockNumbers[currentFileBlock];

            try {
                ioSystem.write_block(currentDiskBlock, OFT.entries[OFTEntryIndex].RWBuffer);
            } catch (Exception e) {
                // never gets here
                e.printStackTrace();
            }
        }

        System.out.println("Close: File \'" + directory.entries.get(getDirectoryEntryIndex(OFT.entries[OFTEntryIndex].FDIndex)).file_name + "\' is closed.");

        OFT.entries[OFTEntryIndex] = null;
        System.out.println(bitmap);
        return STATUS_SUCCESS;
    }

    /**
     * Sequentially reads a number of bytes from the specified file into main memory.
     * Reading begins with the current position in the file.
     *
     * @param OFTEntryIndex index of file in OFT.
     * @param memArea       starting main memory address.
     * @param count         number of bytes to be read.
     * @return int    number of bytes read.
     */
    public int read(int OFTEntryIndex, ByteBuffer memArea, int count) {
        if (checkOFTIndex(OFTEntryIndex) == STATUS_ERROR || count < 0 || fileDescriptors[OFT.entries[OFTEntryIndex].FDIndex].fileLengthInBytes == 0 || isPointedToByteAfterLastByte(OFT.entries[OFTEntryIndex].FDIndex))
            return STATUS_ERROR;
        if (count == 0) {
            return 0;
        }

        OpenFileTable.OFTEntry OFTEntry = OFT.entries[OFTEntryIndex];

        // find current position inside RWBffer
        int currentBufferPosition = OFTEntry.currentPosition % IOSystem.getBlockLengthInBytes();
        int currentMemoryPosition = 0;

        FileDescriptor fileDescriptor = fileDescriptors[OFTEntry.FDIndex];
        int readCount = 0;

        // read count bytes starting at RWBuffer[currentBufferPosition] to memArea
        for (int i = 0; i < count && i < memArea.array().length; i++) {
            // if end of file -> return number of bytes read
            if (OFTEntry.currentPosition == fileDescriptor.fileLengthInBytes) {
                if (OFTEntry.currentPosition == 0) {
                    System.out.println("Read: File is empty.");
                    break;
                }
                System.out.println("Read: File has ended before " + count + " bytes could be read.");
                break;
            } else {

                // if end of block -> write buffer to the disk, then read next block to RWBuffer
                if (currentBufferPosition == IOSystem.getBlockLengthInBytes()) {
                    int currentFileBlock = OFTEntry.currentPosition / IOSystem.getBlockLengthInBytes();

                    try {
                        ioSystem.write_block(fileDescriptor.blockNumbers[currentFileBlock - 1], OFTEntry.RWBuffer);

                        ByteBuffer temp = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
                        ioSystem.read_block(fileDescriptor.blockNumbers[currentFileBlock], temp);

                        OFT.entries[OFTEntryIndex].RWBuffer = temp.array();

                    } catch (Exception e) {
                        // never gets here
                        e.printStackTrace();
                    }

                    currentBufferPosition = 0;
                }

                // read 1 byte to memory
                memArea.put(currentMemoryPosition, OFTEntry.RWBuffer[currentBufferPosition]);
                // update positions, readCount
                readCount++;
                currentBufferPosition++;
                currentMemoryPosition++;
                OFTEntry.currentPosition++;
            }
        }

        // OFTEntry.currentPosition - points to first byte after last accessed
        System.out.println("Read: " + readCount + " bytes, current position: " + OFTEntry.currentPosition);
        return readCount;
    }

    /**
     * Sequentially writes a number of bytes from main memory into the specified file.
     * Writing begins with the current position in the file.
     *
     * @param OFTEntryIndex index of file in OFT.
     * @param memArea       starting main memory address.
     * @param count         number of bytes to be written.
     * @return int    number of bytes written to file.
     */
    public int write(int OFTEntryIndex, byte[] memArea, int count) {
        if (checkOFTIndex(OFTEntryIndex) == STATUS_ERROR || count < 0) return STATUS_ERROR;
        if (count == 0) {
            return 0;
        }

        OpenFileTable.OFTEntry OFTEntry = OFT.entries[OFTEntryIndex];

        // find current position inside RWBffer
        if (OFTEntry.currentPosition == END_OF_FILE) {
            System.out.println("Write: File is full (current position is at the end of the file.");
            return 0;
        }

        int currentFileBlock = OFTEntry.currentPosition / IOSystem.getBlockLengthInBytes();
        int currentBufferPosition = OFTEntry.currentPosition % IOSystem.getBlockLengthInBytes();

        int currentMemoryPosition = 0;

        FileDescriptor fileDescriptor = fileDescriptors[OFTEntry.FDIndex];
        int writtenCount = 0;

        if (fileDescriptor.fileLengthInBytes == 0 || isPointedToByteAfterLastByte(OFTEntry.FDIndex)) {
            if (isPointedToByteAfterLastByte(OFTEntry.FDIndex)) {
                try {
                    ioSystem.write_block(fileDescriptor.blockNumbers[currentFileBlock - 1], OFTEntry.RWBuffer);
                } catch (Exception e) {
                    // never gets here
                    e.printStackTrace();
                }
                OFTEntry.RWBuffer = new byte[IOSystem.getBlockLengthInBytes()];
            }

            int newBlock = getFreeDataBlockNumber();
            fileDescriptor.blockNumbers[currentFileBlock] = newBlock;
            fileDescriptor.fileLengthInBytes += IOSystem.getBlockLengthInBytes();           // important
            bitmap.set(newBlock, true);
        }

        // write count bytes from memArea to RWBuffer starting at currentBufferPosition
        for (int i = 0; i < count && i < memArea.length; i++) {

            // if end of buffer, check if we can load next block (allocate or read, but previously write that buffer to the disk)
            // меньше 2, ибо если 2 - то это и был последний блок, и выделить новый или загрузить следующий - мы не можем
            if (currentBufferPosition == IOSystem.getBlockLengthInBytes()) {
                if (currentFileBlock < 2) {
                    // OFTEntry.currentPosition == currentFileBlock * IOSystem.getBlockLengthInBytes()

                    // в любом случае пишем буффер на диск
                    int currentDiskBlock = fileDescriptor.blockNumbers[currentFileBlock];

                    try {
                        ioSystem.write_block(currentDiskBlock, OFTEntry.RWBuffer);
                    } catch (Exception e) {
                        // never gets here
                        e.printStackTrace();
                    }

                    currentBufferPosition = 0;

                    currentFileBlock++;
                    currentDiskBlock = fileDescriptor.blockNumbers[currentFileBlock];

                    // check if we should allocate one more block or just read one more block to buffer
                    if (currentDiskBlock == -1) {
                        // блока на диске под следующий блок файла - не было, выделяем новый
                        currentDiskBlock = getFreeDataBlockNumber();
                        fileDescriptor.blockNumbers[currentFileBlock] = currentDiskBlock;
                        fileDescriptor.fileLengthInBytes += IOSystem.getBlockLengthInBytes();               // important
                        bitmap.set(currentDiskBlock, true);

                        OFTEntry.RWBuffer = new byte[IOSystem.getBlockLengthInBytes()];
                    } else {
                        // блок на диске был выделен ранее - читаем его в буфер

                        ByteBuffer temp = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
                        try {
                            ioSystem.read_block(currentDiskBlock, temp);
                        } catch (Exception e) {
                            // never gets here
                            e.printStackTrace();
                        }
                        OFTEntry.RWBuffer = temp.array();
                    }
                } else {
                    System.out.println("Write: No more blocks can be allocated or wrote.");
                    break;
                }
            }

            // write 1 byte to file
            OFTEntry.RWBuffer[currentBufferPosition] = memArea[currentMemoryPosition];

            // update positions, writtenCount
            writtenCount++;
            currentBufferPosition++;
            currentMemoryPosition++;
            OFTEntry.currentPosition++;
        }

        System.out.println("Written: " + writtenCount + " bytes, current position: " + OFTEntry.currentPosition);
        // OFTEntry.currentPosition - points to first byte after last accessed
        System.out.println(fileDescriptor.blockNumbers[0] + " " + fileDescriptor.blockNumbers[1] + " " + fileDescriptor.blockNumbers[2]);
        return writtenCount;
    }

    /**
     * Move the current position of the file to new position.
     * When a file is initially opened, the current position
     * is automatically set to zero. After each read or write
     * operation, it points to the byte immediately following
     * the one that was accessed last. lseek permits the position
     * to be explicitly changed without reading or writing the data.
     * Seeking to position 0 implements a reset command, so that the
     * entire file can be reread or rewritten from the beginning.
     *
     * @param OFTEntryIndex index of file in OFT.
     * @param pos           new position, specifies the number of bytes from the beginning of the file
     * @return int    status.
     */
    public int lseek(int OFTEntryIndex, int pos) {
        System.out.println("in lseek");
        if (checkOFTIndex(OFTEntryIndex) == STATUS_ERROR) return STATUS_ERROR;

        FileDescriptor fileDescriptor = fileDescriptors[OFT.entries[OFTEntryIndex].FDIndex];

        if (pos >= fileDescriptor.fileLengthInBytes || pos < 0) {
            System.out.println("LSeek: New position is out of file.");
            return STATUS_ERROR;
        }

        OpenFileTable.OFTEntry OFTEntry = OFT.entries[OFTEntryIndex];
        int newFileBlock = pos / IOSystem.getBlockLengthInBytes();

        int currentFileBlock = OFTEntry.currentPosition / IOSystem.getBlockLengthInBytes();

        if (isPointedToByteAfterLastByte(OFT.entries[OFTEntryIndex].FDIndex)) currentFileBlock--;

        int currentDiskBlock = fileDescriptor.blockNumbers[currentFileBlock];

        if (newFileBlock != currentFileBlock) {
            // write current block to buffer
            try {
                ioSystem.write_block(currentDiskBlock, OFTEntry.RWBuffer);
            } catch (Exception e) {
                // never gets here
                e.printStackTrace();
            }

            // update current position
            OFTEntry.currentPosition = pos;

            //  read new block to buffer
            int newDiskBlock = fileDescriptor.blockNumbers[newFileBlock];
            ByteBuffer temp = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
            try {
                ioSystem.read_block(newDiskBlock, temp);
            } catch (Exception e) {
                // never gets here
                e.printStackTrace();
            }
            OFTEntry.RWBuffer = temp.array();
        } else {
            OFTEntry.currentPosition = pos;
        }

        System.out.println("LSeek: Current position is " + pos);
        return STATUS_SUCCESS;
    }

    /**
     * Lists the names of all files and their lengths.
     */
    public void directory() {
        for (Directory.DirEntry dirEntry : directory.entries) {
            String fileName = dirEntry.file_name;
            int fileLength = fileDescriptors[dirEntry.FDIndex].fileLengthInBytes;

            System.out.println(fileName + " " + fileLength);
        }
    }

    //*******************************************************************************************************/

    private int getFileDescriptorIndex(String fileName) {
        for (Directory.DirEntry dirEntry : directory.entries) {
            if (dirEntry.file_name.equals(fileName)) return dirEntry.FDIndex;
        }
        return -1;
    }

    private int getDirectoryEntryIndex(int FDIndex) {
        for (int i = 0; i < NUMBER_OF_FILE_DESCRIPTORS - 1; i++) {
            if (directory.entries.get(i) != null && directory.entries.get(i).FDIndex == FDIndex) return i;
        }
        return -1;
    }

    private int getOFTEntryIndex(int FDIndex) {
        for (int i = 1; i < OFT.entries.length; i++) {
            if (OFT.entries[i] != null && OFT.entries[i].FDIndex == FDIndex) return i;
        }
        return -1;
    }

    private int getFreeDescriptorIndex() {
        for (int i = 0; i < NUMBER_OF_FILE_DESCRIPTORS; i++) {
            if (fileDescriptors[i] == null) return i;
        }
        return -1;
    }

    private int getFreeOFTEntryIndex() {
        for (int i = 1; i < 4; i++) {
            if (OFT.entries[i] == null) return i;
        }
        return -1;
    }

    int getFreeDataBlockNumber() {
        for (int i = 8; i < 64; i++) {
            if (!bitmap.get(i)) {
                return i;
            }
        }
        return -1;
    }

    private void writeDataToDisk() {
        // bitmap to disk
        // -1 for empty FD and so on..
        // Directory bytes - to disk blocks
    }

    int checkOFTIndex(int OFTEntryIndex) {
        // ++++++ if open returns STATUS_ERROR => file doesn't exist or it could not be opened
        if (OFTEntryIndex == STATUS_ERROR) {
            System.out.println("File is not opened or does not exist.");
            return STATUS_ERROR;
        }

        if (OFTEntryIndex <= 0 || OFTEntryIndex >= OFT.entries.length || OFT.entries[OFTEntryIndex] == null) {
            System.out.println("File is not opened.");
            return STATUS_ERROR;
        }
        return STATUS_SUCCESS;
    }

    boolean isPointedToByteAfterLastByte(int FDIndex) {
        return ((fileDescriptors[FDIndex].fileLengthInBytes != 0) && (OFT.entries[getOFTEntryIndex(FDIndex)].currentPosition == fileDescriptors[FDIndex].fileLengthInBytes));
    }

    //*******************************************************************************************************/

    private void initFileSystemFromFile(String fileName) {
        initDiskFromFile(fileName);
        initFileSystemFromDisk();
    }

    public void initDiskFromFile(String fileName) {

        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(new FileInputStream(fileName));
        } catch (IOException e) {
            // gets here when errors with files
            e.printStackTrace();
        }

        try {
            ioSystem.ldisk = (LDisk) objectInputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void initFileSystemFromDisk() {
        try {
            initBitmapFromDisk();
            initFileDescriptorsFromDisk();
            initDirectoryFromDisk();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initBitmapFromDisk() throws Exception {
        bitmap = new BitSet(64);

        ByteBuffer block = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
        ioSystem.read_block(0, block);
        String string;
//        = new StringBuilder();
        for (int i = 0; i < 16; i++) {
//            stringBuilder.delete(0, stringBuilder.length());
            string = Integer.toBinaryString(block.getInt());

            for (int j = 0; j < string.length(); j++) {
                if (string.charAt(j) == '1') bitmap.set(j, true);
            }
        }

        System.out.println(bitmap);
    }

    private void initFileDescriptorsFromDisk() throws Exception {
        int numberOfFileDescriptorsInOneBlock = IOSystem.getBlockLengthInBytes() / 16;
        int numberOfBlocksForFileDescriptors = NUMBER_OF_FILE_DESCRIPTORS * 16 / IOSystem.getBlockLengthInBytes();

        ByteBuffer block;
        fileDescriptors = new FileDescriptor[NUMBER_OF_FILE_DESCRIPTORS];
        int fileLengthInBytes;
        int[] blockNumbers;

        for (int i = 0; i < numberOfBlocksForFileDescriptors; i++) {
            block = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
            ioSystem.read_block(i + 1, block);
            for (int j = 0; j < numberOfFileDescriptorsInOneBlock; j++) {
                fileLengthInBytes = block.getInt();
                if (fileLengthInBytes == -1) {
                    // continue;
                } else {
                    blockNumbers = new int[FileDescriptor.MAX_NUMBER_OF_BLOCKS];
                    blockNumbers[0] = block.getInt();
                    blockNumbers[1] = block.getInt();
                    blockNumbers[2] = block.getInt();
                    fileDescriptors[i * numberOfFileDescriptorsInOneBlock + j] = new FileDescriptor(fileLengthInBytes, blockNumbers);
                }
            }
        }
    }

    private void initDirectoryFromDisk() throws Exception {
        ByteBuffer block;
        StringBuilder fileName = new StringBuilder();
        int FDIndex;
        byte b;
        int numberOfEntriesInOneBlock = IOSystem.getBlockLengthInBytes() / 8;
        directory.entries.clear();

        boolean reading = true;
        for (int i = 0; i < FileDescriptor.MAX_NUMBER_OF_BLOCKS && reading; i++) {
            block = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
            ioSystem.read_block(5 + i, block);
            for (int j = 0; j < numberOfEntriesInOneBlock; j++) {
                b = block.get();
                if (b == 0) {
                    reading = false;
                    break;
                } else {
                    fileName.delete(0, fileName.length());
                    fileName.append((char) b);
                    fileName.append((char) block.get());
                    fileName.append((char) block.get());
                    fileName.append((char) block.get());
                    FDIndex = block.getInt();

                    directory.addEntry(fileName.toString(), FDIndex);
                }
            }
        }
    }


    public void saveFileSystemToFile(String fileName) {
        saveFileSystemToDisk();
        saveDiskToFile(fileName);
    }

    private void saveFileSystemToDisk() {
        // close all open files except for directory
        for (int i = 1; i < OFT.entries.length; i++) {
            if (OFT.entries[i] != null)
                close(i);
        }

        try {
            writeBitmapToDisk();
            writeFileDescriptorsToDisk();
            writeDirectoryToDisk();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveDiskToFile(String fileName) {
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(new FileOutputStream(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            objectOutputStream.writeObject(ioSystem.ldisk);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] bitsetToByteArray(BitSet bits) {
        byte[] bytes = new byte[64];
        for (int i = 0; i < bits.size(); i++) {
            if (bits.get(i)) {
                bytes[i] |= 1 << (i % 8);
            }
        }
        return bytes;
    }

    private void writeBitmapToDisk() throws Exception {
        ioSystem.write_block(0, bitsetToByteArray(bitmap));
    }

    private void writeFileDescriptorsToDisk() throws Exception {
        int numberOfFileDescriptorsInOneBlock = IOSystem.getBlockLengthInBytes() / 16;
        int numberOfBlocksForFileDescriptors = NUMBER_OF_FILE_DESCRIPTORS * 16 / IOSystem.getBlockLengthInBytes();

        ByteBuffer block;

        for (int i = 0; i < numberOfBlocksForFileDescriptors; i++) {
            block = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
            for (int j = 0; j < numberOfFileDescriptorsInOneBlock; j++) {
                int currentDescriptor = i * numberOfFileDescriptorsInOneBlock + j;
                if (fileDescriptors[currentDescriptor] == null) {
                    block.putInt(-1);
                    block.putInt(-1);
                    block.putInt(-1);
                    block.putInt(-1);
                } else {
                    block.putInt(fileDescriptors[currentDescriptor].fileLengthInBytes);

                    block.putInt(fileDescriptors[currentDescriptor].blockNumbers[0]);
                    block.putInt(fileDescriptors[currentDescriptor].blockNumbers[1]);
                    block.putInt(fileDescriptors[currentDescriptor].blockNumbers[2]);
                }
            }
            ioSystem.write_block(i + 1, block.array());
        }
    }

    private void writeDirectoryToDisk() throws Exception {
        ByteBuffer block = null;
        int numberOfEntriesInOneBlock = IOSystem.getBlockLengthInBytes() / 8;
        int currentDirectoryBlock = 0;

        for (int i = 0; i < directory.entries.size(); i++) {
            if (block == null) {
                block = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
            }
            String fileName = directory.entries.get(i).file_name;
            for (int k = 0; k < fileName.length(); k++) {
                block.put((byte) fileName.charAt(k));
            }
            block.putInt(directory.entries.get(i).FDIndex);

            if ((i + 1) % numberOfEntriesInOneBlock == 0) {
                ioSystem.write_block(5 + currentDirectoryBlock, block.array());
                currentDirectoryBlock++;
                block = null;
            }
        }
        if (block == null) {
            block = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
        }
        block.put((byte) 0);
        ioSystem.write_block(5 + currentDirectoryBlock, block.array());
    }
}
