import java.io.*;
import java.net.Socket;
import java.nio.file.Paths;

public class CloudClient implements Closeable {

    public static final int PAUSE = 1000;
    private BufferedReader commandReader;

    private ObjectOutputStream serverWriter;

    private String serverAddress;
    private int serverPort;
    private Socket server;

    public static void main(String[] args) throws IOException {
        try {
            CloudClient client = new CloudClient();
            client.connect("localhost", 3000);
            client.process();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() throws IOException {
        if (server != null && !server.isClosed()) {
            server.close();
            serverAddress = null;
            serverPort = 0;
            System.out.println("Disconnected from server");
        }
    }

    public boolean connect(String serverAddress, int serverPort) {
        try {
            disconnect();
            server = new Socket(serverAddress, serverPort);
            commandReader = new BufferedReader(new InputStreamReader(System.in));
            serverWriter = new ObjectOutputStream(server.getOutputStream());
            System.out.printf("Connected to server: %s:%d%n", serverAddress, serverPort);
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
        } catch (IOException e) {
            System.out.printf("Server connection failed: %s%n", e.getMessage());
        }
        return !server.isClosed();
    }

    public void sendFile(String path) throws IOException {
        sendFile(path, 0);
    }

    public void sendFile(String path, int maxLen) throws IOException {
        if (maxLen == 0) {
            FileMessage fileMessage = new FileMessage(Paths.get(path));
            serverWriter.writeObject(fileMessage);
        } else {

        }


    }

    public void processCommand(String commandString) throws RuntimeException, IOException {
        String[] command = commandString.split(" "); // 0 - команды, 1-N - параметры
        System.out.printf("COMMAND: %s%n", command);
        switch (command[0]) {
            case "send":
                if (command.length < 2) throw new RuntimeException(String.format(
                        "WRONG SYNTAX: %s (passed parameters number: %d, expected: 1)",
                        command[0], command.length - 1));
                sendFile(command[1]);
                break;
            default:
                throw new RuntimeException(String.format(
                        "UNEXPECTED COMMAND: %s", command[0]));
        }
    }

    public void process() throws Exception {
        String command = null;
        while (server != null && server.isConnected()) {
            try {
                if (commandReader.ready()) {
                    command = commandReader.readLine();
                    if (command != null && !command.isEmpty())
                        processCommand(command);
                }
            } catch (Exception e) {
                System.out.printf("ERROR: %s%n", e.getMessage());
            } finally {
                command = null;
                Thread.sleep(PAUSE);
            }
        }
    }

    @Override
    public void close() throws IOException {
        disconnect();
        commandReader.close();
        serverWriter.close();
    }

}