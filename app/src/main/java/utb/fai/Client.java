package utb.fai;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

/*
    TENTO SOUBOR BYL VYTVOŘEN VE CVIČENÍ 26.10
*/

public class Client {
    public static void main(String[] args) throws UnknownHostException {
        try (DatagramSocket socket = new DatagramSocket()) {
            final InetAddress serverAddress = InetAddress.getByName("localhost");
            final int port = 9999;


            Scanner scanner = new Scanner(System.in);
            final byte[] buffer = new byte[1024];
            while (true) {
                System.out.println("Zadejte zpravu: ");
                String message = scanner.nextLine();
                byte[] data = message.getBytes();
                
                DatagramPacket packet = new DatagramPacket(data,data.length, serverAddress, port);
                try {
                    socket.send(packet);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                DatagramPacket response = new DatagramPacket(buffer,buffer.length);
                try {
                    socket.receive(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String responseString = new String(response.getData(),0,response.getLength());
                System.out.println(responseString);
            }
            
        } catch (SocketException e) {
            e.printStackTrace();
        }        
    }
}
