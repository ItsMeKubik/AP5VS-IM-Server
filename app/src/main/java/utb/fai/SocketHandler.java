package utb.fai;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.Set;

public class SocketHandler {
    Socket mySocket;
    String clientID;
    ActiveHandlers activeHandlers;
    ArrayBlockingQueue<String> messages = new ArrayBlockingQueue<String>(20);
    CountDownLatch startSignal = new CountDownLatch(2);
    OutputHandler outputHandler = new OutputHandler();
    InputHandler inputHandler = new InputHandler();
    volatile boolean inputFinished = false;

    String clientName = null; 
    Set<String> groups = ConcurrentHashMap.newKeySet(); 

    public SocketHandler(Socket mySocket, ActiveHandlers activeHandlers) {
        this.mySocket = mySocket;
        clientID = mySocket.getInetAddress().toString() + ":" + mySocket.getPort();
        this.activeHandlers = activeHandlers;
    }

    class OutputHandler implements Runnable {
        public void run() {
            try {
                startSignal.countDown();
                startSignal.await();
                OutputStreamWriter writer = new OutputStreamWriter(mySocket.getOutputStream(), "UTF-8");
                
                writer.write("Zadejte sve jmeno:\r\n");
                writer.flush();
                
                while (!inputFinished) {
                    String m = messages.take(); 
                    writer.write(m + "\r\n"); 
                    writer.flush(); 
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class InputHandler implements Runnable {
        public void run() {
            try {
                startSignal.countDown();
                startSignal.await();
                
                activeHandlers.add(SocketHandler.this);
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(mySocket.getInputStream(), "UTF-8"));
                String request = "";

                while ((request = reader.readLine()) != null) {
                    request = request.trim(); 
                    if (request.isEmpty()) continue;

                    if (clientName == null) {
                        if (request.contains(" ")) continue; 
                        
                        if (!activeHandlers.isNameTaken(request)) {
                            clientName = request;
                            groups.add("public");
                        }
                        continue; 
                    }

                    if (request.startsWith("#")) {
                        String[] parts = request.split(" ", 3);
                        String command = parts[0];

                        if (command.equals("#setMyName") && parts.length >= 2) {
                            String newName = parts[1];
                            if (!newName.contains(" ") && parts.length == 2) {
                                if (!activeHandlers.isNameTaken(newName)) {
                                    clientName = newName;
                                }
                            }
                        } 
                        else if (command.equals("#join") && parts.length >= 2) {
                            groups.add(parts[1]);
                        } 
                        else if (command.equals("#leave") && parts.length >= 2) {
                            groups.remove(parts[1]);
                        } 
                        else if (command.equals("#groups")) {
                            messages.offer(String.join(",", groups));
                        } 
                        else if (command.equals("#sendPrivate") && parts.length == 3) {
                            String targetName = parts[1];
                            String msgText = parts[2];
                            String formattedMsg = "[" + clientName + "] >> " + msgText;
                            activeHandlers.sendPrivateMessage(targetName, formattedMsg);
                        }
                    } 
                    else {
                        String formattedMsg = "[" + clientName + "] >> " + request;
                        activeHandlers.sendMessageToAll(SocketHandler.this, formattedMsg);
                    }
                }
                
                inputFinished = true;
                messages.offer("OutputHandler, wakeup and die!");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                activeHandlers.remove(SocketHandler.this);
            }
        }
    }
}