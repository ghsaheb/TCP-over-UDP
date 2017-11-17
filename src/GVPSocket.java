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

    public GVPSocket(String IP, int portNumber) throws Exception
    {
        socket = new DatagramSocket(0);
        destIP = InetAddress.getByName(IP);
        destPort = portNumber;
        handshake();
    }

    public GVPSocket(InetAddress IP, int portNumber) throws Exception
    {
        socket = new DatagramSocket(0);
        destIP = IP;
        destPort = portNumber;
    }

    private void handshake() throws Exception
    {
        GVPHeader syn = new GVPHeader(socket.getLocalPort(), destPort);
        syn.setSYN(true);
        send(syn.getArray());
        byte[] array = new byte[17];
        read(array);
        GVPHeader syn_ack = new GVPHeader(array);
        System.out.println(Arrays.toString(array));
        if (!(syn_ack.getSYN() && syn_ack.getACK())) System.out.println("Connection not established"); // EXCEPTION
        else {
            destPort = syn_ack.getSourcePortNumber();
            GVPHeader ack = new GVPHeader(socket.getLocalPort(), destPort);
            ack.setACK(true);
            send(ack.getArray());
        }
    }

    public int getLocalPort(){
        return socket.getLocalPort();
    }
//    void send(String pathToFile) throws Exception;
//    void read(String pathToFile) throws Exception;

    void send(byte[] array) throws Exception
    {
        DatagramPacket sendPacket = new DatagramPacket(array, array.length, destIP, destPort);
        socket.send(sendPacket);
    }

    void read(byte[] array) throws Exception
    {
        DatagramPacket receivePacket = new DatagramPacket(array, array.length);
        socket.receive(receivePacket);
    }

    void close() throws Exception
    {
        socket.close();
    }

//    Map<String,String> getHeaders() throws Exception;

}