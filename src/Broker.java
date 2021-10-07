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
        ArrayList<InetSocketAddress> list = (getTopicNumber(packet) != 0)?
                topic.getSubscriberList(getTopicNumber(packet) - 1): topic.getAll();
        for (InetSocketAddress inetSocketAddress : list) {
            DatagramPacket sendPacket = new DatagramPacket(
                    message.getBytes(StandardCharsets.UTF_8), message.length(), inetSocketAddress);
            socket.send(sendPacket);
        }
    }

    public synchronized void sendMessage(String message, InetSocketAddress dstAddress) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(message.getBytes(StandardCharsets.UTF_8), message.length(), dstAddress);
        socket.send(sendPacket);
    }

    public synchronized void initialiseTopic(DatagramPacket packet) throws IOException {
        String sensorName = getMessage(packet);
        if(!topicSubscriptions.containsKey(sensorName)) {
            topicNames.put((InetSocketAddress) packet.getSocketAddress(), sensorName);
            Topic newTopic = new Topic();
            topicSubscriptions.put(sensorName, newTopic);
            sendMessage("true", (InetSocketAddress) packet.getSocketAddress());
        }
        else sendMessage("false", (InetSocketAddress) packet.getSocketAddress());
    }

    public synchronized  void subscribe(DatagramPacket packet) throws IOException {
        String message = getMessage(packet);
        String[] messageArray = message.split(" ");
        if(topicSubscriptions.containsKey(messageArray[0])) {
            Topic list = topicSubscriptions.get(messageArray[0]);
            if(messageArray.length == 2) {
                if (list.addSubscriber(messageArray[1], (InetSocketAddress) packet.getSocketAddress())) {
                    topicSubscriptions.put(messageArray[0], list);
                } else sendMessage("fALSE", (InetSocketAddress) packet.getSocketAddress());
            }
            else if(messageArray.length == 1) {
                list.addSubscriber((InetSocketAddress) packet.getSocketAddress());
            }
        }
        else sendMessage("fALSE", (InetSocketAddress) packet.getSocketAddress());
    }


    @Override
    public void onReceipt(DatagramPacket packet) throws IOException {
        switch (packet.getData()[0]){
            case INITIALISE_SENSOR:
                initialiseTopic(packet);
                break;
            case SUBSCRIBE:
                subscribe(packet);
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
