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
    private ArrayList<Integer> ACKbuffer;
    private ReadThread thread;
    private int buffCounter;
    private byte[] sendBuffer; // BADAN BUFFER BEZANIM KE WINDOW DASHTE BASHIM

    public GVPSocket(String IP, int portNumber) throws Exception
    {
        seqNum = 0;
        buffCounter = 0;
        socket = new DatagramSocket(0);
        destIP = InetAddress.getByName(IP);
        destPort = portNumber;
        buffer = new ArrayList<byte[]>();
        ACKbuffer = new ArrayList<Integer>();
        thread = new ReadThread();
        handshake();
    }

    public GVPSocket(InetAddress IP, int portNumber) throws Exception //socket ie k server misaze handshaking nadare
    {
        seqNum = 0;
        buffCounter = 0;
        socket = new DatagramSocket(0);
        destIP = IP;
        destPort = portNumber;
        buffer = new ArrayList<byte[]>();
        ACKbuffer = new ArrayList<Integer>();
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
        //make header for packet
        GVPHeader packetHeader = new GVPHeader(socket.getLocalPort(),destPort);
        packetHeader.setSeqNumber(seqNum);
        Checksum checksum = new CRC32();
        checksum.update(array,0,array.length);
        long checksumValue = checksum.getValue();
        packetHeader.setChecksum(checksumValue);
        //send packet
        sendBuffer = concat(packetHeader.getArray(),array);
        sendPacket(sendBuffer);
        //start timer
        TimeoutThread timeoutthread = new TimeoutThread(seqNum);
        timeoutthread.start();
        System.out.println("packet with number "+ seqNum + "sent");
        seqNum = 1- seqNum;
    }

    void sendPacket(byte[] array) throws Exception
    {
        DatagramPacket packet = new DatagramPacket(array, array.length, destIP, destPort);
        socket.send(packet);
    }

    void read(byte[] array) throws Exception
    {
        byte[] withHeader = new byte[MSS];
        System.out.println("buffer.size: "+ buffer.size()+ " buff counter : "+ buffCounter);
        while (buffCounter>=buffer.size()) Thread.sleep(1);
        withHeader = buffer.get(buffCounter);
        buffCounter++;
        System.arraycopy(withHeader, GVPHeader.headerSize, array, 0, Math.min(array.length, withHeader.length) - 25);
    }

    void readPacket(byte[] array) throws Exception // READS PACKET FROM SOCKET
    {
        DatagramPacket receivePacket = new DatagramPacket(array, array.length);
        socket.receive(receivePacket);
        System.out.println("packet size: " + receivePacket.getLength());
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
                System.out.println("reading new packet");
                byte[] array = new byte[MSS];
                DatagramPacket receivePacket = new DatagramPacket(array, array.length);
                try {
                    socket.receive(receivePacket);
                    System.out.println("packet received");
                } catch (IOException e) {
                    System.out.println("read thread is not receiving any packets");
                }
                byte[] header = new byte[GVPHeader.headerSize];
                for (int i=0;i<GVPHeader.headerSize;i++) header[i] = array[i];
                GVPHeader head = new GVPHeader(header);
                if (head.getACK()){
                    System.out.println("packet is ack with number: "+head.getACKNumber());
                    ACKbuffer.add(head.getACKNumber());
                    System.out.println("ack packet added to ackbuffer and size is now:"+ACKbuffer.size());
                }
                else {
                    System.out.println("Packet has data and is not ack "+"packet number is:"+head.getSeqNumber());
                    System.arraycopy(header, GVPHeader.headerSize, array, 0, Math.min(array.length, header.length) - 25);
                    Checksum checksum = new CRC32();
                    checksum.update(array, GVPHeader.headerSize, receivePacket.getLength() - GVPHeader.headerSize);
                    long checksumValue = checksum.getValue();
                    if (errorDetection(head.getChecksum(), checksumValue)){
                        System.out.println("Error in packet checksum");
                        continue;
                    }
                    try {
                        Thread.sleep(1000);
                        sendAck(head.getSeqNumber());
                        System.out.println("ACK sent and ackNum is:"+ head.getSeqNumber());
                    } catch (Exception e) {}
                    buffer.add(array);
                }
            }
        }
    }

    private void resend() throws Exception
    {
        sendPacket(sendBuffer);
    }

    private class TimeoutThread extends Thread{
        private int seqNum;
        Timer timer;

        public TimeoutThread(int _seqNum){
            super();
            seqNum = _seqNum;
            timer = new Timer();
            timer.schedule(new Timeout(), new Date(),10000);
            System.out.println("received seq number is: "+seqNum);
        }

        private class Timeout extends TimerTask
        {
            public void run(){
                try {
                    System.out.println("time limit exceeded");
                    boolean flag = false;
                    System.out.println("ack buffer size is: "+ACKbuffer.size());
                    for(int i=0;i<ACKbuffer.size();i++){
                        int ackNumber = ACKbuffer.get(i);
                        System.out.println("searching for ack "+ seqNum +"and ack buffer size: "+ACKbuffer.size());
                        if (ackNumber == seqNum){
                            ACKbuffer.remove(i);
                            System.out.println("ack number"+i+"removed");
                            System.out.println("ack found in ackbuffer");
                            timer.cancel();
                            System.out.println("timer canceled");
                            flag = true;
                            break;
                        }
                    }
                    if (!flag){
                        System.out.println("ack not found => resending" + seqNum);
                        resend();
                    }
                } catch (Exception e) {}
            }
        }
    }

}