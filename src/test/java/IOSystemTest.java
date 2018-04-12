import disk.LDisk;
import org.junit.Assert;
import org.junit.Test;

public class IOSystemTest {

    @Test
    public void read_block() {
        LDisk ldisk = new LDisk();
        IOSystem IO_System = new IOSystem(ldisk);

        Block block = new Block();

        int i = 0;
        int[] expectedResult = {0, 0, 0, 0};
        int[] actualResult = null;

        try {
            actualResult = IO_System.read_block(i, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 3;
        expectedResult = new int[]{0, 0, 1, 64};

        try {
            actualResult = IO_System.read_block(i, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 7;
        expectedResult = new int[]{0, 0, 3, 64};

        try {
            actualResult = IO_System.read_block(i, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 8;
        expectedResult = new int[]{0, 1, 0, 0};

        try {
            actualResult = IO_System.read_block(i, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 23;
        expectedResult = new int[]{0, 2, 3, 64};

        try {
            actualResult = IO_System.read_block(i, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 31;
        expectedResult = new int[]{0, 3, 3, 64};

        try {
            actualResult = IO_System.read_block(i, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 32;
        expectedResult = new int[]{1, 0, 0, 0};

        try {
            actualResult = IO_System.read_block(i, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 39;
        expectedResult = new int[]{1, 0, 3, 64};

        try {
            actualResult = IO_System.read_block(i, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 56;
        expectedResult = new int[]{1, 3, 0, 0};

        try {
            actualResult = IO_System.read_block(i, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 63;
        expectedResult = new int[]{1, 3, 3, 64};
        actualResult = null;

        try {
            actualResult = IO_System.read_block(i, block);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 64;
        Exception exception = null;
        try {
            IO_System.read_block(i, block);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull(exception);

        /************************************************************************/

        i = -1;
        exception = null;
        try {
            IO_System.read_block(i, block);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull(exception);

        /************************************************************************/

        i = -1;
        exception = null;
        try {
            IO_System.read_block(i, block);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull(exception);

        /************************************************************************/


        i = 24;
        try {
            IO_System.read_block(i, block);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertEquals(IOSystem.getBlockLengthInBytes(), block.bytes.length);

        System.out.println("\nblock bytes:\n");
        for (Byte b : block.bytes) {
            System.out.print(b + " ");
            Assert.assertEquals(0, b.intValue());
        }
        System.out.println();

        /************************************************************************/

        byte[] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37,
                38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
                57, 58, 59, 60, 61, 62, 63};
        try {
            IO_System.write_block(i, bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            IO_System.read_block(i, block);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\nblock bytes:\n");
        for (Byte b : block.bytes) {
            System.out.print(b + " ");
        }
        System.out.println();
    }
}