import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Broker extends Node {
    Terminal terminal;
    HashMap<String, ArrayList<InetSocketAddress>> sensorSubscriptions;
    HashMap<InetSocketAddress, String> sensorNames;
    InetSocketAddress dstAddress;

    Broker (String dstHost, int dstPort, int srcPort){
        sensorSubscriptions = new HashMap<String, ArrayList<InetSocketAddress>>();
        sensorNames = new HashMap<InetSocketAddress, String>();
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
        byte[] messageArray = new byte[packet.getData().length];
        System.arraycopy(packet.getData(), 6, messageArray, 0,
                packet.getData().length-6);
        String message = new String(messageArray).trim();
        String sensorName = sensorNames.get((InetSocketAddress) packet.getSocketAddress());
        ArrayList<InetSocketAddress> list = sensorSubscriptions.get(sensorName);
        DatagramPacket sendPacket = new DatagramPacket(
                message.getBytes(StandardCharsets.UTF_8), message.length(), dstAddress);
        socket.send(sendPacket);
    }

    @Override
    public void onReceipt(DatagramPacket packet) throws IOException {
        switch (packet.getData()[0]){
            case INITIALISE_SENSOR:
                byte[] sensorNameArray = new byte[packet.getData().length];
                System.arraycopy(packet.getData(), 6, sensorNameArray, 0,
                        packet.getData().length-6);
                String sensorName = new String(sensorNameArray).trim();
                sensorNames.put((InetSocketAddress) packet.getSocketAddress(), sensorName);
                ArrayList<InetSocketAddress> subscriberList = new ArrayList<>();
                sensorSubscriptions.put(sensorName, subscriberList);
                break;
            case SUBSCRIBE:
                byte[] topicNameArray = new byte[packet.getData().length];
                System.arraycopy(packet.getData(), 6, topicNameArray, 0,
                        packet.getData().length-6);
                String topicName = new String(topicNameArray).trim();
                ArrayList<InetSocketAddress> list = sensorSubscriptions.get(topicName);
                list.add((InetSocketAddress) packet.getSocketAddress());
                sensorSubscriptions.put(topicName,list);
                break;
            case MESSAGE:
                sendMessage(packet);
        }
        terminal.println("Recieved message");
    }

    public static void main(String[] args){
        try{
            (new Broker("localhost", 50003, 50001)).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
