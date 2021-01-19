import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;


public class NioServer {

    public static final int DEFAULT_PORT = 3000;
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    public static final String DEFAULT_SERVER_DIR = "serverDir\\test1";

    private final Selector selector;
    private final ServerSocketChannel serverSocket;
    private final ByteBuffer buffer;
    private final Path rootPath = Paths.get(DEFAULT_SERVER_DIR);
    private final FileSystemProcessor fsp;
    private int port = DEFAULT_PORT;

    public NioServer(int port) throws IOException {
        fsp = new FileSystemProcessor(rootPath);
        this.port = port;
        buffer = ByteBuffer.allocate(5);
        serverSocket = ServerSocketChannel.open();
        selector = Selector.open();
        serverSocket.configureBlocking(false);
        serverSocket.bind(new InetSocketAddress(port));
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        listen();
    }

    public NioServer() throws IOException {
        this(DEFAULT_PORT);
    }

    public static void main(String[] args) throws IOException {
        NioServer server = new NioServer();
        server.listen();
    }

    public void handleCommand(SelectionKey key, String command) throws IOException {
        answerClient(key, fsp.call(command));
    }

    private void acceptClient(SelectionKey key) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.printf("Client accepted: %s%n", channel.socket().getRemoteSocketAddress());
        channel.register(selector, SelectionKey.OP_READ);
    }

    private String readClient(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder command = new StringBuilder();
        boolean ret = false;
        while (!ret) {
            while ((channel.read(buffer)) > 0) {
                buffer.flip();
                while (buffer.hasRemaining()) {
                    byte b = buffer.get();
                    if (b == 10 || b == 13)
                        ret = true;
                    else
                        command.append((char) b);
                    System.out.println(b);
                }
                buffer.clear();
            }
        }
        return command.toString();
    }

    private void answerClient(SelectionKey key, String message) throws IOException {
        if (message.isEmpty()) return;
        System.out.println(message);
        SocketChannel channel = (SocketChannel) key.channel();
        message = String.format("%s\r\n", message);
        channel.write(ByteBuffer.wrap(message.getBytes(DEFAULT_CHARSET)));
    }

    public void listen() throws IOException {
        System.out.printf("Listening port: %d%n", port);
        while (serverSocket.isOpen()) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isAcceptable()) {
                    acceptClient(key);
                }
                if (key.isReadable()) {
                    String command = readClient(key);
                    if (!command.isEmpty()) handleCommand(key, command);
                }
            }
        }
    }
}
