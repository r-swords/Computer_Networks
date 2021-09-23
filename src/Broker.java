import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class Broker extends Node {
    Terminal terminal;
    InetSocketAddress dstAddress;

    Broker (String dstHost, int dstPort, int srcPort){
        try {
            terminal = new Terminal("Broker");
            socket = new DatagramSocket(srcPort);
            dstAddress = new InetSocketAddress(dstHost, dstPort);
            listener.go();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public synchronized void start() throws InterruptedException {
        terminal.println("waiting for message");
        while(true) {
            this.wait();
        }
    }

    public synchronized void sendMessage(DatagramPacket packet) throws IOException {
        String message = new String(packet.getData()).trim();
        DatagramPacket sendPacket = new DatagramPacket(
                message.getBytes(StandardCharsets.UTF_8), message.length(), dstAddress);
        socket.send(sendPacket);
    }

    @Override
    public void onReceipt(DatagramPacket packet) throws IOException {
        terminal.println("Recieved message");
        sendMessage(packet);
    }

    public static void main(String[] args){
        try{
            (new Broker("localhost", 50003, 50001)).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
