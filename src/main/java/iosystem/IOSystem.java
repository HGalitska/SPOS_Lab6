package iosystem;

import disk.Cylinder;
import disk.LDisk;
import disk.Track;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class IOSystem {

    /**
     * ldisk - the physical disk
     * <p>
     * (L) - numberOfBlocks - is the number of logical blocks
     * <p>
     * (B) - blockLengthInBytes  - is the block length, i.e., the number of bytes per block.
     */
    private LDisk ldisk;
    private final int numberOfBlocks;
    private static final int blockLengthInBytes = 64;
    private final int numOfBlocksInOneCylinder;
    private final int numOfBlocksInOneTrack;
    private final int numOfBlocksInOneSector;

    public IOSystem(LDisk ldisk) {
        this.ldisk = ldisk;
        System.out.println("Total number of bytes = " + LDisk.numOfBytes);
        numberOfBlocks = LDisk.numOfBytes / blockLengthInBytes;
        System.out.println("Block length in bytes = " + blockLengthInBytes + "\nNumber of blocks = " + numberOfBlocks);
        numOfBlocksInOneCylinder = (Cylinder.numOfBytes / blockLengthInBytes);
        numOfBlocksInOneTrack = numOfBlocksInOneCylinder / Cylinder.numOfTracks;
        numOfBlocksInOneSector = numOfBlocksInOneTrack / Track.numOfSectors;
        System.out.println("numOfBlocksInOneCylinder = " + numOfBlocksInOneCylinder + "\nnumOfBlocksInOneTrack = " + numOfBlocksInOneTrack + "\nnumOfBlocksInOneSector = " + numOfBlocksInOneSector);
    }

    public static int getBlockLengthInBytes() {
        return blockLengthInBytes;
    }

    /**
     * @param blockNumber index of the block
     * @return int array that contains cylinder number, track number, sector number on disk, that correspond to the block.
     */
    private int[] getBlockLocationOnDisk(int blockNumber) throws Exception {
        int cylinderNumber = -1;
        for (int k = 1; k <= LDisk.numOfCylinders; k++) {
            if (blockNumber <= k * numOfBlocksInOneCylinder - 1) {
                cylinderNumber = k - 1;
                break;
            }
        }

        if (cylinderNumber < 0) throw new Exception("cannot define cylinder number for block #" + blockNumber);


        int trackNumber = -1;
        int tempBlockNumber = blockNumber % numOfBlocksInOneCylinder;

        for (int k = 1; k <= Cylinder.numOfTracks; k++) {
            if (tempBlockNumber <= k * numOfBlocksInOneTrack - 1) {
                trackNumber = k - 1;
                break;
            }
        }

        if (trackNumber < 0) throw new Exception("cannot define track number for block #" + blockNumber);

        int sectorNumber = -1;
        tempBlockNumber %= numOfBlocksInOneTrack;

        for (int k = 1; k <= Track.numOfSectors; k++) {
            if (tempBlockNumber <= k * numOfBlocksInOneSector - 1) {
                sectorNumber = k - 1;
                break;
            }
        }

        if (sectorNumber < 0) throw new Exception("cannot define sector number for block #" + blockNumber);

        int[] result = new int[3];

        System.out.println("try to get block #" + blockNumber + "\ncylinder number = " + cylinderNumber + "\ntrackNumber = " + trackNumber + "\nsector number = " + sectorNumber);

        result[0] = cylinderNumber;
        result[1] = trackNumber;
        result[2] = sectorNumber;

        return result;
    }

    /**
     * Copies the logical block ldisk[blockNumber] into main memory starting at the location
     * specified by the pointer p. The number of characters copied corresponds to the
     * block length, B (blockLengthInBytes).
     *
     * @param blockNumber the number of the logical block that should be read
     * @param buffer      the pointer that specified the destination location in main memory for storage the block's copy
     * @throws IllegalArgumentException
     * @throws Exception
     */
    public int[] read_block(int blockNumber, ByteBuffer buffer) throws IllegalArgumentException, Exception {
        if (0 > blockNumber || blockNumber >= numberOfBlocks)
            throw new IllegalArgumentException("(blockNumber) should be: (0 <= blockNumber || blockNumber < numberOfBlocks); blockNumber = " + blockNumber + "; numberOfBlocks = " + numberOfBlocks);
        if (buffer.array().length != blockLengthInBytes)
            throw new IllegalArgumentException("Byte[] p.length != blockLengthInBytes");

        System.out.println("\nread block(" + blockNumber + ")");

        int[] blockLocation = getBlockLocationOnDisk(blockNumber);

        System.out.println("block location: " + Arrays.toString(blockLocation));

        for (int k = 0; k < blockLengthInBytes; k++) {
            buffer.put(k, ldisk.cylinders[blockLocation[0]].tracks[blockLocation[1]].sectors[blockLocation[2]].bytes[k]);
        }

        return blockLocation;
    }

    /**
     * Copies the number of character corresponding to the block length, B (blockLengthInBytes), from
     * main memory starting at the location specified by the pointer p, into the logical
     * block ldisk[blockNumber].
     *
     * @param blockNumber the number of the destination logical block to which the block should be written
     * @param buffer      the pointer that specified the source location in main memory from which block will be copied
     * @throws IllegalArgumentException
     * @throws Exception
     */
    public void write_block(int blockNumber, byte[] buffer) throws IllegalArgumentException, Exception {
        if (0 > blockNumber || blockNumber >= numberOfBlocks)
            throw new IllegalArgumentException("(blockNumber) should be: (0 <= blockNumber || blockNumber < numberOfBlocks); blockNumber = " + blockNumber + "; numberOfBlocks = " + numberOfBlocks);
        if (buffer.length != blockLengthInBytes)
            throw new IllegalArgumentException("Byte[] p.length != blockLengthInBytes");

        System.out.println("\nwrite block(" + blockNumber + ")");

        int[] blockLocation = getBlockLocationOnDisk(blockNumber);
        System.out.println("block location: " + Arrays.toString(blockLocation));

        for (int k = 0; k < blockLengthInBytes; k++) {
            ldisk.cylinders[blockLocation[0]].tracks[blockLocation[1]].sectors[blockLocation[2]].bytes[k] = buffer[k];
        }
    }
}
