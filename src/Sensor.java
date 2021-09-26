import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;



public class Sensor extends Node {

    Terminal terminal;
    InetSocketAddress dstAddress;
    String sensorName;

    Sensor(String dstHost, int dstPort, int srcPort){
        try {
            terminal = new Terminal("Sensor");
            dstAddress= new InetSocketAddress(dstHost, dstPort);
            socket= new DatagramSocket(srcPort);
            listener.go();
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    public synchronized void publishMessage(String message) throws IOException {
        DatagramPacket datagramPacket = createPacket(MESSAGE, message, dstAddress);
        socket.send(datagramPacket);
    }

    public synchronized void start() throws IOException, InterruptedException {
        sensorName = terminal.read("Enter sensor name: ");
        DatagramPacket initialisePacket = createPacket(INITIALISE_SENSOR, sensorName, dstAddress);
        socket.send(initialisePacket);
        while(true){
            String message = terminal.read("Enter message: ");
            terminal.println("Enter message: " + message);
            publishMessage(message);
        }
    }

    @Override
    public synchronized void onReceipt(DatagramPacket packet) {
        terminal.println("recieved");
    }

    public static void main(String[] args){
        try{
            (new Sensor("localhost", 50001, 50002)).start();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
