package nioWithThreads;

public class Main {
    public static void main(String[] args){
        //开启接受本地浏览器socket的线程
        //！！这个线程只能有一个，否则需要修改RunnableAccept增加同步
        //！！事实上没有必要，因为使用了select
        RunnableAccept runnableAccept =new RunnableAccept();
        Thread threadAccept=new Thread(runnableAccept);
        threadAccept.start();

        RunnableDoClientRequest runnableDoClientRequest= new RunnableDoClientRequest();
        Thread threadDoClientRequest=new Thread(runnableDoClientRequest);
        threadDoClientRequest.start();

        RunnableDoServerResponse runnableDoServerResponse=new RunnableDoServerResponse();
        Thread threadDoServerResponse=new Thread(runnableDoServerResponse);
        threadDoServerResponse.start();

        RunnableDestroy runnableDestroy=new RunnableDestroy();
        Thread threadDestroy=new Thread(runnableDestroy);
        threadDestroy.start();

    }
}
