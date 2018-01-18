import java.net.*;

public class TestUrl {
    public static void main(String[] args){
//        URL url;
//
//        {
//            try {
//                url = new URL("http://detectportal.firefox.com/success.txt");
//                System.out.println(url.getPort());
//            } catch (MalformedURLException e) {
//                e.printStackTrace();
//            }
//        }

        InetAddress socketAddress= null;
        try {
            socketAddress = InetAddress.getLoopbackAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(socketAddress.getHostAddress());

    }
}
