package com.kinnack.dvr.kafka;

import com.kinnack.dvr.kafka.models.ChangeEvent;
import com.kinnack.dvr.kafka.models.Departure;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class DiffProcessor implements Transformer<String, List<Departure>, KeyValue<String, ChangeEvent>> {

    private ProcessorContext context;
    private KeyValueStore<String, List<Departure>> kvStore;
    public final static String DIFF_STATE_STORE = "DIFF_STATE_STORE";
    private final static Logger logger = LoggerFactory.getLogger(DiffProcessor.class);

    @Override
    public void init(ProcessorContext context) {
        this.context = context;

        kvStore = (KeyValueStore<String, List<Departure>>) context.getStateStore(DIFF_STATE_STORE);
    }

    @Override
    public KeyValue<String, ChangeEvent> transform(String key, List<Departure> current) {
        List<Departure> prev = kvStore.get(key);
        if (prev == null) {
            prev = new ArrayList<Departure>();
        }
        Collector<Map.Entry<String, Departure>, ?, Map<String, Departure>> entriesAsMap = Collectors.toMap((e) -> e.getKey(), (e)->e.getValue());
        Map<String, Departure> prevByTrainId = prev.stream().map((d)-> Map.entry(d.getTrainId(), d)).collect(entriesAsMap);
        Map<String, Departure> currByTrainId = current.stream().map((d)-> Map.entry(d.getTrainId(), d)).collect(entriesAsMap);

        Set<String> removed = new HashSet(prevByTrainId.keySet());
        removed.removeAll(currByTrainId.keySet());
        for (String trainId : removed) {
            ChangeEvent event = ChangeEvent.dropped(prevByTrainId.get(trainId));
            logger.debug("Removing "+event);
            context.forward(key, event);
        }

        Set<String> added = new HashSet(currByTrainId.keySet());
        added.removeAll(prevByTrainId.keySet());
        for (String trainId : added) {
            ChangeEvent event = ChangeEvent.added(currByTrainId.get(trainId));
            logger.debug("Adding "+event);
            context.forward(key, event);
        }

        Set<String> remain = new HashSet(currByTrainId.keySet());
        remain.retainAll(prevByTrainId.keySet());
        for (String trainId : remain) {
            ChangeEvent.changed(prevByTrainId.get(trainId), currByTrainId.get(trainId)).ifPresent((event) -> {
                logger.debug("Changing "+event);
                context.forward(key, event);
            });
        }


        kvStore.put(key, current);
        context.commit();
        return null;
    }

    @Override
    public void close() {

    }
}
