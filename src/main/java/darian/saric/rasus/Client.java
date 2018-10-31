package darian.saric.rasus;

import darian.saric.rasus.background.ServerThread;
import darian.saric.rasus.model.Measurement;
import darian.saric.rasus.model.Sensor;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Client {
    /**
     * {@link Path} do podataka o mjerenjima
     */
    private static final Path DATA_PATH = Paths.get("src/main/resources/mjerenja.csv");
    /**
     * Port REST poslužitelja (unaprijed dogovoreno)
     */
    private static final int SERVER_PORT = 8080;
    /**
     * Vremenski zapis o početku rada klijenta
     */
    private final int secondsStart = Math.toIntExact(System.currentTimeMillis() / 1000);
    /**
     * Dretva poslužitelja za komunikaciju s drugim senzorima
     */
    private ServerThread serverThread = new ServerThread();
    /**
     * {@link List} iz koje se generiraju nasumična mjerenja
     */
    private List<Measurement> measurements = readMeasurements();
    /**
     * {@link InetSocketAddress} objekt koji omata podatke o REST poslužitelju
     */
    private InetSocketAddress serverAddress;
    /**
     * Korisničko ime senzora
     */
    private String username;

    /**
     * Konstruktor.
     *
     * @param username {@linkplain #username}
     * @param ip       IP adresa senzora
     * @param port     port senzora
     * @param serverIp IP adresa REST poslužitelja
     */
    private Client(String username, String ip, int port, String serverIp) throws IOException {
        this.username = username;
        serverThread.setIp(ip);
        serverThread.setPort(port);
        serverThread.setMain(this);
        serverAddress = new InetSocketAddress(InetAddress.getByName(serverIp), SERVER_PORT);
    }

    /**
     * Hešteg Mejijn Metoda.
     *
     * @param args argumenti narednika retka
     */
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
                    System.out.println("Senzor '" + c.username + "' uspješno registriran");
                    break;
                }
            }

            System.out.println("Pokrećem poslužitelj za druge senzore...");
            c.serverThread.setDaemon(true);
            c.serverThread.start();

        } catch (NumberFormatException e) {
            System.err.println("Neispravna datoteka mjerenja");
            return;
        }

        System.out.println("Dobrodošli na sučelje za upravljanje senzorom '" + c.username + "'!!!!");
        System.out.println("Unesite naredbu ili KRAJ");
        Scanner scanner = new Scanner(System.in);

        for (boolean done = false; !done && scanner.hasNext(); ) {
            String command = scanner.nextLine();
            switch (command.toLowerCase()) {
                case "mjerenje":
                    c.measure();
                    System.out.println("Unesite naredbu ili KRAJ");
                    break;

                case "kraj":
                    c.shutdown();
                    done = true;
                    break;

                default:
                    System.out.println("Neispravna naredba");
                    System.out.println("Unesite naredbu ili KRAJ");
            }
        }

        scanner.close();
        System.out.println("Doviđenja!!!");

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

    /**
     * Registrira senzor na centralni poslužitelj te vraća status uspješnosti registracije.
     */
    private boolean registerToServer() throws IOException {
        final String REGISTER_URL_FORMAT = "http://%s:%d/central/rest/sensor";
        final String REGISTER_STATUS_KEY = "status";

        System.out.println("Pokrećem spajanje na centralni poslužitelj...");

        //generiranje geolokacije
        Random r = new Random();
        Sensor s = new Sensor(username,
                r.nextDouble() * (16 - 15.87) + 15.87,
                r.nextDouble() * (45.85 - 45.75) + 45.75,
                serverThread.getIp(), serverThread.getPort());


        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost httpPost = new HttpPost(String.format(REGISTER_URL_FORMAT,
                    serverAddress.getHostName(), SERVER_PORT));
            httpPost.setEntity(new StringEntity(
                    new JSONObject(s)
                            .toString(),
                    ContentType.APPLICATION_JSON));

            return new JSONObject(EntityUtils.toString(
                    client.execute(httpPost).getEntity(),
                    StandardCharsets.UTF_8)).getBoolean(REGISTER_STATUS_KEY);
        }
    }

    private void shutdown() throws IOException {
        serverThread.quit();
        deregisterFromServer();
    }

    private void deregisterFromServer() throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpDelete httpDelete = new HttpDelete(
                    String.format("http://%s:%d/central/rest/sensor/%s",
                            serverAddress.getHostName(), serverAddress.getPort(), username));

            while (true) {
                if (client.execute(httpDelete).getStatusLine().getStatusCode() == 200) {
                    break;
                }
            }
        }
        System.out.println(username + " uspješno deregistriran sa centralnog poslužitelja!!");
    }

    private void measure() throws IOException {
        System.out.println("Generiram mjerenje...");
        Measurement m = measurements.get(Math.toIntExact(
                (System.currentTimeMillis() - serverThread.getStartTime()) / 1000) % 100);
        System.out.println("Generirano mjerenje " + m);

        Sensor s = getClosestNeighbour();
        if (s == null) {
            System.out.println("Senzor nema susjeda, šaljem generirano mjerenje...");
        } else {
            m = getAverageMeasurement(s.getIp(), s.getPort(), m);
            System.out.println("Šaljem umjereno mjerenje " + m);
        }

        while (true) {
            if (storeMeasurement(m)) {
                System.out.println("Mjerenje " + m + " uspješno pohranjeno na centralni poslužitelj...");
                break;
            }
        }
    }

    /**
     * Pohranjuje predani {@linkplain Measurement} i vraća status uspješnosti pohrane.
     *
     * @param m neko {@linkplain Measurement}
     *
     * @return status uspješnosti pohrane
     */
    private boolean storeMeasurement(Measurement m) throws IOException {
        final String POST_MEASUREMENT_URL = "http://%s:%d/central/rest/sensor/%s/measure";
        final String RESPONSE_STATUS_KEY = "status";

        System.out.println("Započinjem slanje mjerenja " + m);
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost httpPost = new HttpPost(
                    String.format(POST_MEASUREMENT_URL, serverAddress.getHostName(), SERVER_PORT, username));

            httpPost.setEntity(new StringEntity(new JSONObject(m).toString(), ContentType.APPLICATION_JSON));
            return new JSONObject(EntityUtils.toString(
                    httpClient.execute(httpPost).getEntity(), StandardCharsets.UTF_8))
                    .getBoolean(RESPONSE_STATUS_KEY);
        }
    }

    /**
     * Vraća umjereno mjerenje za predani {@linkplain Measurement} u komunikaciji s najbližim senzorom.
     *
     * @param ip   IP adresa senzora
     * @param port port senzora
     * @param m    generirano mjerenje
     *
     * @return umjereno mjerenje
     */
    private Measurement getAverageMeasurement(String ip, int port, Measurement m) throws IOException {
        final String SERVER_STOP_WORD = "end";
        final String CLIENT_SEND_MORE_WORD = "send";
        List<Measurement> ms = new ArrayList<>(ServerThread.NUMBER_OF_MEASUREMENTS_SENT + 1);
        ms.add(m);

        try (Socket socket = new Socket(ip, port)) {
            System.out.println("Započinjem komunikaciju sa susjednim senzorom na adresi: " + socket);

            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            for (String json = in.readLine(); !json.equals(SERVER_STOP_WORD); json = in.readLine()) {
                JSONObject o = new JSONObject(json);
                ms.add(new Measurement(
                        o.has("temperature") ? o.getInt("temperature") : null,
                        o.has("pressure") ? o.getInt("pressure") : null,
                        o.has("humidity") ? o.getInt("humidity") : null,
                        o.has("co") ? o.getInt("co") : null,
                        o.has("no2") ? o.getInt("no2") : null,
                        o.has("so2") ? o.getInt("so2") : null));

                out.println(CLIENT_SEND_MORE_WORD);
            }
        }

        return generateAverageMeasurement(ms);
    }

    /**
     * Generira umjereno mjerenje.
     *
     * @param ms generirano i primljena mjerenja
     *
     * @return umjereno mjerenje
     */
    private Measurement generateAverageMeasurement(List<Measurement> ms) {
        int t = Math.toIntExact(Math.round(
                ms.stream()
                        .map(Measurement::getTemperature)
                        .filter(Objects::nonNull)
                        .mapToInt(x -> x)
                        .average().orElse(-1)));

        int p = Math.toIntExact(Math.round(
                ms.stream()
                        .map(Measurement::getPressure)
                        .filter(Objects::nonNull)
                        .mapToInt(x -> x)
                        .average().orElse(-1)));

        int h = Math.toIntExact(Math.round(
                ms.stream()
                        .map(Measurement::getHumidity)
                        .filter(Objects::nonNull)
                        .mapToInt(x -> x)
                        .average().orElse(-1)));

        int co = Math.toIntExact(Math.round(
                ms.stream()
                        .map(Measurement::getCo)
                        .filter(Objects::nonNull)
                        .mapToInt(x -> x)
                        .average().orElse(-1)));

        int no2 = Math.toIntExact(Math.round(
                ms.stream()
                        .map(Measurement::getNo2)
                        .filter(Objects::nonNull)
                        .mapToInt(x -> x)
                        .average().orElse(-1)));

        int so2 = Math.toIntExact(Math.round(
                ms.stream()
                        .map(Measurement::getSo2)
                        .filter(Objects::nonNull)
                        .mapToInt(x -> x)
                        .average().orElse(-1)));

        return new Measurement(
                t == -1 ? null : t,
                p == -1 ? null : p,
                h == -1 ? null : h,
                co == -1 ? null : co,
                no2 == -1 ? null : no2,
                so2 == -1 ? null : so2);
    }

    private Sensor getClosestNeighbour() throws IOException {
        final String GET_NEIGHBOUR_URL = "http://%s:%d/central/rest/sensor/%s";
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

            HttpGet httpGet = new HttpGet(
                    String.format(GET_NEIGHBOUR_URL, serverAddress.getHostString(), SERVER_PORT, username));
            String s = EntityUtils.toString(client.execute(httpGet).getEntity(), StandardCharsets.UTF_8);

            if (s.equals("null")) {
                return null;
            }
            JSONObject object = new JSONObject(s);
            return new Sensor(
                    object.getString("username"),
                    object.getDouble("latitude"),
                    object.getDouble("longitude"),
                    object.getString("ip"),
                    object.getInt("port"));

        }
    }

    public synchronized List<Measurement> getMeasurements() {
        return measurements;
    }

    public int getSecondsStart() {
        return secondsStart;
    }
}
