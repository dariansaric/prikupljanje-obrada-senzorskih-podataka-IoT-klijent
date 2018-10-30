package darian.saric.rasus;

import darian.saric.rasus.background.ServerThread;
import darian.saric.rasus.model.Measurement;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class Client {

    private ServerThread serverThread = new ServerThread();
    private List<Measurement> measurements = new LinkedList<>();

    public Client(String ip, int port) {
        serverThread.setIp(ip);
        serverThread.setPort(port);
        serverThread.setMain(this);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 2) {
            System.err.println("Očekuju se dva argumenta: IP i port");
            return;
        }
        Client c;
        try {
            c = new Client(args[0], Integer.parseInt(args[1]));
        } catch (NumberFormatException e) {
            System.err.println("Očekuju se dva argumenta: IP i port");
            return;
        }
        c.serverThread.setDaemon(true);
        c.serverThread.start();

//        System.out.println("Napisi podatke za mjerenje:");
//        String param = sc.nextLine();
//        System.out.println("Napisi vrijednost");
//        double y = Double.parseDouble(sc.nextLine());
//
//        Measurement m = new Measurement(param, y);
//
//        Socket s = new Socket();
//        s.connect(new InetSocketAddress("127.0.0.1", 1999));
//
//        ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
//        ObjectInputStream is = new ObjectInputStream(s.getInputStream());
//        os.writeObject(m);
//
//        os.flush();
////            PacketSerialization
//
//        Measurement m1 = (Measurement) is.readObject();
//        System.out.println(m1);
    }

    public synchronized List<Measurement> getMeasurements() {
        return measurements;
    }
}
