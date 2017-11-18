import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.CRC32;
import java.util.Map;
import java.util.zip.Checksum;


public class GVPSocket extends Thread
{
    private DatagramSocket socket;
    private InetAddress destIP;
    private int destPort;
    private int seqNum;
    public static final int cwnd = 10;
    public static final int MSS = 1000 + GVPHeader.headerSize;
    private Timer timer;

    public GVPSocket(String IP, int portNumber) throws Exception
    {
        seqNum = 0;
        socket = new DatagramSocket(0);
        destIP = InetAddress.getByName(IP);
        destPort = portNumber;
        handshake();
    }

    public GVPSocket(InetAddress IP, int portNumber) throws Exception
    {
        seqNum = 0;
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
        if (!(syn_ack.getSYN() && syn_ack.getACK())) System.out.println("Connection not established"); // EXCEPTION
        else {
            destPort = syn_ack.getSourcePortNumber();
            GVPHeader ack = new GVPHeader(socket.getLocalPort(), destPort);
            ack.setACK(true);
            sendPacket(ack.getArray());
        }
    }

    public void run(){

    }

    public int getLocalPort(){
        return socket.getLocalPort();
    }
//    void send(String pathToFile) throws Exception;
//    void read(String pathToFile) throws Exception;

    void send(byte[] array) throws Exception
    {
        if (array.length > MSS - GVPHeader.headerSize){
            // PACKETIZING
            System.out.println("big packet");
        }

        GVPHeader packetHeader = new GVPHeader(socket.getLocalPort(),destPort);
        packetHeader.setSeqNumber(seqNum);
        Checksum checksum = new CRC32();
        checksum.update(array,0,array.length);
        long checksumValue = checksum.getValue();
        packetHeader.setChecksum(checksumValue);
        sendPacket(concat(packetHeader.getArray(),array));
        Thread.sleep(1000);
        while (readAck()!=seqNum);
        seqNum = 1- seqNum;
    }

    void sendPacket(byte[] array) throws Exception
    {
        DatagramPacket packet = new DatagramPacket(array, array.length, destIP, destPort);
        socket.send(packet);
    }

    void read(byte[] array) throws Exception // EXTRACTS DATA FROM PACKET
    {
        byte[] withHeader = new byte[MSS];
        int packetLength = readPacket(withHeader);
        byte[] header = new byte[GVPHeader.headerSize];
        for (int i=0;i<GVPHeader.headerSize;i++) header[i] = array[i];
        for (int i=GVPHeader.headerSize;i<Math.min(array.length, withHeader.length);i++) array[i-GVPHeader.headerSize] = withHeader[i];
        GVPHeader headerObject = new GVPHeader(withHeader);

        Checksum checksum = new CRC32();
        checksum.update(array, 0, packetLength - GVPHeader.headerSize);
        long checksumValue = checksum.getValue();
        if (errorDetection(headerObject.getChecksum(),checksumValue)) System.out.println("Error in packet");
        else if (!(headerObject.getACK())){
            sendAck(headerObject.getSeqNumber());
        }
        System.out.println("header checksum: " + headerObject.getChecksum() + "\ndata checksum: " + checksumValue);
    }

    int readPacket(byte[] array) throws Exception // READS PACKET FROM SOCKET
    {
        DatagramPacket receivePacket = new DatagramPacket(array, array.length);
        socket.receive(receivePacket);
        System.out.println("packet size: " + receivePacket.getLength());
        return receivePacket.getLength();
    }

    void close() throws Exception {
        GVPHeader fin = new GVPHeader(socket.getLocalPort(), destPort);
        fin.setFIN(true);
        sendPacket(fin.getArray());
        socket.close();
    }

    public void sendAck(int seqNum) throws Exception{
        GVPHeader ack = new GVPHeader(socket.getLocalPort(),destPort);
        ack.setACK(true);
        ack.setACKNumber(seqNum);
        sendPacket(ack.getArray());
    }
    public int readAck() throws Exception{
        byte[] ack = new byte[GVPHeader.headerSize];
        readPacket(ack);
        GVPHeader ackHeader = new GVPHeader(ack);
        if(ackHeader.getACK()){
            return ackHeader.getACKNumber();
        }
        else System.out.println("not ack");
        return -1;
    }

    // to start or stop the timer
//    public void setTimer(boolean isNewTimer){
//        if (timer != null) timer.cancel();
//        if (isNewTimer){
//            timer = new Timer();
//            timer.schedule(new Timeout(), timeoutVal);
//        }
//    }

    private byte[] concat(byte[] array1, byte[] array2) {
        int aLen = array1.length;
        int bLen = array2.length;
        byte[] result = new byte[aLen + bLen];

        System.arraycopy(array1, 0, result, 0, aLen);
        System.arraycopy(array2, 0, result, aLen, bLen);
        return result;
    }

    private boolean errorDetection(long headerChecksum, long dataChecksum){
        return headerChecksum != dataChecksum;
    }

}