import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public abstract class Node {
    static final int PACKETSIZE = 1000;

    static final byte INITIALISE_SENSOR = 1;
    static final byte MESSAGE = 2;
    static final byte SUBSCRIBE = 3;
    static final byte CREATE_SUBTOPIC = 4;
    static final byte UNSUBSCRIBE = 5;
    static final byte AUTH = 6;

    DatagramSocket socket;
    Listener listener;
    CountDownLatch latch;

    Node() {
        latch= new CountDownLatch(1);
        listener= new Listener();
        listener.setDaemon(true);
        listener.start();
    }

    public String getMessage(DatagramPacket packet){
        byte[] messageArray = new byte[packet.getData().length];
        System.arraycopy(packet.getData(), 26, messageArray, 0,
                packet.getData().length-26);
        return new String(messageArray).trim();
    }

    public String getTopic(DatagramPacket packet){
        byte[] topicArray = new byte[10];
        System.arraycopy(packet.getData(), 1, topicArray, 0, topicArray.length);
        return new String(topicArray).trim();
    }

    public String getSubtopic(DatagramPacket packet) {
        byte[] subtopic = new byte[10];
        System.arraycopy(packet.getData(), 11, subtopic,0, subtopic.length);
        return new String(subtopic).trim();
    }

    public DatagramPacket createPacket(byte type, String message, InetSocketAddress dstAddress, String topic, String subtopic){
        if(subtopic.length() <= 10 && topic.length() <= 10) {
            byte[] data = new byte[PACKETSIZE];
            data[0] = type;
            byte[] topicBytes = topic.getBytes();
            System.arraycopy(topicBytes,0, data, 1, topicBytes.length);
            byte[] subTopicBytes = subtopic.getBytes();
            System.arraycopy(subTopicBytes,0, data, 11, subTopicBytes.length);
            ByteBuffer buffer = ByteBuffer.allocate(5);
            byte[] bufferArray = buffer.array();
            System.arraycopy(bufferArray, 0, data, 21, bufferArray.length);
            byte[] messageArray = message.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(messageArray, 0, data, 26, messageArray.length);
            return new DatagramPacket(data, data.length, dstAddress);
        }
        return null;
    }


    public abstract void onReceipt(DatagramPacket packet) throws IOException, InterruptedException;

    /**
     *
     * Listener thread
     *
     * Listens for incoming packets on a datagram socket and informs registered receivers about incoming packets.
     */
    class Listener extends Thread {

        /*
         *  Telling the listener that the socket has been initialized
         */
        public void go() {
            latch.countDown();
        }

        /*
         * Listen for incoming packets and inform receivers
         */
        public void run() {
            try {
                latch.await();
                // Endless loop: attempt to receive packet, notify receivers, etc
                while(true) {
                    DatagramPacket packet = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE);
                    socket.receive(packet);

                    onReceipt(packet);
                }
            } catch (Exception e) {if (!(e instanceof SocketException)) e.printStackTrace();}
        }
    }
}