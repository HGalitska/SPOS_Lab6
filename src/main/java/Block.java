public class Block {
    public byte[] bytes;

    Block() {
        bytes = new byte[IOSystem.getBlockLengthInBytes()];
    }
}
