import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Bootstrap {
    public static void main(String[] args){
        int numWorker=20;

        Acceptor acceptor=new Acceptor(numWorker);
        List<ConcurrentLinkedQueue<SocketChannel>> localChannelQueues=acceptor.getLocalChannelQueues();
        List<Selector> workerSelectors=acceptor.getWorkerSelectors();

        //开启接受本地连接的线程
        Thread threadAccept=new Thread(acceptor);
        threadAccept.start();

        //开启worker（处理数据交换的线程）
        ExecutorService executor= Executors.newFixedThreadPool(numWorker);
        for (int i = 0; i <numWorker ; i++) {
            //创建i对应的queue的worker，提交任务
            ConcurrentLinkedQueue<SocketChannel> channels=localChannelQueues.get(i);
            Selector workerSelector=workerSelectors.get(i);
            Worker worker=new Worker(channels,workerSelector);
            executor.submit(worker);
        }


//        ConcurrentLinkedQueue<SocketChannel> channels=localChannelQueues.get(0);
//            Worker worker=new Worker(channels);
//        try {
//            worker.call();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
