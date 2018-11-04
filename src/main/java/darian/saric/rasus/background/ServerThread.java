package darian.saric.rasus.background;

import darian.saric.rasus.Client;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerThread extends Thread {
    /**
     * Broj usporednih mjerenja koje će se generirati za klijenta
     */
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

    public void quit() {
        active = false;
        threadPool.shutdown();
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(ip, port));
            System.out.println("Poslužitelj uspješno pokrenut na adresi: (" + ip + "," + port + ")...");
            while (active) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(new ClientWorker(clientSocket));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setMain(Client main) {
        this.main = main;
    }

    /**
     * @return {@linkplain #startTime}
     */
    public long getStartTime() {
        return startTime;
    }

    private class ClientWorker implements Runnable {
        private Socket clientSocket;
//        private BufferedReader reader;
//        private PrintWriter writer;

        ClientWorker(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            System.out.println("Opslužujem senzor na adresi (" +
                    clientSocket.getInetAddress().getHostAddress() + "," + clientSocket.getPort() + ")");
            try (PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(clientSocket.getOutputStream()), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
//                writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
//                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                for (int i = 0; i < NUMBER_OF_MEASUREMENTS_SENT; i++) {
                    JSONObject o = generateMeasureMent();
                    writer.println(o);
                    System.out.println("Poslano mjerenje: " + o);
                    String in = reader.readLine();
                    if (!in.equals("send")) {
                        throw new IOException("Greška pri komunikaciji");
                    }
                }
                writer.println("end");
                System.out.println("Opslužen zahtjev za adresu (" +
                        clientSocket.getInetAddress().getHostAddress() + "," + clientSocket.getPort() + ")");

            } catch (IOException e) {
                e.printStackTrace();

//            } finally {
//
//                try {
//                    writer.close();
//                    reader.close();
//                    clientSocket.close();
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        }

        /**
         * Vraća generirano mjerenje u JSON formatu.
         *
         * @return JSON format mjerenja
         */
        private JSONObject generateMeasureMent() {
            // TODO: rad s milisekundama?
            int currentSeconds = Math.toIntExact(System.currentTimeMillis() / 1000);
            System.out.println("Uređaj je aktivan " + currentSeconds + " sekundi...");
            int index = (Math.toIntExact(System.currentTimeMillis() / 1000)
                    - main.getSecondsStart())
                    % 100;
            System.out.println("Odabire se index " + index);
            return new JSONObject(
                    main.getMeasurements().get(index));
        }

    }
}
