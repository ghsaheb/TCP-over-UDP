import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;
import java.nio.*;
public class Test
{
    public static void main(String[] args) throws Exception
    {
        /*
        byte[] arr = new byte[10];
        arr[0] = 10;
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(1010);
        byte[] result = b.array();
        String modifiedSentence = new String(result, "UTF-8");
        System.out.println(Arrays.toString(result));
        int x = ByteBuffer.wrap(result).getInt();
        System.out.println(x);
        
        GVPHeader gh = new GVPHeader(1010, 1020);
        gh.setSYN(true);
        gh.setACK(true);
        gh.setFIN(false);
        gh.setSeqNumber(20);
        gh.setACKNumber(0);
        System.out.println("CHECK");
        result = gh.getArray();
        System.out.println(Arrays.toString(result));

    //    GVPHeader v = new GVPHeader(result);
//        byte[] sendData1 = "string".getBytes();
//        byte[] sendData2 = "ghazal".getBytes();
    //    System.out.println(Arrays.toString(concat(sendData1, sendData2)));
      */
        byte[] array = new byte[10];
        DatagramSocket socket = new DatagramSocket(9999);
        DatagramPacket pck = new DatagramPacket(array, array.length);

    }
    
}