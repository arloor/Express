import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ChanelAndBuff {
    private SocketChannel channel;
    private ByteBuffer buff;


    public ChanelAndBuff(SocketChannel channel) {
        this.channel=channel;
        buff=ByteBuffer.allocate(8192);
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public ByteBuffer getBuff() {
        return buff;
    }

    public SelectionKey register(Selector selector, int opRead) {
        try {
            return channel.register(selector,opRead);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
        return null;
    }
}
