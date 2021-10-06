import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class Broker extends Node {
    Terminal terminal;
    HashMap<String, Topic> topicSubscriptions;
    HashMap<InetSocketAddress, String> topicNames;
    InetSocketAddress dstAddress;

    Broker (String dstHost, int dstPort, int srcPort){
        topicSubscriptions = new HashMap<>();
        topicNames = new HashMap<>();
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
        String topicName = topicNames.get((InetSocketAddress) packet.getSocketAddress());
        Topic topic = topicSubscriptions.get(topicName);
        ArrayList<InetSocketAddress> list = topic.getSubscriberList(getTopicNumber(packet) - 1);
        for(int i = 0; i < list.size(); i++){
            DatagramPacket sendPacket = new DatagramPacket(
                    message.getBytes(StandardCharsets.UTF_8), message.length(), list.get(i));
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
                if(!topicSubscriptions.containsKey(sensorName)) {
                    topicNames.put((InetSocketAddress) packet.getSocketAddress(), sensorName);
                    Topic newTopic = new Topic();
                    topicSubscriptions.put(sensorName, newTopic);
                    sendMessage("true", (InetSocketAddress) packet.getSocketAddress());
                }
                else sendMessage("false", (InetSocketAddress) packet.getSocketAddress());
                break;
            case SUBSCRIBE:
                String message = getMessage(packet);
                String[] messageArray = message.split(" ");
                if(topicSubscriptions.containsKey(messageArray[0])) {
                    Topic list = topicSubscriptions.get(messageArray[0]);
                    if(list.addSubscriber(messageArray[1], (InetSocketAddress) packet.getSocketAddress())) {
                        topicSubscriptions.put(messageArray[0], list);
                    }
                    else sendMessage("fALSE", (InetSocketAddress) packet.getSocketAddress());
                }
                else sendMessage("fALSE", (InetSocketAddress) packet.getSocketAddress());
                break;
            case MESSAGE:
                sendMessage(packet);
                break;
            case CREATE_SUBTOPIC:
                String topicName = topicNames.get((InetSocketAddress) packet.getSocketAddress());
                Topic topic = topicSubscriptions.get(topicName);
                topic.addTopic(getMessage(packet));
        }
        terminal.println("Received message");
    }

    public static void main(String[] args){
        try{
            (new Broker("localhost", 50003, 50001)).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
