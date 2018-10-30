package darian.saric.rasus.background;

import com.google.gson.Gson;
import darian.saric.rasus.model.Measurement;

import java.nio.charset.StandardCharsets;

public class PacketSerialization {
    private static final Gson gson = new Gson();

    public static byte[] serialize(Measurement object) {
        return gson.toJson(object, Measurement.class).getBytes(StandardCharsets.UTF_8);
    }

    public static Measurement deserialize(byte[] data, int offset, int length) {
        String string = new String(data, offset, length, StandardCharsets.UTF_8);
        return gson.fromJson(string, Measurement.class);
    }
}
