package com.kinnack.dvr.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinnack.dvr.kafka.models.ChangeEvent;
import com.kinnack.dvr.kafka.models.Departure;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.ConsumerRecordFactory;
import org.apache.kafka.streams.test.OutputVerifier;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class TrackNumbersTest {
    TopologyTestDriver testDriver;
    ConsumerRecordFactory<String, JsonNode> sourceFactory;
    TrackNumbers trackNumbers = new TrackNumbers();

    @Before
    public void setup() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        Topology topology = trackNumbers.getStreamTopology("test.dvr");
        System.out.println(topology.describe());
        testDriver = new TopologyTestDriver(topology, props);
        sourceFactory = new ConsumerRecordFactory<>(new StringSerializer(), trackNumbers.jsonSerde.serializer());
    }

    @Test
    public void producesDeltaStream() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode step0 = mapper.readValue(getClass().getClassLoader().getResource("diffStream/00appears.json"), JsonNode.class);
        JsonNode step1 = mapper.readValue(getClass().getClassLoader().getResource("diffStream/01nochange.json"), JsonNode.class);
        JsonNode step2 = mapper.readValue(getClass().getClassLoader().getResource("diffStream/02statuschange.json"), JsonNode.class);
        JsonNode step3 = mapper.readValue(getClass().getClassLoader().getResource("diffStream/03departs.json"), JsonNode.class);

        testDriver.pipeInput(sourceFactory.create("test.dvr", "", step0));
        ProducerRecord<String, ChangeEvent> outputRecord = testDriver.readOutput("test.dvr.diff", new StringDeserializer(), trackNumbers.changeEventSerDe.deserializer());
        OutputVerifier.compareKeyValue(outputRecord, "test.dvr", ChangeEvent.added(Departure.fromJsonNode(step0.get("body").withArray("departures").get(0))));

        testDriver.advanceWallClockTime(120000L); //two min
        testDriver.pipeInput(sourceFactory.create("test.dvr", "", step1));
        outputRecord = testDriver.readOutput("test.dvr.diff", new StringDeserializer(), trackNumbers.changeEventSerDe.deserializer());
        assertEquals("Input that doesn't cause change should not produce records", null, outputRecord);


        testDriver.advanceWallClockTime(120000L); //two min
        testDriver.pipeInput(sourceFactory.create("test.dvr", "", step2));
        outputRecord = testDriver.readOutput("test.dvr.diff", new StringDeserializer(), trackNumbers.changeEventSerDe.deserializer());
        OutputVerifier.compareKeyValue(outputRecord, "test.dvr",
                ChangeEvent.changed(
                        Departure.fromJsonNode(step0.get("body").withArray("departures").get(0)),
                        Departure.fromJsonNode(step2.get("body").withArray("departures").get(0))).get());

        testDriver.advanceWallClockTime(120000L); //two min
        testDriver.pipeInput(sourceFactory.create("test.dvr", "", step3));
        outputRecord = testDriver.readOutput("test.dvr.diff", new StringDeserializer(), trackNumbers.changeEventSerDe.deserializer());
        OutputVerifier.compareKeyValue(outputRecord, "test.dvr", ChangeEvent.dropped(Departure.fromJsonNode(step2.get("body").withArray("departures").get(0))));

    }

}
