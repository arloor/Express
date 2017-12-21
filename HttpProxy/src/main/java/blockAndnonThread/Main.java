package blockAndnonThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//http://detectportal.firefox.com/success.txt
//让以上url通过代理测试成功

public class Main {
    public static void main(String[] args) {
        ExecutorService executor= Executors.newFixedThreadPool(100);
        try(ServerSocket ss=new ServerSocket(2222)) {

            while(true){
                //获取浏览器socket
                Socket localSocket=ss.accept();
                //开启service线程
                ThreadService serviceThread=new ThreadService(localSocket);
               executor.submit(serviceThread);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
