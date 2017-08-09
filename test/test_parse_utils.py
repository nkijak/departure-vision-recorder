import unittest
from lib.parse_utils import Departure

class TestDeparture(unittest.TestCase):
    def test_changed(self):
        a = Departure(departs_at="11:00", dest="Trenton", line="NEC", track="", train_id="123")
        b = Departure(departs_at="11:00", dest="Trenton", line="NEC", track="1", train_id="123")
        c = Departure(departs_at="11:05", dest="Trenton", line="NEC", track="", train_id="123")
        self.assertTrue(a.changed(b))
        self.assertTrue(a.changed(c))
        self.assertTrue(b.changed(a))
        self.assertTrue(b.changed(c))
        self.assertTrue(c.changed(a))
        self.assertTrue(c.changed(b))

if __name__=="__main__":
    unittest.main()
