#### File System Simulator 
##### (with I/O system emulator and CLI)

This project develops a simple file system using an emulated I/O system.
The user interacts with the file system using commands, such as create, open, or
read file. The file system views the disk as a linear sequence of logical blocks numbered
from 0 to L − 1. The I/O system uses a memory array to emulate the disk and presents
the logical blocks abstraction to the file system as its interface.

##### Project Specific Tasks:
1. Design and implement the emulated I/O system, in particular, the two functions
read block(int i, char *p) and write block(int i, char *p).
2. Design and implement the file system on top of the I/O system. It should support
the functions that define the user/file system interface.
3. Define a command language for the presentation shell. Then, design and implement
the shell so that you can test and demonstrate the functionality of your file system
interactively.
4. Test the file system using a variety of command sequences to explore all aspects
of its behavior.

##### The file system supports the following functions: 
* create(symbolic file name): create a new file with the specified name.
* destroy(symbolic file name): destroy the named file.
* open(symbolic file name): open the named file for reading and writing; return an
index value which is used by subsequent read, write, lseek, or close operations.
* close(index): close the specified file.
* read(index, mem area, count): sequentially read a number of bytes from the spec-
ified file into main memory. The number of bytes to be read is specified in count
and the starting memory address in mem area. The reading starts with the current
position in the file.
* write(index, mem area, count): sequentially write a number of bytes from main
memory starting at mem area into the specified file. As with the read operation,
the number of bytes is given in count and the writing begins with the current
position in the file.
* lseek(index, pos): move the current position of the file to pos, where pos is an
integer specifying the number of bytes from the beginning of the file. When a file
is initially opened, the current position is automatically set to zero. After each read
or write operation, it points to the byte immediately following the one that was
accessed last. lseek permits the position to be explicitly changed without reading
or writing the data. Seeking to position 0 implements a reset command, so that the
entire file can be reread or rewritten from the beginning.
* directory: list the names of all files and their lengths.


Language of implementation: JAVA.
