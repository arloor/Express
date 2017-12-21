package nioWithThreads.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class Request {
    private String requestLine;
    private String method;
    private String path;
    private String protocol;
    private String host;
    private int port;
    private List<Header> headers;
    private byte[] requestByteArray;

    private byte[] body;


    public Request(byte[] requestArry){
        requestByteArray=requestArry;
        String requsetStr=new String(requestArry);
        //找到body
        int indexBodyStart=requsetStr.indexOf("\r\n\r\n")+4;
        byte[] body=new byte[requestArry.length-indexBodyStart];
        for (int i = indexBodyStart; i < body.length; i++) {
            body[i-indexBodyStart]=requestArry[i];
        }
        //找到剩下的部分
        String rest=requsetStr.substring(0,indexBodyStart);
        String[] lines=rest.split("\r\n");
        String requestLine=lines[0];
        List<Header> headers=new LinkedList<>();
        for (int i = 1; i <lines.length ; i++) {
            int index=lines[i].indexOf(": ");
            String key=lines[i].substring(0,index);
            String value=lines[i].substring(index+2);
            Header header=new Header(key,value);
            headers.add(header);
        }

        this.requestLine = requestLine;
        this.headers = headers;
        this.body = body;

        String requestLineSplit[] =requestLine.split(" ");
        method=requestLineSplit[0];
        path=requestLineSplit[1];
        protocol=requestLineSplit[2];

        //todo
        if(method.equals("CONNECT")){
            host=path.split(":")[0];
            port=Integer.parseInt(path.split(":")[1]);
        }else{
            try {
                URL url=new URL(path);
                host=url.getHost();
                port=url.getPort();
                if(port==-1){
                    port=url.getDefaultPort();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getHeader(String key){
        for (Header header:headers
                ) {
            if(header.getKey().equals(key)){
                return header.getValue();
            }
        }
        return null;
    }

    public String getRequestLine() {
        return requestLine;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public byte[] getRequestByteArray() {
        return requestByteArray;
    }

    @Override
    public String toString() {
        StringBuilder builder=new StringBuilder();
        builder.append("向"+host+"发送的请求如下:body部分可能乱码，是正常现象\t\n");
        builder.append("=============================\r\n");
        builder.append(requestLine+"\r\n");
        for (Header header:headers
                ) {
            builder.append(header.getKey());
            builder.append(": ");
            builder.append(header.getValue());
            builder.append("\r\n");
        }
        builder.append("\r\n");
        builder.append(new String(body));
        builder.append("\n");
        return builder.toString();
    }


}