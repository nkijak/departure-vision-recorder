package com.kinnack.dvr.kafka;

import com.kinnack.dvr.kafka.models.ChangeEvent;
import com.kinnack.dvr.kafka.models.Departure;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

public class DiffProcessorTest {
    final TrackNumbers trackNumbers = new TrackNumbers();
    KeyValueStore<String, List<Departure>> store;
    MockProcessorContext context;
    Transformer<String, List<Departure>, KeyValue<String, ChangeEvent>> transformer;
    private static final String TEST_KEY="test.dvr";

    @Before
    public void setup() {
        transformer = new DiffProcessor();
        final Properties props = new Properties();
        props.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, "test.diff.transformer");
        props.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "");
        context = new MockProcessorContext(props);

        store = Stores.keyValueStoreBuilder(
                Stores.inMemoryKeyValueStore(DiffProcessor.DIFF_STATE_STORE),
                Serdes.String(),
                trackNumbers.listDepartureSerde
        ).withLoggingDisabled()
                .build();
        store.init(context, store);
        store.delete(TEST_KEY);

        context.register(store, null);
        transformer.init(context);
    }

    @Test
    public void testSeesAdds() {
        Departure departure = new Departure();
        departure.setTrainId("trainId");
        departure.setStatus("ontime");
        transformer.transform(TEST_KEY, List.of(departure));

        final Iterator<MockProcessorContext.CapturedForward> forwared = context.forwarded().iterator();
        KeyValue actual = forwared.next().keyValue();
        assertFalse("Should only emit one event", forwared.hasNext());
        assertEquals(actual.key, TEST_KEY);
        assertEquals(actual.value, ChangeEvent.added(departure));
    }

    @Test
    public void testSeesRemovals() {
        Departure departure = new Departure();
        departure.setTrainId("trainId");
        departure.setStatus("ontime");
        store.put(TEST_KEY, List.of(departure));

        transformer.transform(TEST_KEY, new ArrayList<>());

        final Iterator<MockProcessorContext.CapturedForward> forwared = context.forwarded().iterator();
        KeyValue actual = forwared.next().keyValue();
        assertFalse("Should only emit one event", forwared.hasNext());
        assertEquals(actual.key, TEST_KEY);
        assertEquals(actual.value, ChangeEvent.dropped(departure));
    }

    @Test
    public void testSeesChanges() {
        Departure prev = new Departure();
        prev.setTrainId("trainId");
        prev.setStatus("ontime");
        store.put(TEST_KEY, List.of(prev));

        Departure curr = new Departure();
        curr.setTrainId("trainId");
        curr.setStatus("boarding");
        transformer.transform(TEST_KEY, List.of(curr));

        final Iterator<MockProcessorContext.CapturedForward> forwared = context.forwarded().iterator();
        KeyValue actual = forwared.next().keyValue();
        assertFalse("Should only emit one event", forwared.hasNext());
        assertEquals(TEST_KEY, actual.key);
        assertEquals(ChangeEvent.changed(prev, curr).get(), actual.value);
    }
}