import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 负责接收浏览器连接和建立远程连接，并将本地-远程连接pair根据hashcode分配到worker线程。
 */
public class Acceptor implements Runnable{
    private int numWorker;
    private ServerSocketChannel proxyChannel;

    //共享
    private List<ConcurrentLinkedQueue<SocketChannel>> localChannelQueues;
    private List<Selector> workerSelectors;

    public Acceptor(int numWorker) {
        try {
            //创建服务器代理socketchannel
            proxyChannel = ServerSocketChannel.open();
            proxyChannel.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(),1081));
            proxyChannel.configureBlocking(false);


            localChannelQueues=new CopyOnWriteArrayList<>();
            workerSelectors=new CopyOnWriteArrayList<>();
            //使用线程安全的queue存储建立的localChannel；
            for (int i = 0; i <numWorker ; i++) {
                localChannelQueues.add(new ConcurrentLinkedQueue<>());
                try {
                    workerSelectors.add(Selector.open());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            this.numWorker=numWorker;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ConcurrentLinkedQueue<SocketChannel>> getLocalChannelQueues() {
        return localChannelQueues;
    }

    public List<Selector> getWorkerSelectors() {
        return workerSelectors;
    }

    @Override
    public void run() {
        //创建selector
        try (Selector acceptAndEstablishSelector = Selector.open()) {
            proxyChannel.register(acceptAndEstablishSelector, SelectionKey.OP_ACCEPT);
            while(true){
                //循环selctor
                int numKey=acceptAndEstablishSelector.select(50000);
                if(numKey>0){
                    Set<SelectionKey> keys=acceptAndEstablishSelector.selectedKeys();
                    Iterator<SelectionKey> keyIterator=keys.iterator();
                    while(keyIterator.hasNext()){
                        SelectionKey key=keyIterator.next();
                        //proxyChannel接收来自浏览器的连接
                        if(key.isAcceptable()){
                            SocketChannel localChanel=proxyChannel.accept();
                            localChanel.configureBlocking(false);
                            //将接收的localChanel加入队列
                            //是使用hashtable的实现方式，使用hashcode然后把localchannel加入对应的queue。
                            int hash = localChanel.hashCode();
                            int index = (hash & 0x7FFFFFFF) % numWorker;
                            localChannelQueues.get(index).add(localChanel);
                            workerSelectors.get(index).wakeup();
                            System.out.println("accept: "+localChanel);
                        }
//                        if(key.isReadable()){
//                            SocketChannel localChannel=(SocketChannel) key.channel();
//                           int numRead= localChannel.read(byteBuffer);
//                           if(numRead==-1){//读到流的结束
//                               localChannel.close();
//                           }
//                           if(numRead==0){
//                               //todo:读到0？
//                               System.err.println("fuck:第一次读local就读到0？？？？？");
//                           }
//                           if(numRead>0){
//
//                           }
//                           byteBuffer.clear();
//                        }

                        keyIterator.remove();
                    }
                }
            }

        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
