package filesystem;

import iosystem.IOSystem;

import java.nio.ByteBuffer;

public class FileSystem {

    final static int NUMBER_OF_FILE_DESCRIPTORS = 16;
    final static int STATUS_SUCCESS = 1;
    final static int STATUS_ERROR = -3;

    private IOSystem ioSystem;

    private OpenFileTable OFT;
    private int[] bitmap;
    private Directory directory;
    private FileDescriptor[] fileDescriptors;

    public FileSystem(IOSystem ioSystem, boolean isDiskEmpty) throws Exception {
        if (ioSystem == null) throw new IllegalArgumentException("IOSystem should NOT be NULL");
        OFT = new OpenFileTable();
        bitmap = new int[2];
        this.ioSystem = ioSystem;
        fileDescriptors = new FileDescriptor[NUMBER_OF_FILE_DESCRIPTORS];
        directory = new Directory();

        if (isDiskEmpty)
            initEmptyDisk();
//        else initFileSystemFromDisk();
    }

    private void initEmptyDisk() {
        int fdsPerBlock = 16 * NUMBER_OF_FILE_DESCRIPTORS / IOSystem.getBlockLengthInBytes();

        // init directory and add it to OFT
        fileDescriptors[0] = new FileDescriptor(0, new int[]{fdsPerBlock + 1, fdsPerBlock + 2, fdsPerBlock + 3});
        OFT.entries[0] = new OpenFileTable.OFTEntry();
        OFT.entries[0].FDIndex = 0;
    }

    private void writeDataToDisk() {
        // bitmap to disk
        // -1 for empty FD and so on..
        // Directory bytes - to disk blocks
    }

    /**
     * Creates a new file with the specified name.
     *
     * @param symbolicFileName name of the file to be created.
     * @return int              status.
     */
    int create(final String symbolicFileName) throws Exception {
        int FDIndex = findEmptyDescriptor();
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
        // !!!!!!!! write new entries to directory blocks -- with write(0, dirEntryBytes) ?
        fileDescriptors[FDIndex] = new FileDescriptor();

        System.out.println("\nAfter creating new object:");
        directory.entries.forEach((o) -> System.out.println("FDIndex: " + o.FDIndex + "; File name: " + o.file_name));

//        /**
//         * if first block of directory is full -> write it to disk, allocate second block
//         */
//        if (directory.entries.size() == 8) {
//            ioSystem.write_block(fileDescriptors[0].blockNumbers[0], OFT.entries[0].RWBuffer);
//            System.out.println("Create: written first directory block to disc.");
//            //fileDescriptors[0].blockNumbers[1] = getEmptyBlock(); <-- implement getEmptyBlock()
//        }
        return STATUS_SUCCESS;
    }

    private int findEmptyDescriptor() {
        for (int i = 0; i < NUMBER_OF_FILE_DESCRIPTORS; i++) {
            if (fileDescriptors[i] == null) return i;
        }
        return -1;
    }

    /**
     * Destroys the named file.
     *
     * @param symbolicFileName name of the file to be destroyed.
     * @return int              status.
     */
    int destroy(String symbolicFileName) throws Exception {
        int FDIndex = getFDIndex(symbolicFileName);
        if (FDIndex == -1) {
            System.out.println("Destroy: file does NOT exist.");
            return STATUS_ERROR;
        }

        /**
         * Close file if it is open
         */
        int OFTEntryIndex = getOFTEntryIndex(FDIndex);
        if (OFTEntryIndex != -1) {
            close(OFTEntryIndex);
        }

        // clear file blocks on disk
        int[] fileBlocks = fileDescriptors[FDIndex].blockNumbers;
        for (int block : fileBlocks) {
            if (block != -1)
                ioSystem.write_block(block, new byte[64]);
        }

        // remove file from directory
        int dirEntryIndex = getDirectoryEntryIndex(FDIndex);
        directory.entries.remove(dirEntryIndex);

        // !!!!!!!!! ----------------- clear bitmap

        // clear file descriptor
        fileDescriptors[FDIndex] = null;

        System.out.println("Destroy: file " + symbolicFileName + " is destroyed.");
        return STATUS_SUCCESS;
    }

