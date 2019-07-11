package com.kinnack.dvr.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.kinnack.dvr.kafka.models.*;
import com.kinnack.dvr.kafka.models.ChangeEvent.ChangeEventOps;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.kinnack.dvr.kafka.models.ChangeEvent.ChangeEventOps.*;
import static com.kinnack.dvr.kafka.models.JsonPOJODeserializer.*;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Stream.*;

import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackNumbers {
    static final String STATION_ARG="station";
    static final String KAFKA_ARG="kafka";
    static final String STREAM_ARG="stream";
    static final Logger logger = LoggerFactory.getLogger(TrackNumbers.class);

    static KTable<String, Departure> currentState(KGroupedStream<String, Departure> streamByTrainId) {
        return streamByTrainId.reduce((trainId, departure) -> {
            if (departure.getStatus() != null || departure.getTrack() != null) {
                return departure;
            } else {
                return null;
            }
        });
    }

    final public Serde<Departure> departureSerde;
    final public Serde<List<Departure>> listDepartureSerde;
    final public Serde<ChangeEvent> changeEventSerDe;
    final public Serde<JsonNode> jsonSerde;
    final public Serde<HashMap<String, String>> trackCountSerDe;
    final public Serde<HashMap<String, HashMap<String, Integer>>> trackStatsSerDe;

    public TrackNumbers() {
        // TODO get rid of this and use the departureSerializer
        final Serializer<JsonNode> jsonSerializer = new JsonSerializer();
        final Deserializer<JsonNode> jsonDeserializer = new JsonDeserializer();
        jsonDeserializer.configure(Map.of(JSON_POJO_CLASS, JsonNode.class), false);
        jsonSerde = Serdes.serdeFrom(jsonSerializer, jsonDeserializer);

        // example of how to make a flat json SerDe
        departureSerde = getSerDe(Departure.class);

        //-----
        final Serializer<List<Departure>> listDepartureSerializer = new JsonPOJOSerializer<>();
        final Deserializer<List<Departure>> listDepartureDeserializer = new JsonPOJODeserializer<>();
        listDepartureDeserializer.configure(Map.of(JSON_POJO_TYPE, new TypeReference<List<Departure>>() { }), false);
        listDepartureSerde = Serdes.serdeFrom(listDepartureSerializer, listDepartureDeserializer);

        //-----
        final TypeReference<ChangeEvent> changeEventReference =  new TypeReference<>() { };
        final Serializer<ChangeEvent> changeEventSerializer = new JsonPOJOSerializer<>();
        final Deserializer<ChangeEvent> changeEventDeserializer = new JsonPOJODeserializer<>();
        changeEventDeserializer.configure(Map.of(JSON_POJO_TYPE, changeEventReference), false);
        changeEventSerDe = Serdes.serdeFrom(changeEventSerializer, changeEventDeserializer);

        //-----
        final TypeReference<HashMap<String, String>> trackCountReference =  new TypeReference<>() { };
        final Serializer<HashMap<String, String>> trackCountSerializer = new JsonPOJOSerializer<>();
        final Deserializer<HashMap<String, String>> trackCountDeserializer = new JsonPOJODeserializer<>();
        trackCountDeserializer.configure(Map.of(JSON_POJO_TYPE, trackCountReference), false);
        trackCountSerDe = Serdes.serdeFrom(trackCountSerializer, trackCountDeserializer);

        //-----
        trackStatsSerDe = getSerDe(new TypeReference<>(){});
    }

    public <T> Serde<T> getSerDe(Class<T> klass) {
        final Serializer<T> serializer = new JsonPOJOSerializer<>();
        final Deserializer<T> deserializer = new JsonPOJODeserializer<>();
        deserializer.configure(Map.of(JSON_POJO_CLASS, klass), false);
        return Serdes.serdeFrom(serializer, deserializer);
    }

    public <T> Serde<T> getSerDe(TypeReference<T> ref) {
        final Serializer<T> serializer = new JsonPOJOSerializer<>();
        final Deserializer<T> deserializer = new JsonPOJODeserializer<>();
        deserializer.configure(Map.of(JSON_POJO_TYPE, ref), false);
        return Serdes.serdeFrom(serializer, deserializer);
    }

    public KGroupedStream<String, Departure> groupByTrainId(KStream<String, List<Departure>> departures) {
        KGroupedStream<String, Departure> streamByTrainId =
                departures.flatMap((key, ds) -> ds.stream()
                        .map((d) -> KeyValue.pair(d.getTrainId(), d))
                        .collect(Collectors.toList()))
                        .groupByKey(Grouped.with(Serdes.String(), departureSerde))
                ;
        return streamByTrainId;
    }


    public Topology getStreamTopology(final String inTopic) {
        final StreamsBuilder builder = new StreamsBuilder();
        builder.addStateStore(Stores.keyValueStoreBuilder(
                Stores.inMemoryKeyValueStore(DiffProcessor.DIFF_STATE_STORE),
                Serdes.String(),
                listDepartureSerde));
        KStream<String, JsonNode> source = builder.stream(inTopic, Consumed.with(Serdes.String(), jsonSerde));
        KStream<String, List<Departure>> departures = source.map((key, jsonValue) ->
                KeyValue.pair(inTopic,
                        StreamSupport.stream(spliteratorUnknownSize(jsonValue.get("body").withArray("departures").elements(), 0), false)
                            .map(Departure::fromJsonNode)
                            .collect(Collectors.toList())
                )
        );
        KStream<String, ChangeEvent> diffStream = departures
                .transform(() -> new DiffProcessor(), DiffProcessor.DIFF_STATE_STORE);

        KTable<String, Departure> departureBoard = diffStream
                .groupBy(
                        (station, event) -> Optional.ofNullable(event.getWas()).orElse(event.getNow()).getTrainId(),
                        Grouped.with(Serdes.String(), changeEventSerDe))
                .aggregate(
                    () -> null,
                    (trainId, event, prev) -> {
                        if (!isDropped(event)) {
                            return Optional.ofNullable(event.getNow()).orElse(event.getWas());
                        }
                        return null;
                    },
                    Materialized.with(Serdes.String(), departureSerde)
                );

        diffStream.to(inTopic + ".diff", Produced.with(Serdes.String(), changeEventSerDe));

        diffStream
                .filter((stationId, changeEvent) -> isDropped(changeEvent))
                .groupByKey(Grouped.with(Serdes.String(), changeEventSerDe))
                .windowedBy(TimeWindows.of(Duration.ofDays(1)))
                .aggregate(
                        () -> new HashMap<String, String>(),
                        (stationId, event, trackByTrainId) -> {
                            String track = Optional.ofNullable(event.getWas().getTrack()).orElseGet(() -> {
                                if (isCancelled(event)) return "-1";
                                return "-2";
                            });
                            trackByTrainId.put(getTrainId(event), track);
                            return trackByTrainId;
                        },
                        Materialized.with(Serdes.String(), trackCountSerDe)
                )
                .toStream()
                .flatMap((windowedStationId, trackByTrainId) ->
                        trackByTrainId.entrySet().stream()
                                .map((entry) ->
                                        KeyValue.pair(
                                                String.join("::", List.of(windowedStationId.window().startTime().toString(), windowedStationId.key(), entry.getKey())),
                                                entry.getValue()
                                        ))
                                .collect(Collectors.toList())
                )
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
                .windowedBy(TimeWindows.of(Duration.ofDays(5)).advanceBy(Duration.ofDays(1)))
                .aggregate(
                        () -> new HashMap<String, HashMap<String, Integer>>(),
                        (dayStationTrainId, track, stats) -> {
                            String[] parts = dayStationTrainId.split("::");
                            String trainId = parts[parts.length - 1];
                            HashMap<String, Integer> tracks = stats.getOrDefault(trainId, new HashMap<>());
                            int count = tracks.getOrDefault(track, 0);
                            tracks.put(track, count + 1);
                            stats.put(trainId, tracks);
                            return stats;
                        },
                        Materialized.with(Serdes.String(), trackStatsSerDe)
                )
                .toStream()
                .map((k, v) -> KeyValue.pair(k.key(), v))
                .peek((k, stats) -> {
                    logger.info(k);
                })
                .to(inTopic + ".hopping_track_stats", Produced.with(Serdes.String(), trackStatsSerDe))
        ;
//                .foreach((windowedStationId, trackByTrainId) -> {
//                        logger.info("Windowed Station: "+windowedStationId.window().startTime()+" -> "+windowedStationId.window().endTime()+ " : " + windowedStationId.key());
//                    List<String> trains = new ArrayList<>(trackByTrainId.keySet());
//                    trains.sort(String::compareTo);
//                    String headers = String.join(" | ", trains);
//                    logger.info(headers);
//                    List<String> tracks = trains.stream().map((t) -> trackByTrainId.get(t).toString()).collect(Collectors.toList());
//                    logger.info(String.join(" | ", tracks));
//                });



        return builder.build();
    }




    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(STATION_ARG, true, "Station code to track. Determines stream name");
        options.addOption(KAFKA_ARG, true, "Kafka server config");
        options.addOption(STREAM_ARG, true, "Stream name override. Defautls to <lower(station)>.dvr");

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "TrackNumbers", options );

        CommandLineParser parser = new DefaultParser();
        CommandLine cli = parser.parse(options, args);


        Properties props = new Properties();
        String stationCode = cli.getOptionValue(STATION_ARG, "local").toLowerCase();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-dvr-tracknumbers-"+stationCode);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, cli.getOptionValue(KAFKA_ARG, "PLAINTEXT://mac-mini.supersixfour:9092")) ;
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        //props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, jsonSerde.getClass());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final String inTopic = stationCode + ".dvr";
        logger.info("Reading from "+inTopic);
        logger.debug("if you can read this, Debugging is on");

        String stream = cli.getOptionValue(STREAM_ARG, inTopic);

        final Topology topology = new TrackNumbers().getStreamTopology(inTopic);

        logger.info(topology.describe().toString());
        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);

    }
}
