package com.kinnack.dvr.kafka.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChangeEvent {
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
        return new ChangeEvent("added", "_window", null, departure);
    }

    public static ChangeEvent dropped(Departure departure) {
        return new ChangeEvent("dropped", "_window", departure, null);
    }

    public static Optional<ChangeEvent> changed(Departure was, Departure now) {
        Map<String, Optional<String>> oldMap = new DepartureOps(was).toMap();
        Map<String, Optional<String>> newMap = new DepartureOps(now).toMap();

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

    private static class DepartureOps {
        private Departure departure;

        public DepartureOps(Departure departure) {
            this.departure = departure;
        }

        public Map<String, Optional<String>> toMap() {
            return Map.of(
                    "departsAt", Optional.ofNullable(departure.getDepartsAt()),
                    "dest", Optional.ofNullable(departure.getDest()),
                    "track", Optional.ofNullable(departure.getTrack()),
                    "line", Optional.ofNullable(departure.getLine()),
                    "trainId", Optional.ofNullable(departure.getTrainId()),
                    "status", Optional.ofNullable(departure.getStatus()),
                    "color", Optional.ofNullable(departure.getColor())
            );
        }
    }
}
