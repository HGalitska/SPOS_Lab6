package filesystem;

import disk.LDisk;
import iosystem.IOSystem;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.BitSet;

public class FileSystem {

    final static int NUMBER_OF_FILE_DESCRIPTORS = 16;
    public final static int STATUS_SUCCESS = 1;
    public final static int STATUS_ERROR = -3;
    private final static int END_OF_FILE = 3 * IOSystem.getBlockLengthInBytes();

    private IOSystem ioSystem;

    OpenFileTable OFT;
    private BitSet bitmap;
    private Directory directory;
    private FileDescriptor[] fileDescriptors;


    public FileSystem(IOSystem ioSystem) {
        if (ioSystem == null) throw new IllegalArgumentException("IOSystem should NOT be NULL");
        this.ioSystem = ioSystem;

        initBasicStructures();

        initEmptyDisk();
    }

    public FileSystem(IOSystem ioSystem, String fileName) {
        if (ioSystem == null) throw new IllegalArgumentException("IOSystem should NOT be NULL");
        this.ioSystem = ioSystem;

        initBasicStructures();

        initFileSystemFromFile(fileName);
    }


    private void initBasicStructures() {

        // create open file table
        OFT = new OpenFileTable();
        OFT.entries[0] = new OpenFileTable.OFTEntry();

        // add first file (directory) as open file to OFT
        OFT.entries[0].FDIndex = 0;

        // create bitmap, set 1 bit for it + 4 for file descriptors + 3 for directory
        bitmap = new BitSet(64);
        bitmap.set(0, 8, true);

        // create file descriptors and directory
        fileDescriptors = new FileDescriptor[NUMBER_OF_FILE_DESCRIPTORS];
        directory = new Directory();
    }

    private void initEmptyDisk() {
        int fdsPerBlock = 16 * NUMBER_OF_FILE_DESCRIPTORS / IOSystem.getBlockLengthInBytes();
        // init directory
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
            return STATUS_ERROR;
        } else if (directory.entries.size() == FileSystem.NUMBER_OF_FILE_DESCRIPTORS - 1) {
            return STATUS_ERROR;
        }

        int FDIndex = getFreeDescriptorIndex();
        if (FDIndex == -1) {
            return STATUS_ERROR;
        }

        boolean doFileExist = false;
        for (Directory.DirEntry dirEntry : directory.entries) {
            if (dirEntry.file_name.equals(symbolicFileName)) doFileExist = true;
        }
        if (doFileExist) {
            return STATUS_ERROR;
        }

