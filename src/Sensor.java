import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashMap;


public class Sensor extends Node {

    Terminal terminal;
    InetSocketAddress dstAddress;
    String sensorName;
    int subTopicNumber;
    HashMap<String, Integer> subTopicMap;

    Sensor(String dstHost, int dstPort, int srcPort){
        try {
            terminal = new Terminal("Sensor");
            dstAddress= new InetSocketAddress(dstHost, dstPort);
            socket= new DatagramSocket(srcPort);
            listener.go();
            subTopicNumber = 1;
            subTopicMap = new HashMap<>();
            subTopicMap.put("ALL", 0);
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    public synchronized void publishMessage() throws IOException {
        String subTopic = terminal.read("Enter sub-topic name, or 'ALL' to " +
                "publish to all sub-topics: ");
        terminal.println("Enter sub-topic name, or 'ALL' to publish to all sub-topics: " + subTopic);
        int subNumber;
        if(subTopicMap.containsKey(subTopic)) {
            subNumber = subTopicMap.get(subTopic);
            String message = terminal.read("Enter Message: ");
            terminal.println("Enter message: " + message);
            DatagramPacket datagramPacket = createPacket(MESSAGE, message, dstAddress, subNumber);
            socket.send(datagramPacket);
        }
        else terminal.println("sub-topic does not exist");
    }

    public synchronized void initialiseSensor() throws IOException {
        sensorName = terminal.read("Enter sensor name: ");
        DatagramPacket initialisePacket = createPacket(INITIALISE_SENSOR, sensorName, dstAddress, -1);
        socket.send(initialisePacket);
    }

    public void runner() throws IOException, InterruptedException {
        while(true) {
            String action = terminal.read("Enter 'CREATE' to create a sub-topic, " +
                    "or 'PUBLISH' to publish a message: ");
            terminal.println("Enter 'CREATE' to create a sub-topic, " +
                    "or 'PUBLISH' to publish a message: " + action);
            if (action.equalsIgnoreCase("CREATE")) {
                String subTopicName = terminal.read("Enter the name of the new sub-topic: ");
                terminal.println("Enter the name of the new sub-topic: " + subTopicName);
                subTopicMap.put(subTopicName, subTopicNumber);
                DatagramPacket subTopicPacket = createPacket(CREATE_SUBTOPIC, subTopicName, dstAddress, subTopicNumber);
                socket.send(subTopicPacket);
                subTopicNumber++;
            } else if (action.equalsIgnoreCase("PUBLISH")) {
                publishMessage();
            }
        }
    }

    public synchronized void start() throws IOException, InterruptedException {
        initialiseSensor();
        runner();
    }


    @Override
    public synchronized void onReceipt(DatagramPacket packet) throws IOException {
        byte[] message = packet.getData();
        String printMessage = new String(message).trim();
        if(printMessage.equals("true")){
            terminal.println("Sensor added.");
            publishMessage();
        }
        else terminal.println("Sensor name already exists.");
    }

    public static void main(String[] args){
        try{
            (new Sensor("localhost", 50001, 50002)).start();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
