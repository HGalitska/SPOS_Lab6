package disk;

public class Sector {
    public static final short numOfBytes = 64;
    public byte[] bytes;

    public Sector() {
        bytes = new byte[numOfBytes];
    }
}
