import disk.LDisk;
import org.junit.Assert;
import org.junit.Test;

public class IOSystemTest {

    @Test
    public void read_block() {
        LDisk ldisk = new LDisk();
        IOSystem IO_System = new IOSystem(ldisk);

        Byte[] buffer = new Byte[IO_System.getBlockLengthInBytes()];

        int i = 0;
        int[] expectedResult = {0, 0, 0};
        int[] actualResult = null;

        try {
            actualResult = IO_System.read_block(i, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 3;
        expectedResult = new int[]{0, 0, 1};

        try {
            actualResult = IO_System.read_block(i, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 7;
        expectedResult = new int[]{0, 0, 3};

        try {
            actualResult = IO_System.read_block(i, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 8;
        expectedResult = new int[]{0, 1, 0};

        try {
            actualResult = IO_System.read_block(i, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 23;
        expectedResult = new int[]{0, 2, 3};

        try {
            actualResult = IO_System.read_block(i, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 31;
        expectedResult = new int[]{0, 3, 3};

        try {
            actualResult = IO_System.read_block(i, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 32;
        expectedResult = new int[]{1, 0, 0};

        try {
            actualResult = IO_System.read_block(i, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 39;
        expectedResult = new int[]{1, 0, 3};

        try {
            actualResult = IO_System.read_block(i, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 56;
        expectedResult = new int[]{1, 3, 0};

        try {
            actualResult = IO_System.read_block(i, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 63;
        expectedResult = new int[]{1, 3, 3};
        actualResult = null;

        try {
            actualResult = IO_System.read_block(i, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(expectedResult, actualResult);

        /************************************************************************/

        i = 64;
        Exception exception = null;
        try {
            IO_System.read_block(i, buffer);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull(exception);

        /************************************************************************/

        i = -1;
        exception = null;
        try {
            IO_System.read_block(i, buffer);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull(exception);
    }
}