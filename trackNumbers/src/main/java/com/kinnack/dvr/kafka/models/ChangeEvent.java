package com.kinnack.dvr.kafka.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.kafka.streams.kstream.internals.Change;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChangeEvent {
    public static final String ADDED = "added";
    public static final String DROPPED = "dropped";
    public static final String WINDOW = "_window";
    private String action;
    private String context;
    private Departure was;
    private Departure now;

    public ChangeEvent(
            @JsonProperty("action") String action,
            @JsonProperty("context") String context,
            @JsonProperty("was") Departure was,
            @JsonProperty("now") Departure now) {
        this.action = action;
        this.context = context;
        this.was = was;
        this.now = now;
    }

    public String getAction() {
        return action;
    }

    public String getContext() {
        return context;
    }

    public Departure getWas() {
        return was;
    }

    public Departure getNow() {
        return now;
    }


    public static ChangeEvent added(Departure departure) {
        return new ChangeEvent(ADDED, WINDOW, null, departure);
    }

    public static ChangeEvent dropped(Departure departure) {
        return new ChangeEvent(DROPPED, WINDOW, departure, null);
    }

    public static Optional<ChangeEvent> changed(Departure was, Departure now) {
        Map<String, Optional<String>> oldMap = DepartureOps.toMap(was);
        Map<String, Optional<String>> newMap = DepartureOps.toMap(now);

        String context = String.join("::", oldMap.entrySet().stream().reduce(new ArrayList<String>(), (accum, next) -> {
            if (!next.getValue().equals(newMap.get(next.getKey())))  {
                accum.add(next.getKey());
            }
            return accum;
        }, (a,b) -> {
            a.addAll(b);
            return a;
        }));

        if (context.isEmpty()) return Optional.<ChangeEvent>empty();
        return Optional.of(new ChangeEvent("changed", context, was, now));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChangeEvent)) return false;
        ChangeEvent that = (ChangeEvent) o;
        return action.equals(that.action) &&
                context.equals(that.context) &&
                Objects.equals(was, that.was) &&
                Objects.equals(now, that.now);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, context, was, now);
    }

    @Override
    public String toString() {
        return "ChangeEvent{" +
                "action='" + action + '\'' +
                ", context='" + context + '\'' +
                ", was=" + was +
                ", now=" + now +
                '}';
    }

    public static class ChangeEventOps {

        public static boolean isDropped(ChangeEvent event) {
            return event.action.equals(DROPPED);
        }
        public static boolean isAdded(ChangeEvent event) {
            return event.action.equals(ADDED);
        }
        public static boolean isCancelled(ChangeEvent event) {
            return Optional.ofNullable(event.now).map(DepartureOps::isCancelled).orElse(false) ||
                   Optional.ofNullable(event.was).map(DepartureOps::isCancelled).orElse(false);

        }
        public static String getTrainId(ChangeEvent event) {
            if (event.was != null) return event.was.getTrainId();
            return event.now.getTrainId();
        }

    }

    private static class DepartureOps {

        public static Map<String, Optional<String>> toMap(Departure d) {
            return Map.of(
                    "departsAt", Optional.ofNullable(d.getDepartsAt()),
                    "dest", Optional.ofNullable(d.getDest()),
                    "track", Optional.ofNullable(d.getTrack()),
                    "line", Optional.ofNullable(d.getLine()),
                    "trainId", Optional.ofNullable(d.getTrainId()),
                    "status", Optional.ofNullable(d.getStatus()),
                    "color", Optional.ofNullable(d.getColor())
            );
        }

        public static boolean isCancelled(Departure d) {
            return d.getStatus().toLowerCase().indexOf("cancel") > -1;
        }
    }
}
