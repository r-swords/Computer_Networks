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
        String message = getMessage(packet);
        String sensorName = sensorNames.get((InetSocketAddress) packet.getSocketAddress());
        ArrayList<InetSocketAddress> list = sensorSubscriptions.get(sensorName);
        if(list.size() > 0) {
            DatagramPacket sendPacket = new DatagramPacket(
                    message.getBytes(StandardCharsets.UTF_8), message.length(), list.get(0));
            socket.send(sendPacket);
        }
    }

    public synchronized void sendMessage(String message, InetSocketAddress dstAddress) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(message.getBytes(StandardCharsets.UTF_8), message.length(), dstAddress);
        socket.send(sendPacket);
    }

    @Override
    public void onReceipt(DatagramPacket packet) throws IOException {
        switch (packet.getData()[0]){
            case INITIALISE_SENSOR:
                String sensorName = getMessage(packet);
                if(!sensorSubscriptions.containsKey(sensorName)) {
                    sensorNames.put((InetSocketAddress) packet.getSocketAddress(), sensorName);
                    ArrayList<InetSocketAddress> subscriberList = new ArrayList<>();
                    sensorSubscriptions.put(sensorName, subscriberList);
                    sendMessage("true", (InetSocketAddress) packet.getSocketAddress());
                }
                else sendMessage("false", (InetSocketAddress) packet.getSocketAddress());
                break;
            case SUBSCRIBE:
                String topicName = getMessage(packet);
                if(sensorSubscriptions.containsKey(topicName)) {
                    ArrayList<InetSocketAddress> list = sensorSubscriptions.get(topicName);
                    list.add((InetSocketAddress) packet.getSocketAddress());
                    sensorSubscriptions.put(topicName, list);
                }
                else sendMessage("fALSE", (InetSocketAddress) packet.getSocketAddress());
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
