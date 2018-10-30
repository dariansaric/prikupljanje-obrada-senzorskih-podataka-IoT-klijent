package darian.saric.rasus.model;

import java.util.Objects;

/**
 * Ovaj razred predstavlja podatke o registriranim senzorima:
 * - identifikator senzora
 * - geolokacija senzora
 * - sva zapisana mjerenja senzora
 */
public class Sensor {
    private String username;
    private double latitude;
    private double longitude;
    private String ip;
    private int port;
    public Sensor(String username, double latitude, double longitude, String ip, int port) {
        this.username = username;
        this.latitude = latitude;
        this.longitude = longitude;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String toString() {
        return "Sensor{" +
                "username='" + username + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }

    public String getUsername() {
        return username;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sensor sensor = (Sensor) o;
        return Double.compare(sensor.latitude, latitude) == 0 &&
                Double.compare(sensor.longitude, longitude) == 0 &&
                port == sensor.port &&
                Objects.equals(username, sensor.username) &&
                Objects.equals(ip, sensor.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, latitude, longitude, ip, port);
    }
}
