package iosystem;

public class Block {
    public byte[] bytes;

    public Block() {
        bytes = new byte[IOSystem.getBlockLengthInBytes()];
    }
}
