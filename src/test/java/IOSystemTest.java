import disk.LDisk;
import org.junit.Assert;
import org.junit.Test;

import java.util.BitSet;

public class IOSystemTest {

    @Test
    public void read_block() {
        LDisk ldisk = new LDisk();
        IOSystem IO_System = new IOSystem(ldisk);

        Block block = new Block();

        int blockNumber = 0;
        int[] expectedResult = {0, 0, 0};
        int[] actualResult = null;

        try {
            actualResult = IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        blockNumber = 3;
        expectedResult = new int[]{0, 0, 3};

        try {
            actualResult = IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        blockNumber = 7;
        expectedResult = new int[]{0, 0, 7};

        try {
            actualResult = IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        blockNumber = 8;
        expectedResult = new int[]{0, 1, 0};

        try {
            actualResult = IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        blockNumber = 23;
        expectedResult = new int[]{1, 0, 7};

        try {
            actualResult = IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        blockNumber = 31;
        expectedResult = new int[]{1, 1, 7};

        try {
            actualResult = IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        blockNumber = 32;
        expectedResult = new int[]{2, 0, 0};

        try {
            actualResult = IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        blockNumber = 39;
        expectedResult = new int[]{2, 0, 7};

        try {
            actualResult = IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        blockNumber = 56;
        expectedResult = new int[]{3, 1, 0};

        try {
            actualResult = IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        blockNumber = 63;
        expectedResult = new int[]{3, 1, 7};
        actualResult = null;

        try {
            actualResult = IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        blockNumber = 64;
        Exception exception = null;
        try {
            IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull(exception);

        /************************************************************************/

        blockNumber = 67;
        exception = null;
        try {
            IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull(exception);

        /************************************************************************/

        blockNumber = -1;
        exception = null;
        try {
            IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull(exception);

        /************************************************************************/

        blockNumber = -4;
        exception = null;
        try {
            IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull(exception);

        /************************************************************************/


        blockNumber = 24;
        try {
            IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertEquals(IOSystem.getBlockLengthInBytes(), block.bytes.length);

        System.out.println("\nbytes from block #24 before writing to it:\n");
        for (Byte b : block.bytes) {
            System.out.print(b + " ");
            Assert.assertEquals(0, b.intValue());
        }
        System.out.println();

        /************************************************************************/

        byte[] bytes = new byte[64];

        for (int k = 0; k < bytes.length; k++) {
            bytes[k] = (byte) (k + 15);
        }


        try {
            IO_System.write_block(blockNumber, bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            IO_System.read_block(blockNumber, block);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\nbytes from block #24 after writing to it:\n");
        for (Byte b : block.bytes) {
            System.out.print(b + " ");
        }
        System.out.println();

        for (int k = 0; k < block.bytes.length; k++) {
            Assert.assertEquals(block.bytes[k], bytes[k]);
        }
    }
}