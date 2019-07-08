package com.kinnack.dvr.kafka.models;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DepartureVision {
    private List<Departure> departures;
    private String objectKey;

    public DepartureVision(@JsonProperty("departures") List<Departure> departures, @JsonProperty("objectKey") String objectKey) {
        this.departures = departures;
        this.objectKey = objectKey;
    }

    public List<Departure> getDepartures() {
        return departures;
    }
    public String getObjectKey() {
        return objectKey;
    }


    @Override
    public boolean equals(Object obj) {
        try {
            DepartureVision other = (DepartureVision) obj;
            return other.getObjectKey().equals(getObjectKey());
        } catch(ClassCastException cce) {
            return false;
        }
    }
}
