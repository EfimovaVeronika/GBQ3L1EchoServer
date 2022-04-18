import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Client {
    public static void main(String[] args) throws IOException {
        System.out.println("Hello from Client");

        Selector selector = Selector.open();

        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress("localhost", 8090));
        socketChannel.register(selector, SelectionKey.OP_CONNECT);

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

            if (selectionKey.isConnectable()) {
                processConnect(selectionKey);
            }

            if (selectionKey.isWritable()) {
                processWrite(selectionKey);
            }

            if (selectionKey.isReadable()) {
                processRead(selectionKey);

            }

            iterator.remove();
        }
    }

    public static void processConnect(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();

        while (channel.isConnectionPending()) {
            channel.finishConnect();
            channel.register(selectionKey.selector(), SelectionKey.OP_WRITE);
        }
    }

    public static void processWrite(SelectionKey selectionKey) throws IOException {
        System.out.println("Enter message:");

        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));

        String message = userInputReader.readLine();
        ByteBuffer byteBuffer = ByteBuffer.wrap(message.getBytes());
        socketChannel.write(byteBuffer);

        if (message.equals("\\n")) {
            socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ);
        }
    }

    public static void processRead(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        int dataSize = socketChannel.read(byteBuffer);
        if (dataSize > 0) {
            String message = new String(byteBuffer.array());

            System.out.println("Echo: " + message);
        }

        socketChannel.register(selectionKey.selector(), SelectionKey.OP_WRITE);
    }
}
