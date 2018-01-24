package nioWithThreads;

import java.nio.channels.SocketChannel;
import java.util.Map;

public class RunnableDestroy implements Runnable{
    @Override
    public void run() {
        ProxyTools proxyTools=ProxyTools.getInstance();
        Map<SocketChannel,SocketChannel> remoteLocalChannelMap=proxyTools.getRemoteLocalChannelsMap();
        Map<SocketChannel,SocketChannel> localRemoteChannelsMap=proxyTools.getLocalRemoteChannelsMap();

        while(true){
            for (Map.Entry<SocketChannel,SocketChannel> entry:remoteLocalChannelMap.entrySet()
                 ) {
                //如果local和remote的读半部都关闭了，就删除这些
                if(proxyTools.isInputShutdown(entry.getKey())&&proxyTools.isInputShutdown(entry.getValue())){
                    proxyTools.removeInputShutdownChannel(entry.getKey());
                    proxyTools.removeInputShutdownChannel(entry.getValue());
                    remoteLocalChannelMap.remove(entry.getKey());
                    localRemoteChannelsMap.remove(entry.getValue());
                    System.out.println("删除了一个链接");
                }
            }
        }
    }
}
