package disk;

public class LDisk {
    public Cylinder[] cylinders;
    public static final int numOfCylinders = 4;
    public static final int numOfBytes = numOfCylinders * Cylinder.numOfBytes;

    public LDisk() {
        cylinders = new Cylinder[numOfCylinders];
        for (int i = 0; i < cylinders.length; i++) {
            cylinders[i] = new Cylinder();
        }
    }
}
