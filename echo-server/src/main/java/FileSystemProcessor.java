import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class FileSystemProcessor {

    Path rootDirectory;
    Path currentDirectory;

    public FileSystemProcessor(String rootDirectory) throws IOException {
        this(Paths.get(rootDirectory).toAbsolutePath());
    }

    public FileSystemProcessor(Path rootDirectory) throws IOException {
        if (!directoryExists(rootDirectory)) throw
                new IOException(String.format(
                        "Directory '%s' does not exist",
                        this.rootDirectory.toAbsolutePath()));
        this.rootDirectory = rootDirectory;
        this.currentDirectory = this.rootDirectory;
    }

    public static void main(String[] args) {
        try {
            FileSystemProcessor fsp = new FileSystemProcessor("Test");
            System.out.println(fsp.rootDirectory.toAbsolutePath());
            System.out.println(fsp.callCd("Test1"));
            System.out.println(fsp.callTouch("1.txt"));
            System.out.println(fsp.callMkDir("newdir"));
            System.out.println(fsp.callRm("newdir"));
            System.out.println(fsp.callLs());
            System.out.println(fsp.callCat("1.txt"));
            System.out.println(fsp.call("MKDIR newdir"));
            System.out.println(fsp.callCd("newdir"));
            System.out.println(fsp.getRelativeToRootPath(fsp.currentDirectory).toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String call(String command) throws IOException {
        String[] argument = command.split(" ");
        String result = "";
        command = argument[0];
        System.out.printf("COMMAND: %s%n", command);
        try {
            if (command.equalsIgnoreCase(Command.LS.name())) result = callLs();
            else if (command.equalsIgnoreCase(Command.MKDIR.name())) result = callMkDir(argument[1]);
            else if (command.equalsIgnoreCase(Command.TOUCH.name())) result = callTouch(argument[1]);
            else if (command.equalsIgnoreCase(Command.RM.name())) result = callRm(argument[1]);
            else if (command.equalsIgnoreCase(Command.CAT.name())) result = callCat(argument[1]);
            else if (command.equalsIgnoreCase(Command.CD.name()))
                result = callCd(argument.length >= 2 ? argument[1] : null);
            else {
                result = String.format("UNKNOWN COMMAND: %s", command);
            }
        } catch (Exception e) {
            result = String.format("ERROR: %s", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public boolean directoryExists(Path path) {
        return Files.exists(path) && Files.isDirectory(path);
    }

    public boolean fileExists(Path path) {
        return Files.exists(path) && !Files.isDirectory(path);
    }

    public Path getRelativeToRootPath(Path path) {
        return rootDirectory.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize());
    }

    public Path getPathFromRoot(Path path) throws IOException {
        return getPathFromRoot(path, false);
    }

    public Path getPathFromRoot(Path path, boolean checkPathExists) throws IOException {
        path = path.toAbsolutePath().normalize();
        if (checkPathExists)
            pathFromRootCheck(path);
        return path;
    }

    public void pathFromRootCheck(Path path) throws IOException {
        pathFromRootExists(path);
        pathFromRootAccessible(path);
    }

    public void pathFromRootExists(Path path) throws IOException {
        if (!(Files.exists(path))) throw
                new IOException(String.format("Path '%s' does not exist", path.toRealPath()));
    }

    public void pathFromRootAccessible(Path path) throws IOException {
        if (!(path.toAbsolutePath().normalize().startsWith(rootDirectory.toAbsolutePath().normalize()))) throw
                new IOException(String.format("Insufficient permissions to access path '%s'", path.toRealPath()));
    }

    public String callCat(String file) throws IOException {
        Path path = getPathFromRoot(currentDirectory.resolve(file), true);
        StringBuilder result = new StringBuilder();
        if (Files.isDirectory(path))
            result.append(callLs());
        else {
            ArrayList<String> line = (ArrayList<String>) Files.readAllLines(path);
            for (int i = 0; i < line.size(); i++)
                result.append(line.get(i) + "\n");
        }
        return result.toString();
    }

    public String callLs() throws IOException {
        String files = Files.list(currentDirectory)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.joining(", "));
        return files;
    }

    public String callCd(String targetPath) throws IOException {
        if (targetPath == null) targetPath = "";
        Path tmpPath = currentDirectory.resolve(targetPath);
        currentDirectory = getPathFromRoot(tmpPath, true);
        return getRelativeToRootPath(currentDirectory).toString();
    }

    public String callTouch(String filePath) throws IOException {
        Path path = getPathFromRoot(currentDirectory.resolve(filePath));
        pathFromRootAccessible(path);
        if (Files.exists(path))
            Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
        else
            Files.createFile(path);
        return String.format("File '%s'", getRelativeToRootPath(path));
    }

    public String callMkDir(String directory) throws IOException {
        String result;
        Path path = getPathFromRoot(currentDirectory.resolve(directory));
        pathFromRootAccessible(path);
        if (!Files.exists(path)) {
            Files.createDirectory(path);
            result = String.format("Created directory '%s'", getRelativeToRootPath(path));
        } else
            result = String.format("Directory '%s' exists", getRelativeToRootPath(path));
        return result;
    }

    public String callRm(String rmPath) throws IOException {
        String result;
        Path path = getPathFromRoot(currentDirectory.resolve(rmPath), true);
        Files.delete(path);
        result = String.format("%s '%s' deleted",
                Files.isDirectory(path) ? "Directory" : "File", getRelativeToRootPath(path).toString());
        return result;
    }

    enum Command {
        CD,
        LS,
        RM,
        CAT,
        TOUCH,
        MKDIR
    }
}
