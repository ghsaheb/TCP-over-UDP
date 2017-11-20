import java.io.*;
import java.net.*;
import java.util.*;

public class Server
{
    public static void main(String args[]) throws Exception
    {
        GVPServerSocket serverSocket = new GVPServerSocket(9999);
        byte[] receiveData = new byte[900];
        byte[] sendData = new byte[1024];
        GVPSocket gh = serverSocket.accept();
        while(true){
            gh.read(receiveData); //imp
            String sentence = new String(receiveData, "UTF-8");
            System.out.println("RECEIVED: " + sentence);
            String capitalizedSentence = sentence.toUpperCase();
            sendData = capitalizedSentence.getBytes();
            gh.send(sendData); //imp
  //          gh.close();
        }
    }
}