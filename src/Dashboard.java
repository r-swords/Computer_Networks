import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class Dashboard extends Node{

    Terminal terminal;
    InetSocketAddress dstAddress;

    public static void main(String[] args){
        try{
            (new Dashboard("localhost", 50001, 50003)).start();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    Dashboard (String dstHost, int dstPort, int srcPort){
        try {
            terminal = new Terminal("Dashboard");
            dstAddress = new InetSocketAddress(dstHost, dstPort);
            socket = new DatagramSocket(srcPort);
            listener.go();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void start() throws InterruptedException, IOException {
        while(true) {
            String topic = terminal.read("Subscribe to topic, " +
                    "subscription requests should be in the form of '<Topic> <Sub-Topic' : ");
            terminal.println("Subscribe to topic, subscription requests should be in the form of " +
                    "'<Topic> <Sub-Topic' : " + topic);
            DatagramPacket newSubscription = createPacket(SUBSCRIBE, topic, dstAddress, -1);
            socket.send(newSubscription);
        }
    }

    @Override
    public synchronized void onReceipt(DatagramPacket packet) {
        byte[] message = packet.getData();
        String printMessage = new String(message).trim();
        if(!printMessage.equals("fALSE")) {
            terminal.println(printMessage);
        }
        else terminal.println("Topic doesn't exist");
    }
}
