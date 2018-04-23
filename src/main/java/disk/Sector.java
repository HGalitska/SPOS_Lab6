package disk;

public class Sector implements java.io.Serializable {
    public static final short numOfBytes = 64;
    public byte[] bytes;

    public Sector() {
        bytes = new byte[numOfBytes];
    }
}
