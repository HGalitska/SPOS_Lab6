package disk;

public class Sector {
    public static final short numOfBytes = 128;
    public byte[] bytes;

    public Sector() {
        bytes = new byte[numOfBytes];
    }
}
