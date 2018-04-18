package filesystem;

import iosystem.IOSystem;

class OpenFileTable {

    static class OFTEntry {
        byte[] RWBuffer;
        int currentPosition;
        int FDIndex;

        OFTEntry() {
            RWBuffer = new byte[IOSystem.getBlockLengthInBytes()];
            currentPosition = -1;
            FDIndex = -1;
        }
    }

    OFTEntry[] entries;

    OpenFileTable() {
        entries = new OFTEntry[4];
    }
}
