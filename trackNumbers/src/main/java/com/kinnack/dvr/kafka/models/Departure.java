package com.kinnack.dvr.kafka.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.Optional;

public class Departure {
    private String departsAt;
    private String dest;
    private String track;
    private String line;
    private String trainId;
    private String status;
    private String at;
    private String color;

    public Departure() { }

    public Departure(String departsAt, String dest, String track, String line, String trainId, String status, String at, String color) {
        this.departsAt = departsAt;
        this.dest = dest;
        this.track = track;
        this.line = line;
        this.trainId = trainId;
        this.status = status;
        this.at = at;
        this.color = color;
    }

    public void setDepartsAt(String departsAt) {
        this.departsAt = departsAt;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public void setTrainId(String trainId) {
        this.trainId = trainId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAt(String at) {
        this.at = at;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDepartsAt() {
        return departsAt;
    }

    public String getDest() {
        return dest;
    }

    public String getTrack() {
        if (track == null || track.isEmpty()) return null;
        return track;
    }

    public String getLine() {
        return line;
    }

    public String getTrainId() {
        return trainId;
    }

    public String getStatus() {
        return status;
    }

    public String getAt() {
        return at;
    }


    public String getColor() {
        return color;
    }


    @Override
    public String toString() {
        return "Departure{" +
                "departsAt='" + departsAt + '\'' +
                ", dest='" + dest + '\'' +
                ", track='" + track + '\'' +
                ", line='" + line + '\'' +
                ", trainId='" + trainId + '\'' +
                ", status='" + status + '\'' +
                ", at='" + at + '\'' +
                ", color='" + color + '\'' +
                '}';
    }

    public static Departure fromJsonNode(JsonNode data) {
        return new Departure(
            Optional.ofNullable(data.get("departs_at")).map(JsonNode::textValue).orElse(null),
            Optional.ofNullable(data.get("dest")).map(JsonNode::textValue).orElse(null),
            Optional.ofNullable(data.get("track")).map(JsonNode::textValue).orElse(null),
            Optional.ofNullable(data.get("line")).map(JsonNode::textValue).orElse(null),
            Optional.ofNullable(data.get("train_id")).map(JsonNode::textValue).orElse(null),
            Optional.ofNullable(data.get("status")).map(JsonNode::textValue).orElse(null),
            Optional.ofNullable(data.get("at")).map(JsonNode::textValue).orElse(null),
            Optional.ofNullable(data.get("color")).map(JsonNode::textValue).orElse(null)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Departure)) return false;
        Departure departure = (Departure) o;
        return Objects.equals(departsAt, departure.departsAt) &&
                Objects.equals(dest, departure.dest) &&
                Objects.equals(track, departure.track) &&
                Objects.equals(line, departure.line) &&
                Objects.equals(trainId, departure.trainId) &&
                Objects.equals(status, departure.status) &&
                Objects.equals(at, departure.at) &&
                Objects.equals(color, departure.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(departsAt, dest, track, line, trainId, status, at, color);
    }
}
