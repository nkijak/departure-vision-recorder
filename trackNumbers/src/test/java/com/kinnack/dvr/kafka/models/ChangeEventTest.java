package com.kinnack.dvr.kafka.models;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

public class ChangeEventTest {
    @Test
    public void testChangesAreSeen() {
        Departure old = new Departure("old-depart", "old-dest", "old-track", "old-line", "old-id", "old-status", "old-at", "old-color");
        Departure now = new Departure("now-depart", "now-dest", "now-track", "now-line", "now-id", "now-status", "now-at", "new-color");
        ChangeEvent actual = ChangeEvent.changed(old, now).get();
        assertEquals("Unexpected context value",
                Set.of("departsAt", "dest", "track", "line", "trainId", "status", "color"),
                new HashSet(Arrays.asList(actual.getContext().split("::"))));
    }
    @Test
    public void noChangesAreShown() {
        Departure old = new Departure("old-depart", "old-dest", "old-track", "old-line", "old-id", "old-status", "old-at", "old-color");
        Departure now = new Departure("old-depart", "old-dest", "old-track", "old-line", "old-id", "old-status", "now-at", "old-color");
        Optional<ChangeEvent> actual = ChangeEvent.changed(old, now);
        assertFalse("No departure info changes should result in no change events", actual.isPresent());
    }

}