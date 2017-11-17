import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.CRC32;
import java.util.Map;


public class GVPSocket
{
    private DatagramSocket socket;
    private InetAddress destIP;
    private int destPort;
    private int seqNum;
    private int ackNum;
    private int cwnd;

    public GVPSocket(String IP, int portNumber) throws Exception
    {
        seqNum = 200;
        ackNum = 0;
        cwnd = 10;
        socket = new DatagramSocket(0);
        destIP = InetAddress.getByName(IP);
        destPort = portNumber;
        handshake();
    }

    public GVPSocket(InetAddress IP, int portNumber) throws Exception
    {
        seqNum = 0;
        ackNum = 0;
        cwnd = 10;
        socket = new DatagramSocket(0);
        destIP = IP;
        destPort = portNumber;
    }

    private void handshake() throws Exception
    {
        GVPHeader syn = new GVPHeader(socket.getLocalPort(), destPort);
        syn.setSYN(true);
        sendPacket(syn.getArray());
        byte[] array = new byte[1024];
        readPacket(array);
        GVPHeader syn_ack = new GVPHeader(array);
 //       System.out.println(Arrays.toString(array));
        if (!(syn_ack.getSYN() && syn_ack.getACK())) System.out.println("Connection not established"); // EXCEPTION
        else {
            destPort = syn_ack.getSourcePortNumber();
            GVPHeader ack = new GVPHeader(socket.getLocalPort(), destPort);
            ack.setACK(true);
            sendPacket(ack.getArray());
        }
    }

    public int getLocalPort(){
        return socket.getLocalPort();
    }
//    void send(String pathToFile) throws Exception;
//    void read(String pathToFile) throws Exception;

    void send(byte[] array) throws Exception
    {
        GVPHeader packetHeader = new GVPHeader(socket.getLocalPort(),destPort);
        packetHeader.setSeqNumber(seqNum);
        sendPacket(concat(packetHeader.getArray(),array));
    }

    void sendPacket(byte[] array) throws Exception
    {
        DatagramPacket packet = new DatagramPacket(array, array.length, destIP, destPort);
        socket.send(packet);
    }

    void read(byte[] array) throws Exception // EXTRACTS DATA FROM PACKET
    {
        byte[] withHeader = new byte[1017];
        readPacket(withHeader);
        byte[] header = new byte[17];
        for (int i=0;i<17;i++) header[i] = array[i];
        for (int i=17;i<withHeader.length;i++) array[i-17] = withHeader[i];
//        System.out.println("read func output "+Arrays.toString(array));
    }

    void readPacket(byte[] array) throws Exception // READS PACKET FROM SOCKET
    {
        DatagramPacket receivePacket = new DatagramPacket(array, array.length);
        socket.receive(receivePacket);
    }

    void close() throws Exception
    {
        socket.close();
    }

    public byte[] concat(byte[] array1, byte[] array2) {
        int aLen = array1.length;
        int bLen = array2.length;
        byte[] result = new byte[aLen + bLen];

        System.arraycopy(array1, 0, result, 0, aLen);
        System.arraycopy(array2, 0, result, aLen, bLen);
        return result;
    }

}