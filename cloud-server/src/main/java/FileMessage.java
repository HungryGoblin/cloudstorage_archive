import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;

public class FileMessage implements Serializable {

    private final String name;
    private final byte[] data;
    private final LocalDate createAt;

    private int partLength = 0;
    private boolean end;

    public FileMessage(Path path) throws IOException {
        if (!Files.exists(path)) throw new IOException(String.format("File does not exist: %s", path.toAbsolutePath()));
        name = path.getFileName().toString();
        data = Files.readAllBytes(path);
        createAt = LocalDate.now();
    }

    public int getPartLength() {
        return partLength;
    }

    public void setPartLength(int partLength) {
        this.partLength = partLength;
    }

//    public int getPartsNumber() {
//        return (partLength == 0) ? 1 : getData().length % partLength + 1;
//    }

//    public byte[] getPart(int part) {
//        byte[] bytePart;
//        int partLength = getPartLength();
//        int from = part * partLength;
//        int len = from + partLength;
//        if (from + len >= getData().length) len = getData().length;
//        System.arraycopy(getData(), from, bytePart, 0, len - );
//    }

    public boolean isEnd() {
        return end;
    }

    public boolean saveFile(String fileName) throws IOException {
        FileOutputStream writer = new FileOutputStream(fileName);
        writer.write(getData());
        writer.flush();
        writer.close();
        return true;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }

    public LocalDate getCreateAt() {
        return createAt;
    }

    @Override
    public String toString() {
        return "FileMessage{" +
                "name='" + name + '\'' +
                ", data=" + Arrays.toString(data) +
                ", createAt=" + createAt +
                '}';
    }

}
