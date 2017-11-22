import java.io.*;
import java.net.*;
import java.util.*;

public class Client
{
    public static void main(String args[]) throws Exception
    {
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        GVPSocket v = new GVPSocket("localhost", 9999);

   //     while (true) {
    //        byte[] receiveData = new byte[1024];
    //        String sentence = inFromUser.readLine();
    //        byte[] sendData = sentence.getBytes();
            v.send("./a.mp3"); //imp
    //        v.send(sendData);
        v.send("./b.mp3");
            System.out.println("Sending Done");
 //           v.read(receiveData); //imp
            //String modifiedSentence = new String(receiveData, "UTF-8");
            //System.out.println("FROM SERVER:" + modifiedSentence/* + " " + receiveData.length + " " + sendData.length*/);
     //   }
        v.close();
    }
}