import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class ThreadService implements Callable<Void>{
    Socket localSocket;
    BufferedReader localSocketBufferReader = null;
    OutputStream localOutputStream=null;

    public ThreadService(Socket localSocket) {
        this.localSocket = localSocket;
    }

    @Override
    public Void call() {
        List<String> requestBody= getRequstFromLocal();
        byte[] response=sendRequest2RemoteThenGetResponse(requestBody);
        sendResponse2Local(response);

        //关闭localSocket相关的writer、reader和socket本身。
        if(localSocketBufferReader!=null){
            try {
                localSocketBufferReader.close();
                localSocketBufferReader=null;
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(localSocketBufferReader!=null){
                    try {
                        localSocketBufferReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if(localOutputStream!=null){
            try {
                localOutputStream.close();
                localOutputStream=null;
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(localOutputStream!=null){
                    try {
                        localOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        try {
            localSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendResponse2Local(byte[] response) {
        try {
            System.out.println("向本地写结果");
            localOutputStream=localSocket.getOutputStream();
            localOutputStream.write(response);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 读取浏览器发送的请求
     * @return
     */
    private List<String> getRequstFromLocal(){
        //http请求头格式说明：
//        POST http://localhost:8080/prac1/login HTTP/1.1/r/n
//        Host: localhost:8080/r/n
//        User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0/r/n
//        Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8/r/n
//        Accept-Language: zh-CN,en-US;q=0.7,en;q=0.3/r/n
//        Accept-Encoding: gzip, deflate/r/n
//        Referer: http://localhost:8080/prac1/report/r/n
//        Content-Type: application/x-www-form-urlencoded/r/n
//        Content-Length: 23/r/n   定义了下面的请求主体的长度，代码中使用到了这一项
//        Cookie: JSESSIONID=EC39EBD9F492EC97A6FAB247ABAF37AB/r/n
//        Connection: keep-alive/r/n
//        Upgrade-Insecure-Requests: 1/r/n
//        /r/n  请求头结束的空行，很重要，用于http协议判断请求头结束，开始读取Content-Length长的请求主体。如果Content-Length没有定义，则直接发送响应
//        user=asd&password=asdas

        //下面是读取本地浏览器发送的请求
        List<String> requestLines=new LinkedList<>();

        try {
            localSocketBufferReader = new BufferedReader(new InputStreamReader(localSocket.getInputStream()));
            //使用BufferedReader获取line并且在line最后加上\r\n（read到的line是没有的需要自己加上）
            String line=null;
            int contentLength=0;
            while((line= localSocketBufferReader.readLine())!=null&&!line.equals("")){//知道读到一个空行
                if(line.startsWith("Content-Length:")){
                    String contentlenStr=line.split(" ")[1];
                    contentLength=Integer.parseInt(contentlenStr);
                }
                line+="\r\n";//因为readline读到的是不带回车/换行的，所以需要手动增加回车换行
                requestLines.add(line);
            }
            requestLines.add("\r\n");//！！！请求头读完之后加上的那个空行，很重要
            if(contentLength!=0){//如果有定义Content-Length，则还需要增加读取请求主体的代码。
                char[] buff=new char[contentLength];//设置contentLength长的buff用来读请求主体
                int num= localSocketBufferReader.read(buff);//num=contentLength
                requestLines.add(String.valueOf(buff));
            }
            System.out.println("读取请求完毕");
            //在这里br不能close，如果close了那么会视为socketclose了。这有一个潜在问题，不知道怎么办

        } catch (IOException e) {
            e.printStackTrace();
        }
        return requestLines;
    }

    /**
     *
     * @param requestBody
     * @return
     */
    private byte[] sendRequest2RemoteThenGetResponse(List<String> requestBody){

        System.out.println("向远程服务器发送请求");
        String host=null;
        int port=80;
        for (String line:requestBody
             ) {
            if(line.startsWith("Host")){
                line=line.substring(0,line.length()-2);
                String hostPort=line.split(" ")[1];
                if(hostPort.indexOf(':')!=-1){
                    host=hostPort.substring(0,hostPort.indexOf(':'));
                    String portStr=hostPort.substring(hostPort.indexOf(':')+1);
                    port=Integer.parseInt(portStr);
                }else {
                    host=hostPort;
                }
                break;
            }
        }
        System.out.println(host+":"+port);
        if(host!=null){
            //下面两个是需要关闭的资源
            BufferedWriter remoteSocketBufferedWriter=null;
            InputStream remoteSocketInputStream=null;

            Socket remoteSocket=null;
            try {
                InetAddress address=InetAddress.getByName(host);
                String hostIP=address.getHostAddress();
                remoteSocket=new Socket(hostIP,port);
                remoteSocketBufferedWriter=new BufferedWriter(new OutputStreamWriter(remoteSocket.getOutputStream()));
                String requset="";
                for (String line:requestBody
                     ) {
                    requset+=line;
                }
                System.out.println(requset);
                remoteSocketBufferedWriter.write(requset);
                remoteSocketBufferedWriter.flush();

                remoteSocketInputStream=remoteSocket.getInputStream();
                List<Byte> byteList=new ArrayList<>();
                byte[] buff=new byte[3555];
                int numRead=0;
                while((numRead=remoteSocketInputStream.read(buff))!=-1){
                    for (int i = 0; i < numRead; i++) {
                        byteList.add(buff[i]);
                    }
                }
                byte[] result=new byte[byteList.size()];
                for (int i = 0; i < result.length; i++) {
                    result[i]=byteList.get(i);
                }



                System.out.println(new String(result));
                System.out.println("读取响应完毕");
                remoteSocketInputStream.close();
                remoteSocketInputStream=null;
                remoteSocketBufferedWriter.close();
                remoteSocketBufferedWriter=null;
                remoteSocket.close();
                remoteSocket=null;
                return result;
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(remoteSocketInputStream!=null){
                    try {
                        remoteSocketInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(remoteSocketBufferedWriter!=null){
                    try {
                        remoteSocketBufferedWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(remoteSocket!=null){
                    try {
                        remoteSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return null;
    }
}
