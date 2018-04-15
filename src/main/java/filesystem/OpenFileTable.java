package filesystem;

import iosystem.IOSystem;

class OpenFileTable {

    class OFTEntry {
        Byte[] RWBuffer;
        int currentPosition;
        int FDIndex;

        OFTEntry() {
            RWBuffer = new Byte[IOSystem.getBlockLengthInBytes()];
            currentPosition = -1;
            FDIndex = -1;
        }
    }

    OFTEntry[] entries;

    OpenFileTable() {
        entries = new OFTEntry[4];

        for (int i = 0; i < 4; i++) {
            entries[i] = new OFTEntry();
        }
    }
}
