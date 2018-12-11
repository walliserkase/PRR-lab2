import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ILamportManager extends Remote {
    void receive(Message message) throws RemoteException;
    void requestCriticalSection() throws RemoteException;
    void releaseCriticalSection() throws RemoteException;
    int getValue() throws RemoteException;
    void setValue(int value) throws RemoteException;
}