    int getDirectoryEntryIndex(int FDIndex) {
        for (int i = 0; i < NUMBER_OF_FILE_DESCRIPTORS - 1; i++) {
            if (directory.entries.get(i) != null && directory.entries.get(i).FDIndex == FDIndex) return i;
        }
        return -1;
    }

    /**
     * Opens the named file for reading and writing.
     *
     * @param symbolicFileName name of the file to be opened.
     * @return int                  OFT index.
     */
    int open(String symbolicFileName) throws Exception {

        int FDIndex = getFDIndex(symbolicFileName);
        if (FDIndex == -1) {
            System.out.println("Open: file does NOT exist.");
            return STATUS_ERROR;
        }

        if (getOFTEntryIndex(FDIndex) != -1) {
            System.out.println("Open: file is already opened.");
            return getOFTEntryIndex(FDIndex);
        }

        int OFTEntryIndex = findFreeOFTEntryIndex();
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

    int getFDIndex(String fileName) {
        for (Directory.DirEntry dirEntry : directory.entries) {
            if (dirEntry.file_name.equals(fileName)) return dirEntry.FDIndex;
        }
        return -1;
    }

    int findFreeOFTEntryIndex() {
        for (int i = 1; i < 4; i++) {
            if (OFT.entries[i] == null) return i;
        }
        return -1;
    }

    int getOFTEntryIndex(int FDIndex) {
        for (int i = 1; i < OFT.entries.length; i++) {
            if (OFT.entries[i] != null && OFT.entries[i].FDIndex == FDIndex) return i;
        }
        return -1;
    }

    /**
     * Closes the specified file.
     *
     * @param OFTEntryIndex index of file in OFT.
     * @return int    status.
     */
    int close(int OFTEntryIndex) throws Exception {

        if (OFTEntryIndex == 0) {
            System.out.println("Close: directory can't be closed.");
            return STATUS_ERROR;
        }

        if (OFTEntryIndex >= OFT.entries.length || OFT.entries[OFTEntryIndex] == null) {
            System.out.println("Close: file is not opened.");
            return STATUS_ERROR;
        }

        // write buffer to disk
        int currentFileBlock = OFT.entries[OFTEntryIndex].currentPosition / IOSystem.getBlockLengthInBytes();
        int currentDiskBlock = fileDescriptors[OFT.entries[OFTEntryIndex].FDIndex].blockNumbers[currentFileBlock];

        if (currentDiskBlock != -1)
            ioSystem.write_block(currentDiskBlock, OFT.entries[OFTEntryIndex].RWBuffer);

        OFT.entries[OFTEntryIndex] = null;

        System.out.println("Close: file is closed.");
        return STATUS_SUCCESS;
    }

    /**
     * Sequentially reads a number of bytes from the specified file into main memory.
     * Reading begins with the current position in the file.
     *
     * @param OFTEntryIndex index of file in OFT.
     * @param memArea       starting main memory address.
     * @param count         number of bytes to be read.
     * @return int    status.
     */
    int read(int OFTEntryIndex, int memArea, int count) {
        return STATUS_SUCCESS;
    }

    /**
     * Sequentially writes a number of bytes from main memory into the specified file.
     * Writing begins with the current position in the file.
     *
     * @param OFTEntryIndex index of file in OFT.
     * @param memArea       starting main memory address.
     * @param count         number of bytes to be written.
     * @return int    status.
     */
    int write(int OFTEntryIndex, int memArea, int count) {
        return STATUS_SUCCESS;
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
    int lseek(int OFTEntryIndex, int pos) {
        return STATUS_SUCCESS;
    }

    /**
     * Lists the names of all files and their lengths.
     */
    void directory() {
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
