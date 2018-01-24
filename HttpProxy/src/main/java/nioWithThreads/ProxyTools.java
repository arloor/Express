package nioWithThreads;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 负责存储相关的数据结构
 * 使用单利模式(多线程下)确保只有一个实例，也就是只有一个相关的数据结构
 */
public class ProxyTools {
    private ServerSocketChannel proxyServerSocketChannel;
    private Map<SocketChannel,SocketChannel> localRemoteChannelsMap=new ConcurrentHashMap<>();
    private Map<SocketChannel,SocketChannel> remoteLocalChannelsMap=new ConcurrentHashMap<>();
    private ConcurrentSkipListSet inputShutdownChannelSet= new ConcurrentSkipListSet();
    private static volatile  ProxyTools proxyTools;

    //构造方法
    private ProxyTools(){
        try {
            proxyServerSocketChannel=ServerSocketChannel.open();
            proxyServerSocketChannel.bind(new InetSocketAddress(2222));
            proxyServerSocketChannel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addInputShutdownChannel(SocketChannel channel){
        inputShutdownChannelSet.add(channel);
    }

    public void removeInputShutdownChannel(SocketChannel channel){
        inputShutdownChannelSet.remove(channel);
    }

    public boolean isInputShutdown(SocketChannel socketChannel){
        return inputShutdownChannelSet.contains(socketChannel);
    }





    //=======================================================================================================================================
    public static ProxyTools getInstance(){
        if(proxyTools ==null){
            synchronized (ProxyTools.class){
                if(proxyTools ==null){
                    proxyTools =new ProxyTools();
                }
            }
        }
        return proxyTools;
    }

    public ServerSocketChannel getProxyServerSocketChannel() {
        return proxyServerSocketChannel;
    }

    public Map<SocketChannel, SocketChannel> getLocalRemoteChannelsMap() {
        return localRemoteChannelsMap;
    }

    public Map<SocketChannel, SocketChannel> getRemoteLocalChannelsMap() {
        return remoteLocalChannelsMap;
    }

}
