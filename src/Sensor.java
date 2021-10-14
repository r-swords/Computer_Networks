import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashSet;


public class Sensor extends Node {

    boolean add;
    Terminal terminal;
    InetSocketAddress dstAddress;
    String sensorName;
    HashSet<String> subtopics;
    HashSet<String> groups;


    Sensor(String dstHost, int dstPort, int srcPort){
        try {
            terminal = new Terminal("Sensor");
            dstAddress= new InetSocketAddress(dstHost, dstPort);
            socket= new DatagramSocket(srcPort);
            subtopics = new HashSet<>();
            groups = new HashSet<>();
            add = false;
            listener.go();
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    public synchronized void publishMessage() throws IOException {
        String message = terminal.read("Enter Message: ");
        String selection = terminal.read("Enter 'SUBTOPIC' to select a sub-topic, 'GROUP' to " +
                "select a group\n, or 'ALL' to publish to all sub-topics: ");
        terminal.println("Enter SUBTOPIC' to select a sub-topic, 'GROUP' to select a group, or 'ALL' " +
                "to publish to all sub-topics: " + selection);
        terminal.println("Enter message: " + message);
        if(selection.equalsIgnoreCase("ALL")){
            DatagramPacket datagramPacket = createPacket(MESSAGE, message, dstAddress, sensorName,
                    "", "");
            socket.send(datagramPacket);
        }
        else if(selection.equalsIgnoreCase("SUBTOPIC")) {
            String subtopic = terminal.read("Enter sub-topic name: ");
            terminal.println("Enter sub-topic name: " + subtopic);
            if(subtopics.contains(subtopic)) {
                DatagramPacket datagramPacket = createPacket(MESSAGE, message, dstAddress, sensorName,
                        subtopic, "");
                socket.send(datagramPacket);
            }
            else terminal.println("Sub-topic does not exist.");
        }
        else if(selection.equalsIgnoreCase("GROUP")){
            String group = terminal.read("Enter group name: ");
            terminal.println("Enter group name: " + group);
            if(groups.contains(group)) {
                DatagramPacket datagramPacket = createPacket(MESSAGE, message, dstAddress, sensorName,
                        "", group);
                socket.send(datagramPacket);
            }
            else terminal.println("Group does not exist.");
        }
        else terminal.println("Invalid input");
    }

    public synchronized void create() throws IOException, InterruptedException {
        add = false;
        String action = terminal.read("Enter 'GROUP' to create a group, or 'SUBTOPIC' to create a " +
                "sub-topic: ");
        terminal.println("Enter 'GROUP' to create a group, or 'SUBTOPIC' to create a sub-topic: " + action);
        if(action.equalsIgnoreCase("SUBTOPIC")) {
            String subTopicName = terminal.read("Enter the name of the new sub-topic: ");
            terminal.println("Enter the name of the new sub-topic: " + subTopicName);
            if(!subtopics.contains(subTopicName)) {
                DatagramPacket subTopicPacket = createPacket(CREATE_SUBTOPIC, "", dstAddress,
                        sensorName, subTopicName, "");
                socket.send(subTopicPacket);
                this.wait();
                if(add) subtopics.add(subTopicName);
            }
            else terminal.println("This sub-topic already exists.");
        }
        else if(action.equalsIgnoreCase("GROUP")){
            String groupName = terminal.read("Enter the name of the new group: ");
            terminal.println("Enter the name of the new group: " + groupName);
            if(!groups.contains(groupName)) {
                DatagramPacket groupPacket = createPacket(CREATE_GROUP, "", dstAddress,
                        sensorName, "", groupName);
                socket.send(groupPacket);
                this.wait();
                if(add) groups.add(groupName);
            }
            else terminal.println("This group already exists");
            add = false;
        }
        else terminal.println("Invalid input.");
    }

    public synchronized void initialiseSensor() throws IOException, InterruptedException {
        sensorName = terminal.read("Enter sensor name: ");
        DatagramPacket initialisePacket = createPacket(INITIALISE_SENSOR, "", dstAddress,
                sensorName, "", "");
        socket.send(initialisePacket);
        this.wait();
    }

    public synchronized void addSubtopicToGroup() throws IOException, InterruptedException {
        String group = terminal.read("Enter group: ");
        terminal.println("Enter group: " + group);
        String subtopic = terminal.read("Enter sub-topic: ");
        terminal.println("Enter sub-topic: " + subtopic);
        if(groups.contains(group) && subtopics.contains(subtopic)){
            DatagramPacket packet = createPacket(ADD_SUBTOPIC_TO_GROUP, "", dstAddress, sensorName,
                    subtopic, group);
            socket.send(packet);
            this.wait();
        }
        else terminal.println("Invalid input");
    }

    public synchronized void runner() throws IOException, InterruptedException {
        while(true) {
            String action = terminal.read("Enter 'CREATE' to create a sub-topic or group, " +
                    "'ADD' to add a sub-topic to a group\n, or 'PUBLISH' to publish a message: ");
            terminal.println("Enter 'CREATE' to create a sub-topic or group, " +
            "'ADD' to add a sub-topic to a group, or 'PUBLISH' to publish a message: " + action);
            if (action.equalsIgnoreCase("CREATE")) {
                create();
            } else if (action.equalsIgnoreCase("PUBLISH")) {
                publishMessage();
            }
            else if(action.equalsIgnoreCase("ADD")){
                addSubtopicToGroup();
            }
            else terminal.println("Invalid input.");
        }
    }

    public synchronized void start() throws IOException, InterruptedException {
        initialiseSensor();
        runner();
    }

    @Override
    public synchronized void onReceipt(DatagramPacket packet) {
        byte[] message = packet.getData();
        if(message[0] == AUTH) {
            terminal.println(getMessage(packet).toUpperCase());
            if(getMessage(packet).equalsIgnoreCase("action accepted")) add = true;
        }
        else {
            String printMessage = new String(message).trim();
            if (printMessage.equals("true")) {
                terminal.println("Sensor added.");
            } else terminal.println("Sensor name already exists.");
        }
        notifyAll();
    }

    public static void main(String[] args){
        try{
            (new Sensor("localhost", 50001, 50002)).start();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}