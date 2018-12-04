import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Cette classe implémente l'algorithme de Lamport pour l'exclusion mutuelle en réparti.
 * */
public class LamportManager extends UnicastRemoteObject {

    private final int id;
    private final int nbSites;

    private Registry registry;
    private Message[] messages;
    private int logicalClock;

    public static void main(String[] args) throws RemoteException {
        int id = Integer.parseInt(args[0]);
        int nbSites = Integer.parseInt(args[1]);
        LamportManager manager = new LamportManager(id, nbSites);
        manager.rmiInit();
    }

    public LamportManager(int id, int nbSites) throws RemoteException {
        super();
        if(id >= nbSites) {
            throw new IllegalArgumentException("Id " + id + " is too big for nb of sites " + nbSites);
        }
        this.id = id;
        this.nbSites = nbSites;
        messages = new Message[nbSites];
        logicalClock = 0;
    }

    private void rmiInit() throws RemoteException {
        try {
            registry = LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            try {
                registry = LocateRegistry.getRegistry(1099);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }

        try {
            registry.bind("lamport" + id, this);
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }
    }

    private void send(Message message, int receiverId) throws RemoteException, NotBoundException {
        LamportManager other = (LamportManager) registry.lookup("lamport" + receiverId);
        other.receive(message);
    }

    private void receive(Message message) throws RemoteException, NotBoundException {
        int senderId = message.getSenderId();
        logicalClock = logicalClock > message.getStamp() ? logicalClock + 1 : message.getStamp() + 1;
        switch(message.getType()) {
            case REQ:
                messages[senderId] = message;
                int receiverId = senderId;
                send(new Message(MessageType.ACK, logicalClock, id), receiverId);
                break;
            case REL:
                messages[senderId] = message;
                break;
            case ACK:
                Message currentMessage = messages[senderId];
                if(currentMessage == null || currentMessage.getType() != MessageType.ACK) {
                    messages[senderId] = message;
                }
                break;
            default:
                throw new IllegalStateException("Illegal message type: " + message.getType());
        }
    }

    private boolean canAccessCriticalSection() {
        Message thisMessage = messages[id];
        if(thisMessage != null && thisMessage.getType() == MessageType.REQ) {
            for(Message m : messages) {
                if(m.getSenderId() != id &&
                        (m.getStamp() < thisMessage.getStamp() ||
                                (m.getStamp() == thisMessage.getStamp() && m.getSenderId() < id))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void accessCriticalSection() throws RemoteException, NotBoundException {
        logicalClock++;
        Message request = new Message(MessageType.REQ, logicalClock, id);
        messages[id] = request;
        for(int i = 0; i < nbSites; i++) {
            if(id != i) {
                send(request, i);
            }
        }
    }

    public void releaseCriticalSection() throws RemoteException, NotBoundException {
        Message release = new Message(MessageType.REL, logicalClock, id);
        messages[id] = release;
        for(int i = 0; i < nbSites; i++) {
            if(id != i) {
                send(release, i);
            }
        }
    }
}
