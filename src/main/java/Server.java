import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final ConcurrentHashMap<String, String> serverCache = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Hello from Server");

        Selector selector = Selector.open();

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress("localhost", 8090));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            if (selector.select() == 0) {
                continue;
            }

            processEvent(selector.selectedKeys());
        }
    }

    public static void processEvent(Set<SelectionKey> keysSet) throws IOException {
        Iterator<SelectionKey> iterator = keysSet.iterator();
        SelectionKey selectionKey;

        while (iterator.hasNext()) {
            selectionKey = iterator.next();

            if (selectionKey.isAcceptable()) {
                processAccept(selectionKey);
            }

            if (selectionKey.isReadable()) {
                processReadAndMakeEcho(selectionKey);
            }

            iterator.remove();
        }
    }

    public static void processAccept(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ);

        System.out.println("New client connecting");
    }

    public static void processReadAndMakeEcho(SelectionKey selectionKey) throws IOException {
        SocketChannel clientSocketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        int dataSize = clientSocketChannel.read(byteBuffer);
        if (dataSize > 0) {
            String message = new String(byteBuffer.array());
            System.out.println("Message received: " + message);
            
            message = message.trim();
            if (!message.equals("\\n")) {
                if (!serverCache.containsKey(clientSocketChannel.getRemoteAddress().toString())) {
                    serverCache.put(clientSocketChannel.getRemoteAddress().toString(), message);
                } else {
                    serverCache.put(clientSocketChannel.getRemoteAddress().toString(),
                            serverCache.get(clientSocketChannel.getRemoteAddress().toString()).concat(message));
                }
            }
            else {
                byteBuffer.flip();
                byteBuffer = ByteBuffer.wrap(serverCache.get(clientSocketChannel.getRemoteAddress().toString()).getBytes());
                clientSocketChannel.write(byteBuffer);

                serverCache.remove(clientSocketChannel.getRemoteAddress().toString());
            }
        }
    }
}