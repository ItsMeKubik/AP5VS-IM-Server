package utb.fai;

import java.util.*;

public class ActiveHandlers {
    private static final long serialVersionUID = 1L;
    private HashSet<SocketHandler> activeHandlersSet = new HashSet<SocketHandler>();

    synchronized void sendMessageToAll(SocketHandler sender, String message) {
        for (SocketHandler handler : activeHandlersSet) {
            if (handler != sender) {
                boolean shareGroup = false;
                for (String group : sender.groups) {
                    if (handler.groups.contains(group)) {
                        shareGroup = true;
                        break;
                    }
                }

                if (shareGroup) {
                    if (!handler.messages.offer(message)) {
                        System.err.printf("Client %s message queue is full...\n", handler.clientID);
                    }
                }
            }
        }
    }

    synchronized void sendPrivateMessage(String targetName, String message) {
        for (SocketHandler handler : activeHandlersSet) {
            if (handler.clientName != null && handler.clientName.equals(targetName)) {
                handler.messages.offer(message);
                break; 
            }
        }
    }

    synchronized boolean isNameTaken(String name) {
        for (SocketHandler handler : activeHandlersSet) {
            if (handler.clientName != null && handler.clientName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    synchronized boolean add(SocketHandler handler) {
        return activeHandlersSet.add(handler);
    }

    synchronized boolean remove(SocketHandler handler) {
        return activeHandlersSet.remove(handler);
    }
}