package com.kinnack.dvr.kafka.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.*;

public class DepartureTest {

    @Test
    public void setTrack_EmptyIsNull() {
        Departure d = new Departure();
        d.setTrack("");
        assertNull(d.getTrack());

    }

    @Test
    public void fromJsonNodeSetsNullValuesCorrectly() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readValue("{\"track\": null, \"train_id\": \"trainId\"}", JsonNode.class);
        Departure actual = Departure.fromJsonNode(node);
        assertNull(actual.getTrack());
        assertNull(actual.getStatus());
        assertEquals("trainId", actual.getTrainId());
    }
}