package filesystem;

class FileDescriptor {
    int fileLengthInBytes;
    int[] blockNumbers;

    FileDescriptor() {
        fileLengthInBytes = -1;
        blockNumbers = new int[]{-1, -1, -1};
    }
}
