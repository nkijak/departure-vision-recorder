package com.kinnack.dvr.kafka.models;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.Test;
import org.junit.Ignore;


import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

public class SerDeTests {

    @Test
    public void complexJson_thenOK() throws Exception { //snark on this test name...
        DepartureVision expected = new DepartureVision(
                Arrays.asList(new Departure("departsAt", "dest", "track", "line", "trainId", "status", "at", "color")),
                "object/key"
        );
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] intermedate = objectMapper.writeValueAsBytes(expected);
        DepartureVision actual = objectMapper.readValue(intermedate, DepartureVision.class);
        assertEquals(expected, actual);
    }

    @Test
    public void optionalToJson() throws Exception {
        Optional<Departure> departure = Optional.of(new Departure());
        Optional<Departure> none = Optional.<Departure>empty();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        byte[] intermediateDeparture = objectMapper.writeValueAsBytes(departure);
        byte[] intermediateNone = objectMapper.writeValueAsBytes(none);
        Optional<Departure> actualDeparture = objectMapper.readValue(intermediateDeparture, new TypeReference<Optional<Departure>>() {});
        assertTrue(actualDeparture.isPresent());
        Optional<Departure> actualNone = objectMapper.readValue(intermediateNone, new TypeReference<Optional<Departure>>() {});
        assertFalse("None should deserialize back to empty", actualNone.isPresent());
    }

    @Test
    public void testSerializationDe() {
        DepartureVision expected = new DepartureVision(
            Arrays.asList(new Departure("departsAt", "dest", "track", "line", "trainId", "status", "at", "color")),
            "object/key"
        );
        JsonPOJOSerializer serializer = new JsonPOJOSerializer<DepartureVision>();
        byte[] serializedData = serializer.serialize("topic", expected);
        JsonPOJODeserializer<DepartureVision> deserializer = new JsonPOJODeserializer<>();
        deserializer.configure(Map.of(JsonPOJODeserializer.JSON_POJO_CLASS, DepartureVision.class), false);
        DepartureVision actual = deserializer.deserialize("topic", serializedData);
        assertEquals(expected, actual);
    }

    @Test
    public void testDeserializationSe() throws Exception {
        String json = "{\"departures\":[{\"departsAt\": \"departsAt\", \"dest\": \"dest\", \"track\": \"track\", \"line\": \"line\", \"trainId\": \"trainId\", \"status\": \"status\", \"at\": \"at\", \"color\": \"color\"}], \"objectKey\": \"object/key\"}";
        byte[] data = json.getBytes("utf-8");
        JsonPOJODeserializer<DepartureVision> de = new JsonPOJODeserializer<>();
        de.configure(Map.of(JsonPOJODeserializer.JSON_POJO_CLASS, DepartureVision.class), false);
        DepartureVision actual = de.deserialize("topic", data);

        DepartureVision expected = new DepartureVision(
                Arrays.asList(new Departure("departsAt", "dest", "track", "line", "trainId", "status", "at", "color")),
                "object/key"
        );
        assertEquals(expected, actual);
    }
}
