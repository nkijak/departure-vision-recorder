from lib.parse_utils import Departure 
from lib.tracks import changes_as_events, pipeline

import unittest
from unittest.mock import MagicMock, patch

TRENTON_123=Departure("tre", "11:00", "Trenton", "", "NEC", "123")
BAY_HEAD_345=Departure("tre", "11:10", "Bay Head", "", "whatever", "345")

class TestNypTracks(unittest.TestCase):
    def test_new_departures(self):
        old = [TRENTON_123]
        new = [TRENTON_123, BAY_HEAD_345]
        events = changes_as_events(old, new)
        self.assertEqual(len(events), 1)
        self.assertEqual(events[0].action, "added")
        self.assertEqual(events[0].context, "_window")
        self.assertEqual(events[0].new.train_id, "345")

    def test_dropped_departures(self):
        old = [TRENTON_123, BAY_HEAD_345]
        new = [BAY_HEAD_345]
        events = changes_as_events(old, new)
        self.assertEqual(len(events), 1)
        self.assertEqual(events[0].action, "dropped")
        self.assertEqual(events[0].context, "_window")
        self.assertEqual(events[0].old.train_id, "123")

    def test_track_assignment(self):
        old = [TRENTON_123, BAY_HEAD_345]
        assignment = Departure(**TRENTON_123.__dict__)
        assignment.track = "1"
        new = [assignment, BAY_HEAD_345]
        events = changes_as_events(old, new)
        self.assertEqual(len(events), 1)
        self.assertEqual(events[0].action, "changed")
        self.assertEqual(events[0].context, "track")
        self.assertEqual(events[0].new.track, "1")
        self.assertEqual(events[0].old.track, "")
    
    @patch('urllib.request.urlopen', MagicMock(side_effect=Exception('mock triggered')))
    def test_pipeline(self):
        pipeline()


if __name__ == '__main__':
    unittest.main()

