package filesystem;

import iosystem.IOSystem;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class FileSystem {

    final static int NUMBER_OF_FILE_DESCRIPTORS = 16;
    private final static int STATUS_SUCCESS = 1;
    private final static int STATUS_ERROR = -3;

    private IOSystem ioSystem;

    private OpenFileTable OFT;
    BitSet bitmap; // made package-public for testing purposes
    private Directory directory;
    private FileDescriptor[] fileDescriptors;

    FileSystem(IOSystem ioSystem, boolean isDiskEmpty) {
        if (ioSystem == null) throw new IllegalArgumentException("IOSystem should NOT be NULL");
        OFT = new OpenFileTable();

        bitmap = new BitSet(64);
        bitmap.set(0, 8, true); // set 1 bit for bitmap + 4 for file descriptors + 3 for dir

        this.ioSystem = ioSystem;
        fileDescriptors = new FileDescriptor[NUMBER_OF_FILE_DESCRIPTORS];
        directory = new Directory();

        if (isDiskEmpty)
            initEmptyDisk();
//        else initFileSystemFromDisk();
    }

    private void initEmptyDisk() {
        bitmap.set(8, 64, false); // all data blocks are empty

        int fdsPerBlock = 16 * NUMBER_OF_FILE_DESCRIPTORS / IOSystem.getBlockLengthInBytes();
        // init directory and add it to OFT
        fileDescriptors[0] = new FileDescriptor(0, new int[]{fdsPerBlock + 1, fdsPerBlock + 2, fdsPerBlock + 3});
        OFT.entries[0] = new OpenFileTable.OFTEntry();
        OFT.entries[0].FDIndex = 0;
    }

    //*******************************************************************************************************/

    /**
     * Creates a new file with the specified name.
     *
     * @param symbolicFileName name of the file to be created.
     * @return int              status.
     */
    int create(final String symbolicFileName) throws Exception {
        int FDIndex = getFreeDescriptorIndex();
        if (FDIndex == -1) {
            System.out.println("\nNo more space for files.\n");
            return STATUS_ERROR;
        }

        boolean doFileExist = false;
        for (Directory.DirEntry dirEntry : directory.entries) {
            if (dirEntry.file_name.equals(symbolicFileName)) doFileExist = true;
        }
        if (doFileExist) {
            System.out.println("\nFile already exists.\n");
            return STATUS_ERROR;
        }

        directory.addEntry(symbolicFileName, FDIndex);
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
    int destroy(String symbolicFileName) throws Exception {
        int FDIndex = getFileDescriptorIndex(symbolicFileName);
        if (FDIndex == -1) {
            System.out.println("Destroy: file does NOT exist.");
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
                ioSystem.write_block(block, new byte[64]);
                bitmap.set(block, false); // clear bits for now empty blocks
            }
        }

        // remove file from directory
        int dirEntryIndex = getDirectoryEntryIndex(FDIndex);
        directory.entries.remove(dirEntryIndex);

        // clear file descriptor
        fileDescriptors[FDIndex] = null;

        System.out.println("Destroy: file " + symbolicFileName + " is destroyed.");
        return STATUS_SUCCESS;
    }

    /**
     * Opens the named file for reading and writing.
     *
     * @param symbolicFileName name of the file to be opened.
     * @return int                  OFT index.
     */
    int open(String symbolicFileName) throws Exception {

        int FDIndex = getFileDescriptorIndex(symbolicFileName);
        if (FDIndex == -1) {
            System.out.println("Open: file does NOT exist.");
            return STATUS_ERROR;
        }

        if (getOFTEntryIndex(FDIndex) != -1) {
            System.out.println("Open: file is already opened.");
            return getOFTEntryIndex(FDIndex);
        }

        int OFTEntryIndex = getFreeOFTEntryIndex();
        if (OFTEntryIndex == -1) {
            System.out.println("Open: number of open files has reached the limit");
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
            ioSystem.read_block(fileDescriptors[FDIndex].blockNumbers[0], temp);
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
    int close(int OFTEntryIndex) throws Exception {

        if (OFTEntryIndex == 0) {
            System.out.println("Directory can't be closed.");
            return STATUS_ERROR;
        }

        if (checkOFTIndex(OFTEntryIndex) == STATUS_ERROR) return STATUS_ERROR;

        // write buffer to disk
        OpenFileTable.OFTEntry OFTEntry = OFT.entries[OFTEntryIndex];
        FileDescriptor fileDescriptor = fileDescriptors[OFTEntry.FDIndex];
        int currentFileBlock = OFTEntry.currentPosition / IOSystem.getBlockLengthInBytes();
        if (OFTEntry.currentPosition == 192) {
            currentFileBlock--;
        }
        int currentDiskBlock = fileDescriptor.blockNumbers[currentFileBlock];

        if (currentDiskBlock != -1)
            ioSystem.write_block(currentDiskBlock, OFT.entries[OFTEntryIndex].RWBuffer);
            bitmap.set(currentDiskBlock, true);

        OFT.entries[OFTEntryIndex] = null;

        System.out.println("Close: file is closed.");
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
    int read(int OFTEntryIndex, ByteBuffer memArea, int count) throws Exception {
        if (checkOFTIndex(OFTEntryIndex) == STATUS_ERROR) return STATUS_ERROR;
        if (count == 0 || count >= memArea.array().length) {
            return 0;
        }

        OpenFileTable.OFTEntry OFTEntry = OFT.entries[OFTEntryIndex];

        // find current position inside RWBffer
        int currentPosition = OFTEntry.currentPosition;
        int currentFileBlock = currentPosition / IOSystem.getBlockLengthInBytes();
        int currentBufferPosition = currentPosition - currentFileBlock * IOSystem.getBlockLengthInBytes();
        int currentMemoryPosition = 0;

        FileDescriptor fileDescriptor = fileDescriptors[OFTEntry.FDIndex];
        int readCount = 0;

        // read count bytes starting at RWBuffer[currentBufferPosition] to memArea
        for (int i = 0; i < count; i++) {
            // if end of file -> return number of bytes read
            if (currentPosition >= fileDescriptor.fileLengthInBytes) {
                if (currentPosition == 0) {
                    System.out.println("Read: File is empty.");
                    break;
                }
                System.out.println("Read: file has ended before " + count + " bytes could be read.");
                break;
            }

            // if end of block -> read next block to RWBuffer
            if (currentBufferPosition == IOSystem.getBlockLengthInBytes()) {
                currentFileBlock = currentPosition / IOSystem.getBlockLengthInBytes();

                ByteBuffer temp = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
                ioSystem.read_block(currentFileBlock, temp);
                OFT.entries[OFTEntryIndex].RWBuffer = temp.array();

                currentBufferPosition = 0;
            }

            // read 1 byte to memory
            memArea.put(currentMemoryPosition, OFTEntry.RWBuffer[currentBufferPosition]);
            // update positions, readCount
            readCount++;
            currentBufferPosition++;
            currentMemoryPosition++;
            currentPosition++; // = OFTEntry.currentPosition + readCount
        }

        OFTEntry.currentPosition = currentPosition; // points to first byte after last accessed
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
    int write(int OFTEntryIndex, byte[] memArea, int count) throws Exception {
        if (checkOFTIndex(OFTEntryIndex) == STATUS_ERROR) return STATUS_ERROR;
        if (count == 0 || count >= memArea.length) {
            return 0;
        }

        OpenFileTable.OFTEntry OFTEntry = OFT.entries[OFTEntryIndex];

        // find current position inside RWBffer
        int currentPosition = OFTEntry.currentPosition;
        if (currentPosition == 192) {
            System.out.println("Write: File is full");
            return 0;
        }
        int currentFileBlock = currentPosition / IOSystem.getBlockLengthInBytes();
        int currentBufferPosition = currentPosition - currentFileBlock * IOSystem.getBlockLengthInBytes();
        int currentMemoryPosition = 0;

        FileDescriptor fileDescriptor = fileDescriptors[OFTEntry.FDIndex];
        int writtenCount = 0;

        if (fileDescriptor.fileLengthInBytes == 0){
            int newBlock = getFreeDataBlockNumber();
            fileDescriptor.blockNumbers[currentFileBlock] = newBlock;
            bitmap.set(newBlock, true);
        }

        // write count bytes from memArea to RWBuffer starting at currentBufferPosition
        for (int i = 0; i < count; i++) {

            //if end of file -> allocate new block if possible
            if (currentPosition >= currentFileBlock * IOSystem.getBlockLengthInBytes() && fileDescriptor.blockNumbers[currentFileBlock] == -1) {
                if (currentFileBlock < 3) {
                    int newBlock = getFreeDataBlockNumber();
                    fileDescriptor.blockNumbers[currentFileBlock] = newBlock;
                } else {
                    System.out.println("Write: no more blocks can be allocated.");
                    break;
                }
            }

            // write 1 byte to file
            OFTEntry.RWBuffer[currentBufferPosition] = memArea[currentMemoryPosition];
            fileDescriptor.fileLengthInBytes++;

            // update positions, writtenCount
            writtenCount++;
            currentBufferPosition++;
            currentMemoryPosition++;
            currentPosition++;

            // if end of block -> write buffer to disk, set bufferPosition to 0
            if (currentBufferPosition == IOSystem.getBlockLengthInBytes()) {

                int currentDiskBlock = fileDescriptor.blockNumbers[currentFileBlock];

                ioSystem.write_block(currentDiskBlock, OFTEntry.RWBuffer);
                System.out.println("Written: " + writtenCount + " bytes, current position: " + currentPosition);

                bitmap.set(currentDiskBlock, true);
                System.out.println(bitmap);

                currentFileBlock++;
                currentBufferPosition = 0;
            }
        }

        OFTEntry.currentPosition = currentPosition; // points to first byte after last accessed
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
    int lseek(int OFTEntryIndex, int pos) throws Exception {
        if (checkOFTIndex(OFTEntryIndex) == STATUS_ERROR) return STATUS_ERROR;

        FileDescriptor fileDescriptor = fileDescriptors[OFT.entries[OFTEntryIndex].FDIndex];

        if (pos > fileDescriptor.fileLengthInBytes || pos < 0) {
            System.out.println("LSeek: New position is out of file.");
            return STATUS_ERROR;
        }

        OpenFileTable.OFTEntry OFTEntry = OFT.entries[OFTEntryIndex];
        int newFileBlock = pos / IOSystem.getBlockLengthInBytes();

        int currentFileBlock = OFTEntry.currentPosition / IOSystem.getBlockLengthInBytes();
        int currentDiskBlock = fileDescriptor.blockNumbers[currentFileBlock];

        if (newFileBlock != currentFileBlock) {
            // write current block to buffer
            ioSystem.write_block(currentDiskBlock, OFTEntry.RWBuffer);

            // update current position
            OFTEntry.currentPosition = pos;

            //  read new block to buffer
            int newDiskBlock = fileDescriptor.blockNumbers[newFileBlock];
            ByteBuffer temp = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
            ioSystem.read_block(newDiskBlock, temp);
            OFTEntry.RWBuffer = temp.array();
        } else {
            OFTEntry.currentPosition = pos;
        }

        System.out.println("LSeek: set current position to " + pos);
        return STATUS_SUCCESS;
    }

    /**
     * Lists the names of all files and their lengths.
     */
    void directory() {
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

        if (OFTEntryIndex >= OFT.entries.length || OFT.entries[OFTEntryIndex] == null) {
            System.out.println("File is not opened.");
            return STATUS_ERROR;
        }
        return STATUS_SUCCESS;
    }
//public void initDiskFromFile() {
//
//}


    //    public void initFileSystemFromDisk() throws Exception {
//        initBitmapFromDisk();
//        initFileDescriptorsFromDisk();
//        initDirectoryFromDisk();
//
//
//        // should write directory to OFT
//    }
//
//    private void initDirectoryFromDisk() throws Exception {
////        ByteBuffer emptyBlock = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
//
//        OFT.entries[0].FDIndex = 0;
//
////        for (int i = 0; i < IOSystem.getBlockLengthInBytes(); i++) {
////            emptyBlock.put(i, (byte) 0);
////        }
//        ByteBuffer directoryBlocks = ByteBuffer.allocate(8 * (NUMBER_OF_FILE_DESCRIPTORS - 1));
//
//        // до 2 ибо директория занимает 1.85 blocks - максимум
//        for (int i = 0; i < 2; i++) {
//            ByteBuffer tempBlock = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
//            ioSystem.read_block(16 * NUMBER_OF_FILE_DESCRIPTORS / IOSystem.getBlockLengthInBytes() + 1 + i, temp);
////            if (tempBlock.compareTo(emptyBlock) == 0) break;
//
//            int fileName = tempBlock.getInt();
//            int FDIndex = tempBlock.getInt();
//
//
//        }
//    }
//
//    private void initBitmapFromDisk() throws Exception {
//        ByteBuffer block = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes())
//        ioSystem.read_block(0, block);
//        for (int i = 0; i < 2; i++) {
//            bitmap[i] = block.getInt();
//        }
//
//        byte[] bytes = ByteBuffer.allocate(4).putInt(bitmap[0]).array();
//        printBits(bytes);
//
//        bytes = ByteBuffer.allocate(4).putInt(bitmap[1]).array();
//        printBits(bytes);
//    }
//
//    private static void printBits(byte[] data) {
//        for (int ll = 0; ll < data.length; ll++) {
//            System.out.print(String.format("%8s", Integer.toBinaryString(data[ll] & 0xFF)).replace(' ', '0'));
//        }
//        System.out.println();
//    }
//
//    private int initFileDescriptorsFromDisk() throws Exception {
//        int numberOfExistFiles = 0;
//
//        ByteBuffer block = ByteBuffer.allocate(IOSystem.getBlockLengthInBytes());
//
//        // if params changes - all arithmetical operations should be done with cast to double
//        // FileDescriptor - 4 int; 4 fd on 1 block. if file name (first int in fd) is empty - fd is empty
//        for (int i = 1 + NUMBER_OF_FILE_DESCRIPTORS * 16 / IOSystem.getBlockLengthInBytes() + 3; i < 16 * NUMBER_OF_FILE_DESCRIPTORS / IOSystem.getBlockLengthInBytes() + 1; i++) {
//            ioSystem.read_block(i, block);
//            for (int j = 0; j < 4; j++) {
//                byte[] bytes = ByteBuffer.allocate(4).putInt(bitmap[0]).array();
//            }
//        }
//
//
//        // add directory as first file and FD
//        fileDescriptors.addFirst(new FileDescriptor(numberOfExistFiles * 8, new int[]{16 * NUMBER_OF_FILE_DESCRIPTORS / IOSystem.getBlockLengthInBytes() + 1, 16 * NUMBER_OF_FILE_DESCRIPTORS / IOSystem.getBlockLengthInBytes() + 2, 16 * NUMBER_OF_FILE_DESCRIPTORS / IOSystem.getBlockLengthInBytes() + 3}));     //
//
//
//        return fileDescriptors.size();
//    }
}
