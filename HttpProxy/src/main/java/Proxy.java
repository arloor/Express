
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class Proxy {

   private Map<String, Tunnel> tunnels;
   private List<ChanelAndBuff> unTunneledLocalChannels;//尚未建立代理链接的localchannel
   private ServerSocketChannel proxySocketChanel;
   private ByteBuffer proxyBuff;

    public Proxy(String proxyPortStr) {
        tunnels = new HashMap<>();
        unTunneledLocalChannels=new LinkedList<>();
        int proxyPort=Integer.parseInt(proxyPortStr);
        try {
            proxySocketChanel=ServerSocketChannel.open();
            proxySocketChanel.bind(new InetSocketAddress(proxyPort));
            proxySocketChanel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("在端口"+proxyPort+"建立ProxySocket失败");
            System.out.println("程序退出");
            System.exit(-1);
        }
        proxyBuff=ByteBuffer.allocate(8192);
    }

    public static void main(String[] args){
        //读取命令行参数，初始化proxy对象
        Proxy proxy=null;
        if(args.length<1||Integer.parseInt(args[0])<=1024||Integer.parseInt(args[0])>65535){
            System.out.println("请在命令行参数中指定代理服务器的端口(1025-65535)");
            return;
        }else{
            proxy=new Proxy(args[0]);
            System.out.println("在端口"+args[0]+"建立ProxySocket成功");
        }
        Selector selector=null;
        try {
            selector=Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //proxy开始服务
        while(true){
            try {
                //注册chanel到selecter
                proxy.proxySocketChanel.register(selector,SelectionKey.OP_ACCEPT);
                for (Map.Entry<String, Tunnel> cell:proxy.tunnels.entrySet()
                     ) {
                    Tunnel tunnel =cell.getValue();
                    ChanelAndBuff localChanelAndBuff= tunnel.getLocal();
                    SelectionKey localkey=localChanelAndBuff.register(selector,SelectionKey.OP_READ);
                    localkey.attach("local:"+tunnel.getHostName());  //添加hostName信息；
                    ChanelAndBuff remoteChanelAndBuff= tunnel.getRemote();
                    if(remoteChanelAndBuff!=null){
                        SelectionKey remoteKey=remoteChanelAndBuff.register(selector,SelectionKey.OP_READ);
                        remoteKey.attach("remote:"+tunnel.getHostName());//添加hostName信息；
                    }
                }
                //对未建立代理通道的localChanel增加注册
                for (ChanelAndBuff channel:proxy.unTunneledLocalChannels
                     ) {
                    SelectionKey key=channel.register(selector,SelectionKey.OP_READ);
                    key.attach(null);//添加hostName信息；这里是null
                }


                int num=selector.select();
                if(num!=0){
                    Set<SelectionKey> selectedKeys=selector.selectedKeys();
                    Iterator keyIterator = selectedKeys.iterator();
                    while(keyIterator.hasNext()) {
                        SelectionKey key = (SelectionKey)keyIterator.next();
                        if(key.isAcceptable()) {
                            //System.out.println(key.channel());
                            //从proxyChannel获取本地浏览的socketChanel；
                            SocketChannel localChannel = ((ServerSocketChannel) key.channel()).accept();
                            localChannel.configureBlocking(false);
                            ChanelAndBuff local=new ChanelAndBuff(localChannel);
                            proxy.unTunneledLocalChannels.add(local);
                        } else if (key.isConnectable()) {
                            // a connection was established with a remote server.
                        } else if (key.isReadable()) {
                            String attachmentStr=(String)key.attachment();
                            if(attachmentStr!=null){//说明这个是一个已经tunnel的连接。
                                String hostName=attachmentStr.substring(attachmentStr.indexOf(":")+1);
                                String localremote=attachmentStr.substring(0,attachmentStr.indexOf(":"));
                                for (Map.Entry<String,Tunnel> tunnelEntry:proxy.tunnels.entrySet()
                                     ) {

                                    if(tunnelEntry.getKey().equals(hostName)){//找到tunnel
                                        System.out.println("复用tunnel");
                                        Tunnel tunnel=tunnelEntry.getValue();

                                        if(localremote.equals("local")){//是本地发送的请求
                                            Request request=proxy.readRequestFromChannel(tunnel.getLocal());
                                            if(request!=null){

                                                //向远程写request
                                                ByteBuffer remotebuff=tunnel.getRemote().getBuff();
                                                SocketChannel remoteChannel=tunnel.getRemote().getChannel();
                                                remotebuff=ByteBuffer.wrap(request.getRequestByteArray());
                                                remoteChannel.write(remotebuff);
                                                remotebuff.flip();
                                            }else {
                                                //删除这个tunnel
                                                proxy.tunnels.remove(hostName);
                                                System.out.println("删除"+hostName+"的tunnel");
                                            }
                                        }else{//是远程发送的响应
                                            byte[] response=proxy.readFromChannelAndBuff(tunnel.getRemote());
                                            tunnel.getLocal().getChannel().write(ByteBuffer.wrap(response));
                                        }



                                        break;
                                    }
                                }
                            }else{//这是一个尚未tunnel的channel
                                //这个刻度的channel一定是local的
                                SocketChannel local=(SocketChannel)key.channel();
                                for (ChanelAndBuff localChanelAndBuff:proxy.unTunneledLocalChannels//找到对应的ChanelAndBuff对象
                                        ) {
                                    if(localChanelAndBuff.getChannel()==local){
                                        //下面要负责建立tunnel
                                        Request request=proxy.readRequestFromChannel(localChanelAndBuff);
//                                        System.out.println("第一次尚未建立tunnel");
                                        //创建tunnel
                                        SocketChannel remote=SocketChannel.open(new InetSocketAddress(request.getHost(),request.getPort()));
                                        remote.configureBlocking(false);
                                        ChanelAndBuff remoteChannelAndBuff=new ChanelAndBuff(remote);
                                        Tunnel tunnel=new Tunnel(localChanelAndBuff,remoteChannelAndBuff,request.getHost());
                                        proxy.unTunneledLocalChannels.remove(localChanelAndBuff);
                                        proxy.tunnels.put(request.getHost(),tunnel);


                                        //向远程写request
                                        ByteBuffer remotebuff=tunnel.getRemote().getBuff();
                                        SocketChannel remoteChannel=tunnel.getRemote().getChannel();
                                        remotebuff=ByteBuffer.wrap(request.getRequestByteArray());
                                        remoteChannel.write(remotebuff);
                                        remotebuff.flip();
                                        break;
                                    }
                                }
                            }
                        } else if (key.isWritable()) {
                            // a channel is ready for writing
                        }
                        keyIterator.remove();
                    }
                }
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] readFromChannelAndBuff(ChanelAndBuff chanelAndBuff){
        SocketChannel socketChannel=chanelAndBuff.getChannel();
        ByteBuffer buff=chanelAndBuff.getBuff();
        byte[] result=readFromChannel(socketChannel,buff);
        return result;
    }

    private byte[] readFromChannel(SocketChannel socketChannel,ByteBuffer buff) {
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

        return result;
    }

    private Request readRequestFromChannel(ChanelAndBuff chanelAndBuff){
        //这里是客户端关闭，所以会读到0个字节。
        //Connection: close 客户端会发这个头
        //这里要删除关闭的tunnel
        byte[] result= readFromChannelAndBuff(chanelAndBuff);
        if(result.length==0){
            return null;
        }
        Request request=new Request(result);
        return request;
    }
}
