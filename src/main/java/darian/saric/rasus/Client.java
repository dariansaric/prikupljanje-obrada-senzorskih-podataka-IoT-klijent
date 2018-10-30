package darian.saric.rasus;

import darian.saric.rasus.background.ServerThread;
import darian.saric.rasus.model.Measurement;
import darian.saric.rasus.model.Sensor;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Client {
    private static final Path DATA_PATH = Paths.get("src/main/resources/mjerenja.csv");
    private static final int SERVER_PORT = 8080;
    private ServerThread serverThread = new ServerThread();
    private List<Measurement> measurements = readMeasurements();
    private InetSocketAddress serverAddress;
    private String username;

    private Client(String username, String ip, int port, String serverIp) throws IOException {
        this.username = username;
        serverThread.setIp(ip);
        serverThread.setPort(port);
        serverThread.setMain(this);
        serverAddress = new InetSocketAddress(InetAddress.getByName(serverIp), SERVER_PORT);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println("Očekuju se dva argumenta: IP i port i IP poslužitelja");
            return;
        }
        Client c;
        try {
            c = new Client(args[0], args[1], Integer.parseInt(args[2]), args[3]);
            while (true) {
                if (c.registerToServer()) {
                    break;
                }
            }

            c.serverThread.setDaemon(true);
            c.serverThread.start();

        } catch (NumberFormatException e) {
            System.err.println("Neispravna datoteka mjerenja");
            return;
        }

        System.out.println("Dobrodošli na sučelje za upravljanje senzorom, unesite naredbu ili KRAJ");
        Scanner scanner = new Scanner(System.in);

        for (boolean done = false; scanner.hasNext() && !done; ) {
            String command = scanner.nextLine();
            switch (command.toLowerCase()) {
                case "mjerenje":
                    c.measure();
                    break;

                case "kraj":
                    c.shutdown();
                    done = true;
                    break;

                default:
                    System.out.println("Neispravna naredba");
            }
        }

        scanner.close();
        System.out.println("Doviđenja!!!");

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
//
//        Measurement m1 = (Measurement) is.readObject();
//        System.out.println(m1);
    }

    private static List<Measurement> readMeasurements() throws IOException {
        final String first = "Temperature,Pressure,Humidity,CO,NO2,SO2,";
        List<Measurement> list = new LinkedList<>();

        for (String s : Files.readAllLines(DATA_PATH)) {
            if (s.equals(first)) {
                continue;
            }

            String[] n = s.replaceAll(",", ", ").split(",");
            list.add(new Measurement(
                    n[0].trim().isEmpty() ? null : Integer.parseInt(n[0].trim()),
                    n[1].trim().isEmpty() ? null : Integer.parseInt(n[1].trim()),
                    n[2].trim().isEmpty() ? null : Integer.parseInt(n[2].trim()),
                    n[3].trim().isEmpty() ? null : Integer.parseInt(n[3].trim()),
                    n[4].trim().isEmpty() ? null : Integer.parseInt(n[4].trim()),
                    n[5].trim().isEmpty() ? null : Integer.parseInt(n[5].trim())
            ));
        }

        return list;
    }

    private boolean registerToServer() throws IOException {
        Random r = new Random();
        Sensor s = new Sensor(username,
                r.nextDouble() * (16 - 15.87) + 15.87,
                r.nextDouble() * (45.85 - 45.75) + 45.75,
                serverThread.getIp(), serverThread.getPort());

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(String.format("http://%s:%d/central/rest/sensor",
                serverAddress.getHostName(), serverAddress.getPort()));
        httpPost.setEntity(new StringEntity(
                new JSONObject(s)
                        .toString(),
                ContentType.APPLICATION_JSON));

        JSONObject object = new JSONObject(EntityUtils.toString(
                client.execute(httpPost).getEntity(),
                StandardCharsets.UTF_8));

        return object.getBoolean("status");
    }

    private void shutdown() {
        // TODO: deregistracija
        // TODO: gašenje servera
    }

    private void measure() {
        Measurement m = measurements.get(Math.toIntExact(
                (System.currentTimeMillis() - serverThread.getStartTime()) / 1000));

        Socket s = new Socket();
        // TODO: dohvati sa servera podatke o najbližem susjedu
    }

    public synchronized List<Measurement> getMeasurements() {
        return measurements;
    }
}
