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
            (new Dashboard("broker", 50001, Integer.parseInt(args[0]))).start();
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


    public synchronized void sendSubscribeUnsubscribe(byte type, String topic) throws InterruptedException, IOException {
        String[] sub = topic.split(" ");
        if(sub.length == 2) {
            DatagramPacket newSubscription = createPacket(type, "", dstAddress, sub[0], sub[1], "");
            socket.send(newSubscription);
            this.wait();
        }
        else if(sub.length == 1) {
            DatagramPacket newSubscription = createPacket(type, "", dstAddress, sub[0], "", "");
            socket.send(newSubscription);
            this.wait();
        }
        else terminal.println("Invalid input.");
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
                sendSubscribeUnsubscribe(SUBSCRIBE, topic);
            }
            else if(action.equalsIgnoreCase("UNSUBSCRIBE")){
                String topic =  terminal.read("Unsubscribe to Topic, " +
                        "unsubscription requests should be in the form of '<Topic> <Sub-Topic>': ");
                terminal.println("Unsubscribe to topic, " +
                        "unsubscription requests should be in the form of '<Topic> <Sub-Topic>': " + topic);
                sendSubscribeUnsubscribe(UNSUBSCRIBE, topic);
            }
            else terminal.println("Invalid input.");
        }
    }

    @Override
    public synchronized void onReceipt(DatagramPacket packet) {
        notifyAll();
        if(packet.getData()[0] == AUTH){
            terminal.println(getMessage(packet).toUpperCase());
        }
        else {
            String message = getMessage(packet);
            String printMessage = "Message from " + getTopic(packet);
            if(!getSubtopic(packet).equals("")) printMessage += " " + getSubtopic(packet);
            else if(!getGroup(packet).equals("")) printMessage += " " + getGroup(packet);
            printMessage += ": " + message;
            terminal.println(printMessage);
        }
    }
}