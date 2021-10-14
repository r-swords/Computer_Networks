import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Broker extends Node {
    Terminal terminal;
    HashMap<String, Topic> topicSubscriptions;
    InetSocketAddress dstAddress;

    Broker (String dstHost, int dstPort, int srcPort){
        topicSubscriptions = new HashMap<>();
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
        String topicName = getTopic(packet);
        Topic topic = topicSubscriptions.get(topicName);
        if(!getGroup(packet).equals("")){
            ArrayList<String> subtopicList = topic.getGroupList(getGroup(packet));
            if (subtopicList != null) {
                HashSet<InetSocketAddress> sentAddresses = new HashSet<>();
                for (String i : subtopicList) {
                    ArrayList<InetSocketAddress> list = topic.getSubscriberList(i);
                    if (list != null) {
                        for (InetSocketAddress j : list) {
                            if (!sentAddresses.contains(j)) {
                                DatagramPacket sendPacket = createPacket(MESSAGE, message, j, topicName, i,
                                        getGroup(packet));
                                socket.send(sendPacket);
                                sentAddresses.add(j);
                            }
                        }
                    }
                }
            }
        }
        else {
            ArrayList<InetSocketAddress> list = (!getSubtopic(packet).equals("")) ?
                    topic.getSubscriberList(getSubtopic(packet)) : topic.getAll();
            if(list != null) {
                for (InetSocketAddress i : list) {
                    DatagramPacket sendPacket = createPacket(MESSAGE, message, i, topicName,
                            getSubtopic(packet), "");
                    socket.send(sendPacket);
                }
            }
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

    public synchronized void initialiseTopic(DatagramPacket packet) throws IOException {
        String sensorName = getTopic(packet);
        String auth = "action successful";
        if(getAuthorisation("Initialise publisher request from " + sensorName +" (y/n): ")) {
            if (!topicSubscriptions.containsKey(sensorName)) {
                Topic newTopic = new Topic();
                topicSubscriptions.put(sensorName, newTopic);
            } else auth = "action rejected";
        }
        else auth = "action rejected";
        DatagramPacket newPacket = createPacket(AUTH, auth, (InetSocketAddress) packet.getSocketAddress(),
                getTopic(packet), getSubtopic(packet),"");
        socket.send(newPacket);
    }

    public synchronized  void subscribe(DatagramPacket packet, boolean isSubscription) throws IOException {
        String auth = "action authorised";
        if(getAuthorisation("Subscription request to " + getTopic(packet)+ " "
                + getSubtopic(packet) + " (y/n): ") && topicSubscriptions.containsKey(getTopic(packet))) {
           Topic topic = topicSubscriptions.get(getTopic(packet));
           String subtopic = getSubtopic(packet);
           if(!subtopic.equals("")){
               if(isSubscription) {
                   if(!topic.addSubscriber(subtopic, (InetSocketAddress) packet.getSocketAddress())) {
                       auth = "action rejected";
                   }
               }
               else topic.removeSubscriber(subtopic, (InetSocketAddress) packet.getSocketAddress());
           }
           else{
               if(isSubscription) topic.addSubscriber((InetSocketAddress) packet.getSocketAddress());
               else topic.removeSubscriber((InetSocketAddress) packet.getSocketAddress());
           }
           topicSubscriptions.put(getTopic(packet), topic);
        }
        else auth = "action rejected";
        DatagramPacket newPacket = createPacket(AUTH, auth, (InetSocketAddress) packet.getSocketAddress(),
                "", "","");
        socket.send(newPacket);
    }

    public synchronized void createSubtopic(DatagramPacket packet) throws IOException {
        String auth = "action accepted";
        if(getAuthorisation("Request to create subtopic " + getSubtopic(packet) + " for " +
                getTopic(packet) + " (y/n): ")) {
            String topicName = getTopic(packet);
            Topic topic = topicSubscriptions.get(topicName);
            topic.addTopic(getSubtopic(packet));
        }
        else auth = "action rejected";
        DatagramPacket newPacket = createPacket(AUTH, auth, (InetSocketAddress) packet.getSocketAddress(),
                "", "","");
        socket.send(newPacket);
    }

    public synchronized void  addSubtopicToGroup(DatagramPacket packet) throws IOException {
        String auth = "action accepted";
        if(getAuthorisation("Request to add " + getSubtopic(packet) + " to " + getGroup(packet)
                + " in " + getTopic(packet) + " (y/n): ")){
            Topic topic = topicSubscriptions.get(getTopic(packet));
            if(!topic.addSubtopicToGroup(getSubtopic(packet), getGroup(packet))) auth = "action rejected";
        }
        else auth = "action rejected";
        DatagramPacket newPacket = createPacket(AUTH, auth, (InetSocketAddress) packet.getSocketAddress(),
                "", "", "");
        socket.send(newPacket);
    }

    public synchronized void createGroup(DatagramPacket packet) throws IOException {
        String auth = "action accepted";
        if(getAuthorisation("Request to create group '" + getGroup(packet) + "' in " +
                getTopic(packet) + " (y/n): ")){
            Topic topic = topicSubscriptions.get(getTopic(packet));
            topic.addGroup(getGroup(packet));
        }
        else auth = "action rejected";
        DatagramPacket newPacket = createPacket(AUTH, auth, (InetSocketAddress) packet.getSocketAddress(),
                "", "", "");
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
                break;
            case CREATE_GROUP:
                createGroup(packet);
                break;
            case ADD_SUBTOPIC_TO_GROUP:
                addSubtopicToGroup(packet);
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