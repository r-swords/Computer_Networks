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

    public synchronized void start() throws InterruptedException, IOException {
        while(true) {
            String topic = terminal.read("Subscribe to sensor: ");
            DatagramPacket newSubscription = createPacket(SUBSCRIBE, topic, dstAddress);
            socket.send(newSubscription);
            this.wait();
        }
    }

    @Override
    public synchronized void onReceipt(DatagramPacket packet) {
        byte[] message = packet.getData();
        String printMessage = new String(message).trim();
        terminal.println(printMessage);
    }
}
