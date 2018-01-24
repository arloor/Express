package nioWithThreads;

import nioWithThreads.model.Request;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ChannelUtils {
    /**
     * 从channel中读取byte[]
     * 如果channel（socket）关闭，则返回null
     * @param socketChannel
     * @return
     */
    public static byte[] readFromChannel(SocketChannel socketChannel) {
        ByteBuffer buff=ByteBuffer.allocate(8912);
        List<Byte> readFromLocal=new LinkedList<>();
        while(true){
            int read= 0;
            try {
                read = socketChannel.read(buff);
                buff.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(read==0||read==-1){//读到0个byte或者留结束
                break;//跳出循环
            }
            for (int i = 0; i <read ; i++) {
                readFromLocal.add(buff.array()[i]);
            }
        }
        byte[] result=new byte[readFromLocal.size()];
        for (int i = 0; i <readFromLocal.size() ; i++) {
            result[i]=readFromLocal.get(i);
        }
        //如果读到0个byte，在只有select表示此channel可读才调用此读的前提下，说明此socket被关闭了
        //return null便于后面处理
        if(result.length==0){
            return null;
        }

        return result;
    }

    public static Request getRequestFromBytes(byte[] bytes){
        return new Request(bytes);
    }

    public static  void sendToChannel(SocketChannel channel,byte[] bytes){
        ByteBuffer byteBuffer=ByteBuffer.wrap(bytes);
        try {
            channel.write(byteBuffer);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public static void closeChannels(SocketChannel socketChannel){
//        ProxyTools proxyTools=ProxyTools.getInstance();
//        Map<SocketChannel,SocketChannel> localRemoteChannels=proxyTools.getLocalRemoteChannelsMap();
//        Map<SocketChannel,SocketChannel> remoteLocalChannels=proxyTools.getRemoteLocalChannelsMap();
//
//        if(localRemoteChannels.get(socketChannel)!=null){
//            SocketChannel theOtherSocketChannel=localRemoteChannels.get(socketChannel);
//            try {
//                socketChannel.close();
//                theOtherSocketChannel.close();
//                localRemoteChannels.remove(socketChannel);
//                remoteLocalChannels.remove(theOtherSocketChannel);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }else{
//            SocketChannel theOtherSocketChannel=remoteLocalChannels.get(socketChannel);
//            try {
//                socketChannel.close();
//                theOtherSocketChannel.close();
//                localRemoteChannels.remove(theOtherSocketChannel);
//                remoteLocalChannels.remove(socketChannel);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public static void closeChannels(SocketChannel socketChannel){
        try {
            socketChannel.shutdownInput();
            ProxyTools proxyTools=ProxyTools.getInstance();
            proxyTools.addInputShutdownChannel(socketChannel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
