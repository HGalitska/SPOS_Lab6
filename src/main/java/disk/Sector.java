package disk;

public class Sector {
    public static final short numOfBytes = 128;
    public Byte[] bytes;

    public Sector() {
        bytes = new Byte[numOfBytes];
    }
}
