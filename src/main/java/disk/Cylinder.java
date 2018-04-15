package disk;

public class Cylinder {
    public Track[] tracks;
    public static final int numOfTracks = 2;
    public static final int numOfBytes = numOfTracks * Track.numOfBytes;

    public Cylinder() {
        tracks = new Track[numOfTracks];
        for (int i = 0; i < tracks.length; i++) {
            tracks[i] = new Track();
        }
    }
}
