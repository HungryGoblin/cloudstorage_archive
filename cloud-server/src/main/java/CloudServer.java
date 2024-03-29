import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class CloudServer implements AutoCloseable {

    public static final int PORT = 3000;
    public static final int PAUSE = 2000;

    private final int port;
    private final ServerSocket server;
    private String clientHost;
    private Socket client;
    private ObjectOutputStream clientWriter;
    private ObjectInputStream clientReader;
    private boolean started = false;

    public CloudServer(int port) throws IOException {
        this.port = port;
        server = new ServerSocket(port);
    }

    public static void main(String[] args) {
        try {
            CloudServer server = new CloudServer(PORT);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void process() throws Exception {
        try {
            while (!client.isClosed()) {
                FileMessage message = (FileMessage) clientReader.readObject();
                if (message != null) {
                    String fileName = message.getName() + "_srv";
                    if (message.saveFile(fileName))
                        System.out.printf("FILE '%s' SAVED TO '%s'", message.getName(), fileName);
                    else
                        System.out.printf("FILE '%s' NOT SAVED", message.getName());
                }
                Thread.sleep(PAUSE);
            }
        } catch (Exception e) {
            System.out.printf("PROCESSING STOPPED: %s", e.getMessage());
        }
        waitConnection();
    }

    public void closeConnection() throws IOException {
        if (client != null && !client.isClosed()) {
            client.close();
            System.out.println("Connection terminated");
        }
        clientHost = null;
    }

    public void start() throws Exception {
        closeConnection();
        started = true;
        System.out.println("Server started");
        waitConnection();
    }

    public void waitConnection() throws Exception {
        closeConnection();
        if (started) {
            System.out.printf("Ready for connect on port: %d%n", port);
            client = server.accept();
            clientHost = client.getInetAddress().getHostAddress();
            System.out.printf("Connection accepted: %s%n", clientHost);
            clientWriter = new ObjectOutputStream(client.getOutputStream());
            clientReader = new ObjectInputStream(client.getInputStream());
            process();
        }
    }

    public void stop() throws Exception {
        closeConnection();
        System.out.println("Server stopped");
        close();
    }

    @Override
    public void close() throws Exception {
        clientReader.close();
        clientWriter.close();
        server.close();
        client.close();
    }
}
