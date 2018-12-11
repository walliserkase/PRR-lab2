import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class App {

    private final int id;

    private ILamportManager lamportManager;
    private Registry registry;
    private Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws RemoteException {
        int id = Integer.parseInt(args[0]);
        App app = new App(id);
        app.rmiInit();
        while(true) {
            app.menu();
        }
    }

    public App(int id) {
        this.id = id;
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
            lamportManager = (ILamportManager) registry.lookup("lamport" + id);
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    private void menu() throws RemoteException {
        int choice = 0;
        while(choice != 1 && choice != 2) {
            System.out.println("Menu");
            System.out.println("1. Display value");
            System.out.println("2. Ask authorization for changing value");
            choice = scanner.nextInt();
        }

        if(choice == 1) {
            displayValue();
        } else {
            changeValue();
        }
    }

    private void displayValue() throws RemoteException {
        System.out.println(lamportManager.getValue());
    }

    private void changeValue() throws RemoteException {
        // Ask for CS access
        System.out.println("Current value: " + lamportManager.getValue());
        System.out.println("Waiting for authorization to change the value...");
        lamportManager.requestCriticalSection();

        // Change value
        System.out.println("Enter new value: ");
        lamportManager.setValue(scanner.nextInt());

        // Release CS access
        lamportManager.releaseCriticalSection();
    }
}
