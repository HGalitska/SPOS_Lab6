package filesystem;

public class FileSystem {

    final static int NUMBER_OF_FILE_DESCRIPTORS = 16;

    OpenFileTable OFT;
    int[] bitmap;

    FileSystem() {
        OFT = new OpenFileTable();
        bitmap = new int[2];
    }

    /**
     * @param FDIndex index of the file descriptor.
     * @return FileDescriptor   that correspond to the block.
     */
    FileDescriptor getFD(int FDIndex) {
        return null;
    }

    /**
     * Creates a new file with the specified name.
     *
     * @param symbolicFileName name of the file to be created.
     * @return boolean              status.
     */
    boolean create(String symbolicFileName) {
        boolean status = false;
        return status;
    }

    /**
     * Destroys the named file.
     *
     * @param symbolicFileName name of the file to be destroyed.
     * @return boolean              status.
     */
    boolean destroy(String symbolicFileName) {
        boolean status = false;
        return status;
    }

    /**
     * Opens the named file for reading and writing.
     *
     * @param symbolicFileName name of the file to be opened.
     * @return int                  index of file descriptor.
     */
    int open(String symbolicFileName) {
        int FDIndex = -1;
        return FDIndex;
    }

    /**
     * Closes the specified file.
     *
     * @param FDIndex index of file descriptor.
     */
    void close(int FDIndex) {
    }

    /**
     * Sequentially reads a number of bytes from the specified file into main memory.
     * Reading begins with the current position in the file.
     *
     * @param FDIndex index of file descriptor.
     * @param memArea starting main memory address.
     * @param count   number of bytes to be read.
     */
    void read(int FDIndex, int memArea, int count) {
    }

    /**
     * Sequentially writes a number of bytes from main memory into the specified file.
     * Writing begins with the current position in the file.
     *
     * @param FDIndex index of file descriptor.
     * @param memArea starting main memory address.
     * @param count   number of bytes to be written.
     */
    void write(int FDIndex, int memArea, int count) {
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
     * @param FDIndex index of file descriptor.
     * @param pos     new position, specifies the number of bytes from the beginning of the file
     */
    void lseek(int FDIndex, int pos) {
    }

    /**
     * Lists the names of all files and their lengths.
     */
    void directory() {
    }
}
