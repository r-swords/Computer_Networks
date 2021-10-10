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

    public synchronized boolean getAuthorisation(String terminalPrompt){
        boolean valid = false;
        String response = "";
        while(!valid){
            response = terminal.read(terminalPrompt);
            terminal.println(terminalPrompt + response);
            if(response.equalsIgnoreCase("Y") || response.equalsIgnoreCase("N")){
                valid = true;
            }
            else System.out.println("Invalid input");
        }
        return response.equalsIgnoreCase("Y");
    }

    public synchronized void sendMessage(String message, InetSocketAddress dstAddress) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(message.getBytes(StandardCharsets.UTF_8), message.length(), dstAddress);
        socket.send(sendPacket);
    }

    public synchronized void initialiseTopic(DatagramPacket packet) throws IOException {
        String sensorName = getMessage(packet);
        String auth = "action successful";
        if(getAuthorisation("Initialise publisher request from " + sensorName +" (y/n): ")) {
            if (!topicSubscriptions.containsKey(sensorName)) {
                topicNames.put((InetSocketAddress) packet.getSocketAddress(), sensorName);
                Topic newTopic = new Topic();
                topicSubscriptions.put(sensorName, newTopic);
                sendMessage("true", (InetSocketAddress) packet.getSocketAddress());
            } else sendMessage("false", (InetSocketAddress) packet.getSocketAddress());
        }
        else auth = "action rejected";
        DatagramPacket newPacket = createPacket(AUTH, auth, (InetSocketAddress) packet.getSocketAddress(),
                -1);
        socket.send(newPacket);
    }

    public synchronized  void subscribe(DatagramPacket packet, boolean isSubscription) throws IOException {
        String message = getMessage(packet);
        String auth = "action authorised";
        if(getAuthorisation("Subscription request to " + message + " (y/n): ")) {
            String[] messageArray = message.split(" ");
            if (topicSubscriptions.containsKey(messageArray[0])) {
                Topic list = topicSubscriptions.get(messageArray[0]);
                if (messageArray.length == 2) {
                    if (isSubscription) {
                        if (list.addSubscriber(messageArray[1], (InetSocketAddress) packet.getSocketAddress())) {
                            topicSubscriptions.put(messageArray[0], list);
                        } else auth = "action rejected";
                    } else {
                        list.removeSubscriber(messageArray[1], (InetSocketAddress) packet.getSocketAddress());
                        topicSubscriptions.put(messageArray[0], list);
                    }
                } else if (messageArray.length == 1) {
                    if (isSubscription) list.addSubscriber((InetSocketAddress) packet.getSocketAddress());
                    else list.removeSubscriber((InetSocketAddress) packet.getSocketAddress());
                }
            } else auth = "action rejected";
        }
        else auth = "action rejected";
        DatagramPacket newPacket = createPacket(AUTH, auth, (InetSocketAddress) packet.getSocketAddress(),
                -1);
        socket.send(newPacket);
    }

    public synchronized void createSubtopic(DatagramPacket packet) throws IOException {
        String auth = "action accepted";
        if(getAuthorisation("Request to create subtopic " + getMessage(packet))) {
            String topicName = topicNames.get((InetSocketAddress) packet.getSocketAddress());
            Topic topic = topicSubscriptions.get(topicName);
            topic.addTopic(getMessage(packet));
        }
        else auth = "action rejected";
        DatagramPacket newPacket = createPacket(AUTH, auth, (InetSocketAddress) packet.getSocketAddress(),
                -1);
        socket.send(newPacket);
    }



    @Override
    public void onReceipt(DatagramPacket packet) throws IOException {
        switch (packet.getData()[0]){
            case INITIALISE_SENSOR:
                initialiseTopic(packet);
                break;
            case SUBSCRIBE:
                subscribe(packet, true);
                break;
            case MESSAGE:
                sendMessage(packet);
                break;
            case CREATE_SUBTOPIC:
                createSubtopic(packet);
                break;
            case UNSUBSCRIBE:
                subscribe(packet, false);
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
