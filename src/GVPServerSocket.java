import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.CRC32;


public class GVPServerSocket
{

    private DatagramSocket serverSocket;
    
    public GVPServerSocket(int portNumber) throws Exception
    {
        serverSocket = new DatagramSocket(portNumber);
    }

    public GVPSocket accept() throws Exception
    {
        byte[] array_syn = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(array_syn, array_syn.length);
        serverSocket.receive(receivePacket);
        GVPHeader syn = new GVPHeader(array_syn);
        if (!(syn.getSYN())) System.out.println("Error in syn"); // EXCEPTION
        InetAddress IPAddress = receivePacket.getAddress();
        int port = receivePacket.getPort();
        GVPSocket newSocket = new GVPSocket(IPAddress, port);
        GVPHeader syn_ack = new GVPHeader(newSocket.getLocalPort(), port);
        syn_ack.setSYN(true);
        syn_ack.setACK(true);
        newSocket.sendPacket(syn_ack.getArray());
        byte[] array_ack = new byte[1024];
        newSocket.readPacket(array_ack);
        GVPHeader ack = new GVPHeader(array_ack);
        if (!(ack.getACK())) System.out.println("Connection refused"); // EXCEPTION
        else {
            System.out.println("Handshake done - connection established");
        }
        //end of hand shaking
        newSocket.startReading(); //start thread
        return newSocket;
    }
}