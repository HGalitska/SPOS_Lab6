package filesystem;

import java.util.LinkedList;

public class Directory {
    final static int FILE_NAME_LENGTH = 2; // 4 bytes

    class DirEntry {
        String file_name;
        int FDIndex;

        public DirEntry(String file_name, int FDIndex) throws IllegalArgumentException {
            if (file_name.length() != FILE_NAME_LENGTH) throw new IllegalArgumentException("file_name.length() != 2");
            this.file_name = file_name;
            this.FDIndex = FDIndex;
        }
    }
    LinkedList<DirEntry> entries;

    Directory() {
        entries = new LinkedList<>();
    }

    public void addEntry(String file_name, int FDIndex) throws Exception {
        if (file_name.length() != FILE_NAME_LENGTH) throw new IllegalArgumentException("file_name.length != 2");
        if (entries.size() >= FileSystem.NUMBER_OF_FILE_DESCRIPTORS - 1)
            throw new Exception("Directory is full");
        entries.add(new DirEntry(file_name, FDIndex));
    }
}
