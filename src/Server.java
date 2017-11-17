import java.io.*;
import java.net.*;
import java.util.*;

public class Server
{
    public static void main(String args[]) throws Exception
    {
        GVPServerSocket serverSocket = new GVPServerSocket(9999);
        byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];
        while(true){
//            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//            serverSocket.receive(receivePacket);
            GVPSocket gh = serverSocket.accept();
            gh.read(receiveData);
            String sentence = new String(receiveData, "UTF-8");
//            String sentence = new String( receivePacket.getData());
//            String sentence = new String("jomjome shode ghomghome");
            System.out.println("RECEIVED: " + sentence);
//            InetAddress IPAddrcd Gess = receivePacket.getAddress();
//            int port = receivePacket.getPort();
            String capitalizedSentence = sentence.toUpperCase();
            sendData = capitalizedSentence.getBytes();
//            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            gh.send(sendData);
//            serverSocket.send(sendPacket);
            gh.close();
        }
    }
}