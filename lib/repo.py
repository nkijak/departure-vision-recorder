import couchdb
from couchdb.loader import load_design_doc
from couchdb.mapping import Document, TextField, IntegerField, DateTimeField

from lib.parse_utils import Departure 

DB_NAME="nyp_departures"

class Repo(object):
    def __init__(self, host, port):
        self.host = host
        self.port = port

    def connect(self):
        self.couch = couchdb.Server("http://%s:%d" % (self.host, self.port))
        self.db = self.__get_db()

    def save_departure(self, obj):
        return DepartureRecord.from_departure(obj).store(self.db)


    def __get_db(self, name=DB_NAME):
        try:
            return self.couch[name]
        except couchdb.http.ResourceNotFound:
            return self.__init_db(name)

    def __init_db(self, name):
        db = self.couch.create(name)
        tracks_design = load_design_doc('couchdb/tracks')
        # TODO figure out how to get design doc loaded
        return db

class DepartureRecord(Document):
    departs_at = TextField()
    dest = TextField()
    track = TextField()
    line = TextField()
    train_id = TextField()
    status = TextField()
    at = DateTimeField()
    color = TextField()

    @staticmethod
    def from_departure(d):
        print(d.__dict__)
        return DepartureRecord(
                departs_at = d.departs_at,
                dest = d.dest,
                track = d.track,
                line = d.line,
                train_id = d.train_id,
                status = d.status,
                at = d.at,
                color = d.color)