        try {
            directory.addEntry(symbolicFileName, FDIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileDescriptors[FDIndex] = new FileDescriptor();

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
                    e.printStackTrace();
                }
                // clear bits of bitmap for now empty blocks
                bitmap.set(block, false);
            }
        }

        // remove file from directory
        int dirEntryIndex = getDirectoryEntryIndex(FDIndex);
        directory.entries.remove(dirEntryIndex);

        // clear file descriptor
        fileDescriptors[FDIndex] = null;

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
            return STATUS_ERROR;
        }

        if (getOFTEntryIndex(FDIndex) != -1) {
            System.out.println("File has been already opened.");
            return getOFTEntryIndex(FDIndex);
        }

        int OFTEntryIndex = getFreeOFTEntryIndex();
        if (OFTEntryIndex == -1) {
            return STATUS_ERROR;
        }


        OFT.entries[OFTEntryIndex] = new OpenFileTable.OFTEntry();
        OFT.entries[OFTEntryIndex].FDIndex = FDIndex;
        OFT.entries[OFTEntryIndex].currentPosition = 0;

        // if file is not empty - read first block of file to the buffer in OFT
        if (fileDescriptors[FDIndex].fileLengthInBytes > 0) {
            ByteBuffer temp = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
            try {
                ioSystem.read_block(fileDescriptors[FDIndex].blockNumbers[0], temp);
                OFT.entries[OFTEntryIndex].fileBlockInBuffer = 0;
            } catch (Exception e) {
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
            return STATUS_ERROR;
        }

        if (checkOFTIndex(OFTEntryIndex) == STATUS_ERROR) return STATUS_ERROR;

        OpenFileTable.OFTEntry OFTEntry = OFT.entries[OFTEntryIndex];

        if (fileDescriptors[OFTEntry.FDIndex].fileLengthInBytes > 0 && OFTEntry.bufferModified) {
            // write buffer to disk

            FileDescriptor fileDescriptor = fileDescriptors[OFTEntry.FDIndex];

            // determine current block by block in buffer
            int currentFileBlock = OFTEntry.fileBlockInBuffer;
            if (isPointedToByteAfterLastByte(OFTEntry.FDIndex)) {
                currentFileBlock--;
            }
            int currentDiskBlock = fileDescriptor.blockNumbers[currentFileBlock];

            try {
                ioSystem.write_block(currentDiskBlock, OFT.entries[OFTEntryIndex].RWBuffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        OFT.entries[OFTEntryIndex] = null;
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
        if (checkOFTIndex(OFTEntryIndex) == STATUS_ERROR || count < 0)
            return STATUS_ERROR;

        if (count == 0) {
            return 0;
        }

        OpenFileTable.OFTEntry OFTEntry = OFT.entries[OFTEntryIndex];
        FileDescriptor fileDescriptor = fileDescriptors[OFTEntry.FDIndex];

        if (isPointedToByteAfterLastByte(OFTEntry.FDIndex) || fileDescriptor.fileLengthInBytes == 0) {
            return STATUS_ERROR;
        }

        if (writeOldBuffer(OFTEntry, fileDescriptor) == STATUS_ERROR) return STATUS_ERROR;

        // find current position inside RWBuffer
        int currentBufferPosition = OFTEntry.currentPosition % IOSystem.getBlockLengthInBytes();
        int currentMemoryPosition = 0;

        int readCount = 0;

        // read count bytes starting at RWBuffer[currentBufferPosition] to memArea
        for (int i = 0; i < count && i < memArea.array().length; i++) {
            // if end of file -> return number of bytes read
            if (OFTEntry.currentPosition == fileDescriptor.fileLengthInBytes) {
                break;
            } else {
                // if end of block -> write buffer to the disk, then read next block to RWBuffer
                if (currentBufferPosition == IOSystem.getBlockLengthInBytes()) {

                    writeOldBuffer(OFTEntry, fileDescriptor);

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
        FileDescriptor fileDescriptor = fileDescriptors[OFTEntry.FDIndex];


        if (OFTEntry.currentPosition == END_OF_FILE) {
            return 0;
        }

        if (writeOldBuffer(OFTEntry, fileDescriptor) == STATUS_ERROR) return STATUS_ERROR;

        // find current position inside RWBffer
        int currentBufferPosition = OFTEntry.currentPosition % IOSystem.getBlockLengthInBytes();
        int currentMemoryPosition = 0;

        int writtenCount = 0;

        if (fileDescriptor.fileLengthInBytes == 0) {
            int newBlock = getFreeDataBlockNumber();
            OFTEntry.fileBlockInBuffer = 0;
            fileDescriptor.blockNumbers[OFTEntry.fileBlockInBuffer] = newBlock;
            fileDescriptor.fileLengthInBytes += IOSystem.getBlockLengthInBytes();
            bitmap.set(newBlock, true);
        }

        // write count bytes from memArea to RWBuffer starting at currentBufferPosition
        for (int i = 0; i < count && i < memArea.length; i++) {

            // if end of buffer, check if we can load next block (allocate or read, but previously write that buffer to the disk)
            if (currentBufferPosition == IOSystem.getBlockLengthInBytes()) {
                if (OFTEntry.fileBlockInBuffer < 2) {
                    currentBufferPosition = 0;
                    writeOldBuffer(OFTEntry, fileDescriptor);
                } else {
                    break;
                }
            }

            // write 1 byte to file
            OFTEntry.RWBuffer[currentBufferPosition] = memArea[currentMemoryPosition];
            OFTEntry.bufferModified = true;

            // update positions, writtenCount
            writtenCount++;
            currentBufferPosition++;
            currentMemoryPosition++;
            OFTEntry.currentPosition++;
        }

        // OFTEntry.currentPosition - points to first byte after last accessed
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
        if (checkOFTIndex(OFTEntryIndex) == STATUS_ERROR) return STATUS_ERROR;

        FileDescriptor fileDescriptor = fileDescriptors[OFT.entries[OFTEntryIndex].FDIndex];

        if (pos > fileDescriptor.fileLengthInBytes || pos < 0) {
            return STATUS_ERROR;
        }

        OFT.entries[OFTEntryIndex].currentPosition = pos;

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

    private int getFreeDataBlockNumber() {
        for (int i = 8; i < 64; i++) {
            if (!bitmap.get(i)) {
                return i;
            }
        }
        return -1;
    }

    private int checkOFTIndex(int OFTEntryIndex) {
        // if open returns STATUS_ERROR => file doesn't exist or it could not be opened
        if (OFTEntryIndex == STATUS_ERROR) {
            return STATUS_ERROR;
        }

        if (OFTEntryIndex <= 0 || OFTEntryIndex >= OFT.entries.length || OFT.entries[OFTEntryIndex] == null) {
            return STATUS_ERROR;
        }
        return STATUS_SUCCESS;
    }

    private boolean isPointedToByteAfterLastByte(int FDIndex) {
        int fileLength = fileDescriptors[FDIndex].fileLengthInBytes;
        int position = OFT.entries[getOFTEntryIndex(FDIndex)].currentPosition;

        boolean fileNotEmpty = (fileLength != 0);
        boolean positionOutOfFile = (position == fileLength);
        return (fileNotEmpty && positionOutOfFile);
    }

    //*******************************************************************************************************/

    private void initFileSystemFromFile(String fileName) {
        initDiskFromFile(fileName);
        initFileSystemFromDisk();
    }

    private void initDiskFromFile(String fileName) {

        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(new FileInputStream(fileName));
        } catch (IOException e) {
            // gets here when errors with files
            e.printStackTrace();
        }

        try {
            ioSystem.setLdisk((LDisk) objectInputStream.readObject());
        } catch (ClassNotFoundException | IOException  e) {
            e.printStackTrace();
        }
    }

    private void initFileSystemFromDisk() {
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

        for (int i = 0; i < 2; i++) {
            string = Integer.toBinaryString(block.getInt());

            for (int j = 0; j < string.length(); j++) {
                if (string.charAt(j) == '1') bitmap.set(i * 32 + j, true);
            }
        }

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
                    block.getInt();
                    block.getInt();
                    block.getInt();
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

    //*******************************************************************************************************/

    private int writeOldBuffer(OpenFileTable.OFTEntry OFTEntry, FileDescriptor fileDescriptor) {

        if (OFTEntry.fileBlockInBuffer == -1) return STATUS_SUCCESS;
        // if buffer holds different block
        if (OFTEntry.fileBlockInBuffer != (OFTEntry.currentPosition / IOSystem.getBlockLengthInBytes())) {
            if (OFTEntry.bufferModified) {
                int diskBlock = fileDescriptor.blockNumbers[OFTEntry.fileBlockInBuffer];
                try {
                    ioSystem.write_block(diskBlock, OFTEntry.RWBuffer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                int newFileBlock = OFTEntry.currentPosition / IOSystem.getBlockLengthInBytes();

                if (fileDescriptor.blockNumbers[newFileBlock] == -1) {
                    int newDiskBlock = getFreeDataBlockNumber();
                    if (newDiskBlock == -1) {
                        return STATUS_ERROR;
                    }
                    fileDescriptor.blockNumbers[newFileBlock] = newDiskBlock;
                    fileDescriptor.fileLengthInBytes += IOSystem.getBlockLengthInBytes();
                    bitmap.set(newDiskBlock, true);
                }

                ByteBuffer temp = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
                ioSystem.read_block(fileDescriptor.blockNumbers[newFileBlock], temp);
                OFTEntry.RWBuffer = temp.array();
                OFTEntry.bufferModified = false;
                OFTEntry.fileBlockInBuffer = newFileBlock;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return STATUS_SUCCESS;
    }

    //*******************************************************************************************************/

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
            objectOutputStream.writeObject(ioSystem.getLdisk());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] binaryStringToBytes(String data) {
        byte[] temp = new BigInteger(data, 2).toByteArray();
        byte[] output = new byte[64];
        for (int i = 0; i < 64; i++) {
            output[i] = temp[i + 1];
        }
        return output;
    }

    private static byte[] bitsetToByteArray(BitSet bits) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < bits.size(); i++) {
            if (bits.get(i)) {
                stringBuilder.append('1');
            } else {
                stringBuilder.append('0');
            }
        }

        for (int i = 0; i < 448; i++) {
            stringBuilder.append('0');
        }

        return binaryStringToBytes(stringBuilder.toString());
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
