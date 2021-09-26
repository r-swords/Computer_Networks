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

    DatagramSocket socket;
    Listener listener;
    CountDownLatch latch;

    Node() {
        latch= new CountDownLatch(1);
        listener= new Listener();
        listener.setDaemon(true);
        listener.start();
    }

    public DatagramPacket createPacket(int type, String message, InetSocketAddress dstAddress){
        byte[] data = new byte[PACKETSIZE];
        data[0] = (byte) type;
        ByteBuffer buffer = ByteBuffer.allocate(5);
        byte[] bufferArray = buffer.array();
        System.arraycopy(bufferArray, 0, data, 1, bufferArray.length);
        byte[] messageArray = message.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(messageArray, 0, data, 6, messageArray.length);
        return new DatagramPacket(data, data.length, dstAddress);
    }


    public abstract void onReceipt(DatagramPacket packet) throws IOException;

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