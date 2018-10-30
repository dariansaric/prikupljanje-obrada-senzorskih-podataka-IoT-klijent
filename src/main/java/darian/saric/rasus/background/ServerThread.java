package darian.saric.rasus.background;

import darian.saric.rasus.Client;
import darian.saric.rasus.model.Measurement;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerThread extends Thread {
    private static final int NUMBER_OF_MEASUREMENTS_SENT = 3;
    private final long startTime = System.currentTimeMillis();
    private Client main;
    private ExecutorService threadPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() - 1);
    private String ip;
    private int port;
    private boolean active = true;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    void quit() {
        active = false;
        threadPool.shutdown();
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(ip, port));

            while (active) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(new ClientWorker(clientSocket));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setMain(Client main) {
        this.main = main;
    }

    public long getStartTime() {
        return startTime;
    }

//    public void setActive(boolean active) {
//        this.active = active;
//    }

    private class ClientWorker implements Runnable {
        private Socket clientSocket;
        private PushbackInputStream pushbackInputStream;
        private OutputStream outputStream;

        ClientWorker(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                pushbackInputStream = new PushbackInputStream(clientSocket.getInputStream());
                outputStream = clientSocket.getOutputStream();

                ObjectOutputStream os = new ObjectOutputStream(outputStream);
                ObjectInputStream objectInputStream = new ObjectInputStream(pushbackInputStream);
//                Measurement m = (Measurement) objectInputStream.readObject();


//                Measurement newMeasurement = compareMeasurements(m, list);
                for(int i = 0; i < NUMBER_OF_MEASUREMENTS_SENT;i++) {
                    os.writeObject(generateMeasureMent());
                }

            } catch (IOException e) {
                e.printStackTrace();

            } finally {

                try {
                    outputStream.flush();
                    outputStream.close();
                    clientSocket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private Measurement generateMeasureMent() {
            return null;
        }

    }
}
