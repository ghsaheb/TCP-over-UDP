// import com.sun.deploy.association.AssociationAlreadyRegisteredException;

import java.io.*;
import java.net.*;
import java.sql.Array;
import java.sql.Time;
import java.util.*;
import java.util.zip.CRC32;
import java.util.Map;
import java.util.zip.Checksum;


public class GVPSocket
{
    private DatagramSocket socket;
    private InetAddress destIP;
    private int destPort;
    private int seqNum;
    public static final int cwnd = 10;
    public static final int MSS = 1000 + GVPHeader.headerSize;
    private Timer timer;
    private ArrayList<byte[]> buffer;
    private ArrayList<byte[]> ACKbuffer;
    private ArrayList<Integer> buffer_size;
    private ReadThread thread;
    private int buffCounter;

    public GVPSocket(String IP, int portNumber) throws Exception
    {
        seqNum = 0;
        buffCounter = 0;
        socket = new DatagramSocket(0);
        destIP = InetAddress.getByName(IP);
        destPort = portNumber;
        buffer = new ArrayList<byte[]>();
        ACKbuffer = new ArrayList<byte[]>();
        thread = new ReadThread();
        handshake();
    }

    public GVPSocket(InetAddress IP, int portNumber) throws Exception
    {
        seqNum = 0;
        buffCounter = 0;
        socket = new DatagramSocket(0);
        destIP = IP;
        destPort = portNumber;
        buffer = new ArrayList<byte[]>();
        ACKbuffer = new ArrayList<byte[]>();
        thread = new ReadThread();
    }

    public void startReading(){
        thread.start();
    }
    private void handshake() throws Exception {
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
        startReading();
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
        TimeoutThread timeoutthread = new TimeoutThread(seqNum);
        timeoutthread.start();
//        while (readAck()!=seqNum);
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
        // int packetLength = readPacket(withHeader);
        System.out.println("buffer.size: "+ buffer.size()+ " buff counter : "+ buffCounter);
        while (buffCounter>=buffer.size()) Thread.sleep(1);//"size of buffer in while: "+buffer.size()+" buff counter: "+buffCounter);
        withHeader = buffer.get(buffCounter);
        buffCounter++;
        byte[] header = new byte[GVPHeader.headerSize];
        for (int i=0;i<GVPHeader.headerSize;i++) header[i] = array[i];
        for (int i=GVPHeader.headerSize;i<Math.min(array.length, withHeader.length);i++) array[i-GVPHeader.headerSize] = withHeader[i];
//        GVPHeader headerObject = new GVPHeader(withHeader);

//        Checksum checksum = new CRC32();
//        checksum.update(array, 0, packetLength - GVPHeader.headerSize);
        //long checksumValue = checksum.getValue();
//        if (errorDetection(headerObject.getChecksum(),checksumValue)) System.out.println("Error in packet");
//        else if (!(headerObject.getACK())){
//            sendAck(headerObject.getSeqNumber());
//        }
//        System.out.println("header checksum: " + headerObject.getChecksum() + "\ndata checksum: " + checksumValue);
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

    private class ReadThread extends Thread
    {
        @Override
        public void run(){
            while (true){
                byte[] array = new byte[MSS];
                DatagramPacket receivePacket = new DatagramPacket(array, array.length);
                try {
                    socket.receive(receivePacket);
                } catch (IOException e) {
                    System.out.println("jomjome shode ghomghome");
                }
                byte[] header = new byte[GVPHeader.headerSize];
                for (int i=0;i<GVPHeader.headerSize;i++) header[i] = array[i];
                GVPHeader  head = new GVPHeader(header);
                if (head.getACK()){
                    ACKbuffer.add(header);
                }
                else {
//                System.out.println("packet size: " + receivePacket.getLength());
//                System.out.println("Data:"+Arrays.toString(array));
//                System.out.println("buffer size:"+buffer.size());
                    Checksum checksum = new CRC32();
                    checksum.update(array, GVPHeader.headerSize, receivePacket.getLength() - GVPHeader.headerSize);
                    long checksumValue = checksum.getValue();
                    if (errorDetection(head.getChecksum(), checksumValue)){
                        System.out.println("Error in packet");
                        continue;
                    }
                    try {
                        sendAck(head.getSeqNumber());
                    } catch (Exception e) {}
                    buffer.add(array);
                }
            }
        }
    }

    private class TimeoutThread extends Thread{
        private int seqNum;
        public TimeoutThread(int _seqNum){
            super();
            seqNum = _seqNum;
        }
        @Override
        public void run(){
            while (true){
                boolean flag = false;
                for(int i=0;i<ACKbuffer.size();i++){
                    byte[] temp = ACKbuffer.get(i);
                    byte[] tempHeader = new byte[GVPHeader.headerSize];
                    for (int j=0;j<GVPHeader.headerSize;j++){
                        tempHeader[j]=temp[j];
                    }
                    GVPHeader header = new GVPHeader(tempHeader);
                    if (!(header.getACK())) continue;
                    if (header.getACKNumber() == seqNum){
                        ACKbuffer.remove(i);
                        flag = true;
                        break;
                    }
                }
                if (flag) break;
            }
            System.out.println("ACK No. " + seqNum + " received.");
        }
    }

}