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
    private int receiveNum;
    public static final int MSS = 1024 + GVPHeader.headerSize;
    private ArrayList<byte[]> buffer;
    private ArrayList<Integer> ACKbuffer;
    private ReadThread thread;
    private int buffCounter;
    private byte[] sendBuffer;

    public GVPSocket(String IP, int portNumber) throws Exception
    {
        receiveNum = 0;
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

    public GVPSocket(InetAddress IP, int portNumber) throws Exception
    {
        receiveNum = 0;
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
        if (!(syn_ack.getSYN() && syn_ack.getACK())){
            throw new GVPHandshakingException("Bad message received. Excpecting SYN-ACK");
        }
        destPort = syn_ack.getSourcePortNumber();
        GVPHeader ack = new GVPHeader(socket.getLocalPort(), destPort);
        ack.setACK(true);
        sendPacket(ack.getArray());
        startReading();
    }

    public int getLocalPort(){
        return socket.getLocalPort();
    }

    void send(String pathToFile) throws Exception {
        File file = new File(pathToFile);
        byte[] bytesArray = new byte[(int) file.length()];
        FileInputStream fis = new FileInputStream(file);
        fis.read(bytesArray);
        fis.close();
        System.out.println(bytesArray.length);
        send(bytesArray);
    }

    void read(String pathToFile) throws Exception
    {
        File file = new File(pathToFile); // ?
        Thread.sleep(10000);
        file.createNewFile();
        System.out.println("Start of writing to file");
        FileOutputStream fos = new FileOutputStream(file);
        while (!buffer.isEmpty()){
            byte[] temp = buffer.get(0);
            byte[] toWrite = new byte[temp.length - GVPHeader.headerSize];
            for (int i=0;i<toWrite.length;i++) toWrite[i] = temp[i+GVPHeader.headerSize];
            fos.write(toWrite);
            buffer.remove(0);   
        }
        System.out.println("End of writing to file");
        fos.close();
    }

    void send(byte[] array) throws Exception
    {
        if (array.length > MSS - GVPHeader.headerSize){
            // PACKETIZING
            byte[] temp = new byte[MSS - GVPHeader.headerSize];
            for (int i = 0;i<array.length;i++){
                temp[i%(MSS - GVPHeader.headerSize)] = array[i];
                if ((i+1)%(MSS - GVPHeader.headerSize) == 0 ){
                    System.out.println("Yasari: " + i );
                    send(temp);
                }
                else if (i == array.length-1){
                    System.out.println("GHADERI");
                    byte[] temp2 = new byte[array.length - ((MSS - GVPHeader.headerSize)*(array.length/(MSS - GVPHeader.headerSize)))];
                    for (int j = ((MSS - GVPHeader.headerSize)*(array.length/(MSS - GVPHeader.headerSize)));j<array.length;j++) temp2[j%(MSS - GVPHeader.headerSize)] = array[j];
                    send(temp2);
                    break;
                }
            }
            System.out.println("big packet");
            return;
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
        System.out.println("SHAMAEIZADEH: " + buffCounter);
        System.arraycopy(withHeader, GVPHeader.headerSize, array, 0, Math.min(array.length, withHeader.length) - GVPHeader.headerSize);
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
                    break;
                }
                byte[] header = new byte[GVPHeader.headerSize];
                for (int i=0;i<GVPHeader.headerSize;i++) header[i] = array[i];
                GVPHeader head = new GVPHeader(header);
                if (head.getACK()){
                    System.out.println("packet is ack with number: "+head.getACKNumber());
                    ACKbuffer.add(head.getACKNumber());
                    System.out.println("ack packet added to ackbuffer and size is now:"+ACKbuffer.size());
                }
                else if (head.getFIN()){
                    System.out.println("Connection closed");
                    socket.close();
                    break;
                }
                else {
                    System.out.println("Packet has data and is not ack "+"packet number is:"+head.getSeqNumber());
                    System.arraycopy(header, GVPHeader.headerSize, array, 0, Math.min(array.length, header.length) - GVPHeader.headerSize);
                    Checksum checksum = new CRC32();
                    checksum.update(array, GVPHeader.headerSize, receivePacket.getLength() - GVPHeader.headerSize);
                    long checksumValue = checksum.getValue();
                    if (errorDetection(head.getChecksum(), checksumValue)){
                        System.out.println("Error in packet checksum");
                        continue;
                    }
                    if (head.getSeqNumber()==receiveNum){
                        receiveNum = 1-receiveNum;
                    }
                    else continue;
                    try {
//                        Thread.sleep(1000);
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
            timer.schedule(new Timeout(), new Date(),1000);
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