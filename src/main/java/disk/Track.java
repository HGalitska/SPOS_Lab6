package disk;

public class Track {
    public Sector[] sectors;
    public static final int numOfSectors = 8;
    public static final int numOfBytes = numOfSectors * Sector.numOfBytes;

    public Track() {
        sectors = new Sector[numOfSectors];
        for (int i = 0; i < sectors.length; i++) {
            sectors[i] = new Sector();
        }
    }
}
