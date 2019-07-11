package com.kinnack.dvr.kafka.models;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TrackStats {
    private HashMap<String, Train> _stats;

    public TrackStats() {
        _stats = new HashMap<>();
    }

    public void track(String trainId, String track) {
        Train train = _stats.getOrDefault(trainId, new Train(trainId));
        train.tick(track);
        _stats.put(trainId, train);
    }

    public HashMap<String, Train> getStats() {
        return _stats;
    }

    public void setStats(HashMap<String, Train> stats) {
        this._stats = stats;
    }

     public static class Train {
        private String id;
        private HashMap<String, Integer> counts;
        public Train(String id) {
            this.id = id;
            this.counts = new HashMap<>();
        }

        public void tick(String track) {
            // TODO this isn't thread safe. does kafka stream app give some safety?
            int count = counts.getOrDefault(track, 0);
            count += 1;
            counts.put(track, count);
        }

        public HashMap<String, Integer> getCounts() {
            return counts;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setCounts(HashMap<String, Integer> counts) {
            this.counts = counts;
        }
    }
}
