package disk;

import java.util.Arrays;

public class LDisk {
    public Cylinder[] cylinders;
    public static final int numOfCylinders = 2;
    public static final int numOfBytes = numOfCylinders * Cylinder.numOfBytes;

    public LDisk() {
        cylinders = new Cylinder[numOfCylinders];
        for (int i = 0; i < cylinders.length; i++) {
            cylinders[i] = new Cylinder();
        }
        System.out.println(Arrays.toString(cylinders[0].tracks[0].sectors[0].bytes));
    }
}
