import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Cette classe implémente l'algorithme de Lamport pour l'exclusion mutuelle en réparti.
 * */
public class LamportManager extends UnicastRemoteObject implements ILamportManager{

    private final static int POOLING_DELAY = 10000;

    // TODO: lock

    private int id;
    private int nbSites;

    private int value;
    private Registry registry;
    private Message[] messages;
    private int logicalClock;
    private ILamportManager[] others;
    private boolean isCSRequested;

    public static void main(String[] args) throws RemoteException {
        int id = Integer.parseInt(args[0]);
        int nbSites = Integer.parseInt(args[1]);
        LamportManager manager = new LamportManager(id, nbSites);
        manager.rmiInit();

        // Background thread
        new Thread(() -> {
            while(true) {
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public LamportManager() throws RemoteException {
        super();
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
        others = new ILamportManager[nbSites];
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
            registry.rebind("lamport" + id, this);
        }
    }

    private void send(Message message, int receiverId) throws RemoteException {
        ILamportManager other = others[receiverId];
        if(other == null) {
            try {
                other = (ILamportManager) registry.lookup("lamport" + receiverId);
                others[receiverId] = other;
            } catch (NotBoundException e) {
                e.printStackTrace();
                return;
            }
        }
        other.receive(message);
    }

    private void sendAll(Message message) throws RemoteException {
        for(int i = 0; i < nbSites; i++) {
            if(id != i) {
                send(message, i);
            }
        }
    }

    @Override
    public void receive(Message message) throws RemoteException {
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
                value = message.getValue();
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

    @Override
    public void requestCriticalSection() throws RemoteException {
        if(!isCSRequested) {
            while(!canAccessCriticalSection()) {
                try {
                    Thread.sleep(POOLING_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            isCSRequested = true;
            logicalClock++;
            Message request = new Message(MessageType.REQ, logicalClock, id);
            messages[id] = request;
            sendAll(request);
        }
    }

    @Override
    public void releaseCriticalSection() throws RemoteException {
        Message release = new Message(MessageType.REL, logicalClock, id, value);
        messages[id] = release;
        sendAll(release);
        isCSRequested = false;
    }

    @Override
    public int getValue() throws RemoteException {
        return value;
    }

    @Override
    public void setValue(int value) throws RemoteException {
        this.value = value;
    }
}
