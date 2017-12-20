import java.util.Calendar;
import java.util.Date;

public class Tunnel {
    private ChanelAndBuff local;
    private ChanelAndBuff remote;

    private String hostName;
    private Date timeToClose;

    public Tunnel() {
        //设置超时关闭的时间
        Calendar now =Calendar.getInstance();
        now.add(Calendar.MINUTE,5);
        timeToClose=now.getTime();
        hostName=null;
    }

    public Tunnel(ChanelAndBuff local, ChanelAndBuff remote,String hostName) {
        this();
        this.local = local;
        this.remote = remote;
        this.hostName=hostName;
    }


    public String getHostName() {
        return hostName;
    }

    public ChanelAndBuff getLocal() {
        return local;
    }

    public ChanelAndBuff getRemote() {
        return remote;
    }

    public Date getTimeToClose() {
        return timeToClose;
    }
}
