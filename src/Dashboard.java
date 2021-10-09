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

    public void start() throws InterruptedException, IOException {
        while(true) {
            String action = terminal.read("Enter 'SUBSCRIBE' or 'UNSUBSCRIBE': ");
            terminal.println("Enter 'SUBSCRIBE' or 'UNSUBSCRIBE': " + action);
            if(action.equalsIgnoreCase("SUBSCRIBE")) {
                String topic = terminal.read("Subscribe to topic, " +
                        "subscription requests should be in the form of '<Topic> <Sub-Topic>': ");
                terminal.println("Subscribe to topic, subscription requests should be in the form of " +
                        "'<Topic> <Sub-Topic>' : " + topic);
                DatagramPacket newSubscription = createPacket(SUBSCRIBE, topic, dstAddress, -1);
                socket.send(newSubscription);
            }
            else if(action.equalsIgnoreCase("UNSUBSCRIBE")){
                String topic =  terminal.read("Unsubscribe to Topic, " +
                        "unsubscription requests should be in the form of '<Topic> <Sub-Topic>': ");
                terminal.println("Unsubscribe to topic, " +
                        "unsubscription requests should be in the form of '<Topic> <Sub-Topic>': " + topic);
                DatagramPacket unsubscribe = createPacket(UNSUBSCRIBE, topic, dstAddress, -1);
                socket.send(unsubscribe);
            }
            else terminal.println("Invalid input.");
        }
    }

    @Override
    public synchronized void onReceipt(DatagramPacket packet) {
        byte[] message = packet.getData();
        String printMessage = new String(message).trim();
        if(!printMessage.equals("fALSE")) {
            terminal.println(printMessage);
        }
        else terminal.println("Topic doesn't exist");
    }
}
