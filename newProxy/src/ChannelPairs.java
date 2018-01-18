import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

public class ChannelPairs {

    private class ChannelPair{
        private SocketChannel localChannel;
        private SocketChannel remoteChannel;

        public ChannelPair(SocketChannel localChannel) {
            this.localChannel = localChannel;
            remoteChannel=null;
        }
    }

    LinkedList<ChannelPair> channelPairList=new LinkedList<>();

    public void createNewPair(SocketChannel localChannel){
        ChannelPair channelPair=new ChannelPair(localChannel);
        channelPairList.add(channelPair);
    }

    public void setPair(SocketChannel local,SocketChannel remote){
        for (ChannelPair channelPair:channelPairList
                ) {
            if(channelPair.localChannel.equals(local)){
                channelPair.remoteChannel=remote;
            }
        }
    }

    /**
     * 当客户端或者服务器关闭socket后
     * 调用此方法关闭local和remote，并且删除引用
     * @param channel
     */
    public void removePair(SocketChannel channel){
        for (ChannelPair channelPair:channelPairList
                ) {
            if(channelPair.localChannel.equals(channel)||channelPair.remoteChannel.equals(channel)){
                try {
                    if(channelPair.remoteChannel!=null){
                        System.out.println("remove: "+channelPair.remoteChannel);
                        channelPair.remoteChannel.close();
                    }
                    if(channelPair.localChannel!=null){
                        System.out.println("remove: "+channelPair.localChannel);
                        channelPair.localChannel.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                channelPairList.remove(channelPair);
                return;
            }
        }
    }

    /**
     * 如果还没有进行过与server的通信
     * 那么，会返回null；
     * @param local
     * @return 与local对应的remotechannel
     */
    public SocketChannel getRemote(SocketChannel local){
        for (ChannelPair channelPair:channelPairList
             ) {
            if(channelPair.localChannel.equals(local)){
                return channelPair.remoteChannel;
            }
        }
        return null;
    }

    /**
     * 应该不会出现null的情况
     * @param remote
     * @return 与remote对应的localchannel;
     */
    public SocketChannel getLocal(SocketChannel remote){
        for (ChannelPair channelPair:channelPairList
                ) {
            if(channelPair.remoteChannel.equals(remote)){
                return channelPair.localChannel;
            }
        }
        return null;
    }
}
