package com.kinnack.dvr.kafka.utils;

import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.ConsumerRecordFactory;

import java.util.Properties;

public class KafkaTestDriver implements AutoCloseable {
    private Topology topology;
    private TopologyTestDriver testDriver;
    private Properties props = new Properties();

    public KafkaTestDriver() {
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
    }

    @Override
    public void close() {
        testDriver = new TopologyTestDriver(topology, props);
    }

    public void setTopology(Topology topology) {
        this.topology = topology;
    }
}

