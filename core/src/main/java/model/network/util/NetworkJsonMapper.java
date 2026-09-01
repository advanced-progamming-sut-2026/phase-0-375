package model.network.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import model.network.packet.Packet;

public final class NetworkJsonMapper {
    private static final ObjectMapper MAPPER = createMapper();

    private NetworkJsonMapper() {}

    public static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        return mapper;
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    /**
     * Serializes a Packet object to a single-line JSON string (without trailing newline).
     */
    public static String serialize(Packet packet) throws JsonProcessingException {
        return MAPPER.writeValueAsString(packet);
    }

    /**
     * Deserializes a single-line JSON string to the polymorphic Packet subtype.
     */
    public static Packet deserialize(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, Packet.class);
    }
}
