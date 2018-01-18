
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Worker implements Callable<Void>{
    //共享
    private ConcurrentLinkedQueue<SocketChannel> socketChannels;

    //封闭在线程内
    private ChannelPairs  channelPairs=new ChannelPairs();

    Selector selector;

    ByteBuffer buffer=ByteBuffer.allocate(8912);




    public Worker(ConcurrentLinkedQueue<SocketChannel> socketChannels, Selector workerSelector) {
        this.socketChannels = socketChannels;
        selector =workerSelector;
    }

    @Override
    public Void call() throws Exception {
        while (true){
            SocketChannel newLocalChannel;
            //从队列中取出未pair的socketChannel。
            while((newLocalChannel=socketChannels.poll())!=null){
                channelPairs.createNewPair(newLocalChannel);
                newLocalChannel.register(selector, SelectionKey.OP_READ);
            }

            int numKey=selector.select(50000000);
            if(numKey>0){
                Set<SelectionKey> keys=selector.selectedKeys();
                Iterator<SelectionKey> keyIterator=keys.iterator();
                while(keyIterator.hasNext()){
                    SelectionKey key=keyIterator.next();
                    if(key.isReadable()){
                        SocketChannel socketChannel= (SocketChannel) key.channel();
                        int numRead=socketChannel.read(buffer);
                        System.out.println("read "+numRead+" from "+socketChannel.getRemoteAddress());
                        if(numRead==-1){
                            //发送方是localchannel
                            //发现firefox和chrome经代理发送请求的ip不同，chrome使用127.0.0.1，Firefox使用了192.168.0.3（个人设置的局域网ip）
                            //所以下面使用的方式看是否来自127.0.0.1或者192.168.0.3来判断是否来自本地比较合理。
                            if(socketChannel.getRemoteAddress().toString().contains(InetAddress.getLocalHost().getHostAddress())||socketChannel.getRemoteAddress().toString().contains(InetAddress.getLoopbackAddress().getHostAddress())){
                                System.out.println("cancel localSoekct: "+socketChannel);
                                key.cancel();
                            }else{
                                channelPairs.removePair(socketChannel);
                            }

                        }

                        if (numRead> 0) {
                            //发送方是localchannel
                            if(socketChannel.getRemoteAddress().toString().contains(InetAddress.getLocalHost().getHostAddress())||socketChannel.getRemoteAddress().toString().contains(InetAddress.getLoopbackAddress().getHostAddress())){
                                SocketChannel remote=channelPairs.getRemote(socketChannel);
                                String read=new String(buffer.array());
//                                System.out.println(read);
                                if(remote==null){//remote为空则需要新建远端。这也说明是此loaclsocket第一发送数据
                                    String firestline=read.substring(0,read.indexOf("\r\n"));//firestline=GET http://detectportal.firefox.com/success.txt HTTP/1.1
                                    String urlStr=firestline.substring(firestline.indexOf(" ")+1,firestline.lastIndexOf(" "));
                                    URL url=new URL(urlStr);
                                    String host=url.getHost();
                                    int port=url.getPort()==-1?url.getDefaultPort():url.getPort();
                                    SocketChannel remoteChannel=SocketChannel.open(new InetSocketAddress(host,port));
                                    remoteChannel.configureBlocking(false);
                                    remoteChannel.register(selector,SelectionKey.OP_READ);
                                    channelPairs.setPair(socketChannel,remoteChannel);
                                    remote=remoteChannel;
                                    //todo：对connect报文的处理
                                }

                                //向远程写：
                                buffer.flip();
                                remote.write(buffer);
                                buffer.clear();

                            }else{//发送方是remote
                                buffer.flip();
                                SocketChannel local=channelPairs.getLocal(socketChannel);
                                System.out.println("write to lcoal:"+local);
//                                System.out.println(new String(buffer.array()));
                                local.write(buffer);
                                buffer.clear();
                            }
                        }
                    }
                }
                keyIterator.remove();
            }
        }
    }
}

