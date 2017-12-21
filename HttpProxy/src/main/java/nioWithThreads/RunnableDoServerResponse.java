package nioWithThreads;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class RunnableDoServerResponse implements Runnable{

    @Override
    public void run(){
        ProxyTools proxyTools=ProxyTools.getInstance();
        Map<SocketChannel,SocketChannel> remoteLocalChannelsMap=proxyTools.getRemoteLocalChannelsMap();
        Selector selector= null;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(true){
            Set<SocketChannel> remoteChannels= remoteLocalChannelsMap.keySet();
            if(remoteChannels.size()==0){//防止因为没有需要seclet的channel而阻塞
                continue;
            }
//            System.out.println("select的本地Channel个数："+localChannelSet.size());
            try {

                for (SocketChannel remoteChannelCell:remoteChannels
                        ) {
                    remoteChannelCell.register(selector, SelectionKey.OP_READ);
                }

                if((selector.select(500000))!=0){
//                    System.out.println("有新请求");
                    Set selectedKeys=selector.selectedKeys();
                    Iterator iterator=selectedKeys.iterator();
                    while(iterator.hasNext()){
                        SelectionKey key=(SelectionKey) iterator.next();
                        SocketChannel remoteChannel=(SocketChannel) key.channel();
                        byte[] responseArray=ChannelUtils.readFromChannel(remoteChannel);
                        if(responseArray!=null){//如果读到了reponse的array
                            //判断从代理到服务器的连接是否存在
                            SocketChannel localChannel=remoteLocalChannelsMap.get(remoteChannel);
                            //下面进行传输：
                            ChannelUtils.sendToChannel(localChannel,responseArray);
                            System.out.println("传输完成");



                        }else{
                            //todo:考虑销毁这个已经关闭的连接
                            ChannelUtils.closeChannels(remoteChannel);
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
