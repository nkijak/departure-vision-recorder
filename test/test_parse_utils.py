import unittest
from lib.parse_utils import Departure, list_departures
from datetime import datetime

class TestDeparture(unittest.TestCase):
    def test_changed(self):
        a = Departure(station='test', departs_at="11:00", dest="Trenton", line="NEC", track="", train_id="123")
        b = Departure(station='test', departs_at="11:00", dest="Trenton", line="NEC", track="1", train_id="123")
        c = Departure(station='test', departs_at="11:05", dest="Trenton", line="NEC", track="", train_id="123")
        self.assertTrue(a.changed(b))
        self.assertTrue(a.changed(c))
        self.assertTrue(b.changed(a))
        self.assertTrue(b.changed(c))
        self.assertTrue(c.changed(a))
        self.assertTrue(c.changed(b))

    def test_at_changes_not_different(self):
        a = Departure(station='test', departs_at="11:00", dest="Trenton", line="NEC", track="", train_id="123", at=datetime(1970, 1, 1, 0))
        b = Departure(station='test', departs_at="11:00", dest="Trenton", line="NEC", track="", train_id="123")
        self.assertFalse(a.changed(b))
        self.assertFalse(b.changed(a))

    def test_station_changes_not_different(self):
        a = Departure(station='a', departs_at="11:00", dest="Trenton", line="NEC", track="", train_id="123")
        b = Departure(station='b', departs_at="11:05", dest="Trenton", line="NEC", track="", train_id="123")
        self.assertFalse(a.changed(b))
        self.assertFalse(b.changed(a))

    def test_list_departures(self):
        expected = [
            Departure(station='for_test', departs_at='12:29', dest='MSU -SEC', line='Montclair-Boonton', track='', train_id='6241'),
            Departure(station='for_test', departs_at='12:37', dest='Trenton -SEC', line='Northeast Corrdr', track='', train_id='3843'),
            Departure(station='for_test', departs_at='12:43', dest='Long Branch-BH -SEC', line='No Jersey Coast', track='', train_id='3243'),
        ]
        with open('./test/resources/dv_board.html') as html:
            actual = list_departures(html, at=datetime(2000, 1, 2), station='for_test')
        assert expected == actual

if __name__ == "__main__":
    unittest.main()
