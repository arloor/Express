package nioWithThreads.model;

import java.util.LinkedList;
import java.util.List;

public class Response {
    private byte[] reponseByteArray;

    private String statusLine;
    private String protocol;
    private int status;
    private String stateInfo;

    private List<Header> headers;
    private byte[] body;

    public Response(byte[] response){
        reponseByteArray =response;
        String responseStr=new String(response);
        //找到body
        int indexBodyStart=responseStr.indexOf("\r\n\r\n")+4;
        byte[] body=new byte[response.length-indexBodyStart];
        for (int i = indexBodyStart; i < body.length; i++) {
            body[i-indexBodyStart]=response[i];
        }
        //找到剩下的部分
        String rest=responseStr.substring(0,indexBodyStart);
        String[] lines=rest.split("\r\n");
        String stateLine=lines[0];
        List<Header> headers=new LinkedList<>();
        for (int i = 1; i <lines.length ; i++) {
            int index=lines[i].indexOf(": ");
            String key=lines[i].substring(0,index);
            String value=lines[i].substring(index+2);
            Header header=new Header(key,value);
            headers.add(header);
        }

        this.statusLine = stateLine;
        this.headers = headers;
        this.body = body;

        String requestLineSplit[] =stateLine.split(" ");
        protocol=requestLineSplit[0];
        status=Integer.parseInt(requestLineSplit[1]);
        stateInfo=requestLineSplit[2];
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
}
