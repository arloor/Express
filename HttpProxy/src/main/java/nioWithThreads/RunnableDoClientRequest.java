package nioWithThreads;

import nioWithThreads.model.Request;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 用于向服务器发送请求
 */
public class RunnableDoClientRequest implements Runnable{

    @Override
    public void run(){
        ProxyTools proxyTools=ProxyTools.getInstance();
        Map<SocketChannel,SocketChannel> localRemoteChannelsMap=proxyTools.getLocalRemoteChannelsMap();
        Map<SocketChannel,SocketChannel> remoteLocalChannelsMap=proxyTools.getRemoteLocalChannelsMap();
        Selector selector= null;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(true){
            Set<SocketChannel> localChannelSet= localRemoteChannelsMap.keySet();
            if(localChannelSet.size()==0){//防止因为没有需要seclet的channel而阻塞
                continue;
            }

            try {
                int num=0;
                for (SocketChannel localChannelCell:localChannelSet
                        ) {
                    if(!proxyTools.isInputShutdown(localChannelCell)) {
                        localChannelCell.register(selector, SelectionKey.OP_READ);
                        num++;
                    }
                }
//                System.out.println("select的本地Channel个数："+num);

                if((selector.select(500000))!=0){
//                    System.out.println("有新请求");
                    Set selectedKeys=selector.selectedKeys();
                    Iterator iterator=selectedKeys.iterator();
                    while(iterator.hasNext()){
                        SelectionKey key=(SelectionKey) iterator.next();
                        SocketChannel localChannel=(SocketChannel) key.channel();
                        byte[] requestArray=ChannelUtils.readFromChannel(localChannel);
                        if(requestArray!=null){//如果读到了request的array
                            //判断从代理到服务器的连接是否存在
                            SocketChannel remoteChannel=localRemoteChannelsMap.get(localChannel);
                            if(remoteChannel==localChannel){//这说明不存在
                                //下面创建连接
                                Request request=new Request(requestArray);
//                                System.out.println(request);
                                String host=request.getHost();
                                int port=request.getPort();
                                remoteChannel=SocketChannel.open(new InetSocketAddress(host,port));
                                remoteChannel.configureBlocking(false);

                                //保存到mappng中
                                //下面是使用线程安全的map，应该不需要同步吧。
                                remoteLocalChannelsMap.put(remoteChannel,localChannel);
                                localRemoteChannelsMap.put(localChannel,remoteChannel);
                            }
                            //下面进行传输：
                            ChannelUtils.sendToChannel(remoteChannel,requestArray);
//                            System.out.println("传输完成");



                        }else{
                            //todo:考虑销毁这个已经关闭的连接
                            ChannelUtils.closeChannels(localChannel);
                        }
                        iterator.remove();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
