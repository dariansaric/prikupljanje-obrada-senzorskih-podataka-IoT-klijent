package darian.saric.rasus.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Ovaj razred predstavlja jedno mjerenje koje obavlja {@link Sensor}.
 */
public class Measurement implements Serializable {

    private Integer temperature;
    private Integer pressure;
    private Integer humidity;
    private Integer co;
    private Integer no2;
    private Integer so2;

    public Measurement(Integer temperature, Integer pressure, Integer humidity, Integer co, Integer no2, Integer so2) {
        this.temperature = temperature;
        this.pressure = pressure;
        this.humidity = humidity;
        this.co = co;
        this.no2 = no2;
        this.so2 = so2;
    }

    public Integer getTemperature() {
        return temperature;
    }

    public Integer getPressure() {
        return pressure;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public Integer getCo() {
        return co;
    }

    public Integer getNo2() {
        return no2;
    }

    public Integer getSo2() {
        return so2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Measurement that = (Measurement) o;
        return Objects.equals(temperature, that.temperature) &&
                Objects.equals(pressure, that.pressure) &&
                Objects.equals(humidity, that.humidity) &&
                Objects.equals(co, that.co) &&
                Objects.equals(no2, that.no2) &&
                Objects.equals(so2, that.so2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(temperature, pressure, humidity, co, no2, so2);
    }

    @Override
    public String toString() {
        return "Measurement{" +
                "temperature=" + temperature +
                ", pressure=" + pressure +
                ", humidity=" + humidity +
                ", co=" + co +
                ", no2=" + no2 +
                ", so2=" + so2 +
                '}';
    }
}
