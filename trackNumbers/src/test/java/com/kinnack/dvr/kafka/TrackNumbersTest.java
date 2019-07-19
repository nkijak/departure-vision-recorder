package com.kinnack.dvr.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinnack.dvr.kafka.models.ChangeEvent;
import com.kinnack.dvr.kafka.models.Departure;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.test.ConsumerRecordFactory;
import org.apache.kafka.streams.test.OutputVerifier;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class TrackNumbersTest {
    TopologyTestDriver testDriver;
    ConsumerRecordFactory<String, JsonNode> sourceFactory;
    TrackNumbers trackNumbers = new TrackNumbers();
    Properties props = new Properties();

    public void setup(Topology topology) {
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");


        System.out.println(topology.describe());
        testDriver = new TopologyTestDriver(topology, props);
        sourceFactory = new ConsumerRecordFactory<>(new StringSerializer(), trackNumbers.jsonSerde.serializer());
    }


    @Test
    public void producesDeltaStream() throws IOException {
        setup(trackNumbers.getStreamTopology("test.dvr"));

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
        testDriver.close();
    }

    private void testDiffToTrackListing(Supplier<ChangeEvent> setupChangeEvent, String expectedKey, String expectedValue) {
        final String input = "test.in";
        final String output = "test.out";
        StreamsBuilder builder = new StreamsBuilder();
        trackNumbers.determineTrackNumber(
                builder.stream(input,
                        Consumed.with(Serdes.String(), trackNumbers.changeEventSerDe))
        ).to(output, Produced.with(Serdes.String(), Serdes.String()));
        setup(builder.build(props));
        ConsumerRecordFactory<String, ChangeEvent> source = new ConsumerRecordFactory<>(new StringSerializer(), trackNumbers.changeEventSerDe.serializer());
        testDriver.pipeInput(source.create(input, "stationId", setupChangeEvent.get(), 0L));
        Deserializer<String> sd = new StringDeserializer();
        ProducerRecord<String, String> outputRecord = testDriver.readOutput(output, sd, sd);
        OutputVerifier.compareKeyValue(outputRecord, expectedKey, expectedValue);
        testDriver.close();
    }

    @Test
    public void diffToTrackListing_OutputsTracks() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode trackListing = mapper
                .readValue(getClass().getClassLoader().getResource("diffStream/02statuschange.json"), JsonNode.class)
                .get("body")
                .withArray("departures")
                .get(0);
        ConsumerRecordFactory<String, ChangeEvent> source = new ConsumerRecordFactory<>(new StringSerializer(), trackNumbers.changeEventSerDe.serializer());
        ChangeEvent removed = ChangeEvent.dropped(Departure.fromJsonNode(trackListing));
        testDiffToTrackListing(() -> removed, "1970-01-01T00:00:00Z::stationId::trainId", "track");
    }

    @Test
    public void diffToTrackListing_OutputsCancelledCode() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode trackListing = mapper
                .readValue(getClass().getClassLoader().getResource("diffStream/02statuschange.json"), JsonNode.class)
                .get("body")
                .withArray("departures")
                .get(0);
        Departure departure = Departure.fromJsonNode(trackListing);
        departure.setStatus("CANCELLED");
        departure.setTrack(null);

        ConsumerRecordFactory<String, ChangeEvent> source = new ConsumerRecordFactory<>(new StringSerializer(), trackNumbers.changeEventSerDe.serializer());
        ChangeEvent removed = ChangeEvent.dropped(departure);
        testDiffToTrackListing(() -> removed, "1970-01-01T00:00:00Z::stationId::trainId", "cancelled");
    }

    @Test
    public void diffToTrackListing_OutputsUnknownCodeForDisapperanceNullTrack() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode trackListing = mapper
                .readValue(getClass().getClassLoader().getResource("diffStream/02statuschange.json"), JsonNode.class)
                .get("body")
                .withArray("departures")
                .get(0);
        Departure departure = Departure.fromJsonNode(trackListing);
        departure.setStatus("");
        departure.setTrack(null);

        ConsumerRecordFactory<String, ChangeEvent> source = new ConsumerRecordFactory<>(new StringSerializer(), trackNumbers.changeEventSerDe.serializer());
        ChangeEvent removed = ChangeEvent.dropped(departure);
        testDiffToTrackListing(() -> removed, "1970-01-01T00:00:00Z::stationId::trainId", "unknown");
    }

    @Test
    public void diffToTrackListing_OutputsUnknownCodeForDisapperanceEmptyTrack() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode trackListing = mapper
                .readValue(getClass().getClassLoader().getResource("diffStream/02statuschange.json"), JsonNode.class)
                .get("body")
                .withArray("departures")
                .get(0);
        Departure departure = Departure.fromJsonNode(trackListing);
        departure.setStatus("");
        departure.setTrack("");

        ConsumerRecordFactory<String, ChangeEvent> source = new ConsumerRecordFactory<>(new StringSerializer(), trackNumbers.changeEventSerDe.serializer());
        ChangeEvent removed = ChangeEvent.dropped(departure);
        testDiffToTrackListing(() -> removed, "1970-01-01T00:00:00Z::stationId::trainId", "unknown");
    }


    @Ignore
    @Test
    public void producesHoppingCounts() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode step0 = mapper.readValue(getClass().getClassLoader().getResource("diffStream/00appears.json"), JsonNode.class);
        JsonNode step1 = mapper.readValue(getClass().getClassLoader().getResource("diffStream/01nochange.json"), JsonNode.class);
        JsonNode step2 = mapper.readValue(getClass().getClassLoader().getResource("diffStream/02statuschange.json"), JsonNode.class);
        JsonNode step3 = mapper.readValue(getClass().getClassLoader().getResource("diffStream/03departs.json"), JsonNode.class);

        testDriver.pipeInput(sourceFactory.create("test.dvr", "", step0, 0L));

        testDriver.advanceWallClockTime(120000L); //two min
        testDriver.pipeInput(sourceFactory.create("test.dvr", "", step1));

        testDriver.advanceWallClockTime(120000L); //two min
        testDriver.pipeInput(sourceFactory.create("test.dvr", "", step2));

        testDriver.advanceWallClockTime(Duration.ofDays(1).toMillis()); // next day
        testDriver.pipeInput(sourceFactory.create("test.dvr", "", step0));

        testDriver.advanceWallClockTime(120000L); //two min
        testDriver.pipeInput(sourceFactory.create("test.dvr", "", step1));

        testDriver.advanceWallClockTime(120000L); //two min
        testDriver.pipeInput(sourceFactory.create("test.dvr", "", step2));


        HashMap<String, Integer> expectedCounts = new HashMap<>();
        expectedCounts.put("track", 1);
        HashMap<String, HashMap<String, Integer>> expected = new HashMap<>();
        expected.put("trainId", expectedCounts);
        ProducerRecord<String, HashMap<String, HashMap<String, Integer>>> outputRecord = testDriver.readOutput("test.dvr.diff", new StringDeserializer(), trackNumbers.trackStatsSerDe.deserializer());
        OutputVerifier.compareKeyValue(outputRecord, "1970-01-01T00:00:00Z::test.dvr::trainId", expected);


    }
}
