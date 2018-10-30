package darian.saric.rasus.background;

import darian.saric.rasus.Client;
import darian.saric.rasus.model.Measurement;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerThread extends Thread {
    public static final int NUMBER_OF_MEASUREMENTS_SENT = 3;
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
//                Measurement m = (Measurement) objectInputStream.readObject();

                StringBuilder builder = new StringBuilder();
                byte[] buf = new byte[1024];
//                Measurement newMeasurement = compareMeasurements(m, list);
                for(int i = 0; i < NUMBER_OF_MEASUREMENTS_SENT;i++) {
                    os.writeObject(generateMeasureMent());
                    os.flush();
                    if (i < 2) {
                        while (pushbackInputStream.available() > 0) {
                            int read = pushbackInputStream.read(buf);
                            builder.append(new String(buf, 0, read));
                        }
                    }
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
