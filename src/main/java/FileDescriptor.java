public class FileDescriptor implements java.io.Serializable {
    /**
     * fileLengthInBytes - file length in bytes
     * <p>
     * blockNumbers - an array of disk block numbers that hold the file contents.
     * <p>
     * The length of this array is a system parameter. Set it to a small number, e.g., 3. Keep this value in "numberOfBlocks"
     */
    private static short numberOfBlocks = 3;
    private short maxFileLengthInBytes;                  // max = numberOfBlocks (for one file) * B (block length in bytes)
    private short fileLengthInBytes;
    private short[] blockNumbers;

    public FileDescriptor(int blockLengthInBytes) throws IllegalArgumentException {
        if (blockLengthInBytes < 0)
            throw new IllegalArgumentException("blockLengthInBytes should be more than 0; blockLengthInBytes = " + blockLengthInBytes);
        int maxFileLengthInBytes = (numberOfBlocks * blockLengthInBytes);
        // check for short limits also
        if ((0 >= maxFileLengthInBytes) || (maxFileLengthInBytes > 32767))
            throw new IllegalArgumentException("maxFileLengthInBytes = (numberOfBlocks * blockLengthInBytes); it should be: ((0 < maxFileLengthInBytes) || (maxFileLengthInBytes <= 32767)); numberOfBlocks = " + numberOfBlocks + "; blockLengthInBytes = " + blockLengthInBytes + "; maxFileLengthInBytes = " + maxFileLengthInBytes);
        this.maxFileLengthInBytes = (short) maxFileLengthInBytes;
        blockNumbers = new short[numberOfBlocks];
    }

    public short getFileLengthInBytes() {
        return fileLengthInBytes;
    }

    public void setFileLengthInBytes(short fileLengthInBytes) throws IllegalArgumentException {
        if ((0 > fileLengthInBytes) || (fileLengthInBytes > maxFileLengthInBytes))
            throw new IllegalArgumentException("(0 > fileLengthInBytes) || (fileLengthInBytes > maxFileLengthInBytes); fileLengthInBytes = " + fileLengthInBytes + "; maxFileLengthInBytes = " + maxFileLengthInBytes);
        this.fileLengthInBytes = fileLengthInBytes;
    }

    public short[] getBlockNumbers() {
        return blockNumbers;
    }

    public void setBlockNumbers(short[] blockNumbers) {
        this.blockNumbers = blockNumbers;
    }
}
