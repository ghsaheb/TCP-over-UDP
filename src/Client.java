import java.io.*;
import java.net.*;
import java.util.*;

public class Client
{
    public static void main(String args[]) throws Exception
    {
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
//        DatagramSocket clientSocket = new DatagramSocket();
        GVPSocket v = new GVPSocket("localhost", 9999);
//        InetAddress IPAddress = InetAddress.getByName("localhost");
        //byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];
        String sentence = inFromUser.readLine();
        byte[] sendData = sentence.getBytes();
//        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
//        clientSocket.send(sendPacket);
        v.send(sendData);
//        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//        clientSocket.receive(receivePacket);
        v.read(receiveData);
//        String modifiedSentence = new String(receivePacket.getData());
        String modifiedSentence = new String(receiveData, "UTF-8");

        System.out.println("FROM SERVER:" + modifiedSentence/* + " " + receiveData.length + " " + sendData.length*/);
        v.close();
    }
}