package nioWithThreads;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 用于建立客户端--代理--web服务器连接的线程
 */
public class RunnableAccept implements Runnable{
    @Override
    public void run() {
        ProxyTools proxyTools = ProxyTools.getInstance();
        ServerSocketChannel proxyServerSocketChannel= proxyTools.getProxyServerSocketChannel();
        Map<SocketChannel,SocketChannel> localRemoteSocketChannelsMap= proxyTools.getLocalRemoteChannelsMap();
        try {
            Selector selector=Selector.open();
            while(true){
                proxyServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                if(selector.select(500000)!=0){
                    Set<SelectionKey> selectedKeys=selector.selectedKeys();
                    Iterator keyIterator = selectedKeys.iterator();
                    while(keyIterator.hasNext()){
                        keyIterator.next();
                        //因为只注册了一个serverSocketChannel，所以一定是处理accept
                        SocketChannel localChannel=proxyServerSocketChannel.accept();
                        localChannel.configureBlocking(false);
                        //System.out.println(localChannel);

                        //将socket加入到localRemoteSocketChannelsMap
                        //需要进行同步
                        synchronized (localRemoteSocketChannelsMap){
                            //这里做的处理是还没有建立到server的连接
                            //就把localchannel作为key和value放进map
                            //也就是如果以后要要判断是否建立了连接，就get（key） 看value和可以是否相同
                            localRemoteSocketChannelsMap.put(localChannel,localChannel);
//                            System.out.println("加入新的本地Channel："+localChannel);
//                            System.out.println("现在本地chanel个数："+localRemoteSocketChannelsMap.size());
                        }
                        keyIterator.remove();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
