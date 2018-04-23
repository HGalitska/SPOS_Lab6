package filesystem;

class FileDescriptor {
    int fileLengthInBytes;
    int[] blockNumbers;
    public static final int MAX_NUMBER_OF_BLOCKS = 3;

    /**
     * Create FD for a new empty file (length = 0)
     */
    FileDescriptor() {
        fileLengthInBytes = 0;
        blockNumbers = new int[]{-1, -1, -1};
    }

    FileDescriptor(int fileLengthInBytes, int[] blockNumbers) {
        this.fileLengthInBytes = fileLengthInBytes;
        this.blockNumbers = blockNumbers;
    }
}
