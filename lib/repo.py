import couchdb
from couchdb.loader import load_design_doc
from couchdb.mapping import Document, TextField, IntegerField, DateTimeField, DictField, Mapping

from lib.parse_utils import Departure 

DB_NAME="nyp_departures"

class Repo(object):
    def __init__(self, host, port):
        self.host = host
        self.port = port

    def connect(self, db_name=DB_NAME):
        self.couch = couchdb.Server("http://%s:%d" % (self.host, self.port))
        self.db = self.__get_db(db_name)

    def save_departure(self, obj):
        return DepartureRecord.from_departure(obj).store(self.db)

    def save_event(self, obj):
        return obj.store(self.db)


    def __get_db(self, name):
        try:
            return self.couch[name]
        except couchdb.http.ResourceNotFound:
            return self.__init_db(name)

    def __init_db(self, name):
        db = self.couch.create(name)
        tracks_design = load_design_doc('couchdb/%s/tracks' % name)
        # TODO figure out how to get design doc loaded
        return db

DEPARTURE_RECORD_MAPPING = Mapping.build(
    departs_at = TextField(),
    dest = TextField(),
    track = TextField(),
    line = TextField(),
    train_id = TextField(),
    status = TextField(),
    at = DateTimeField(),
    color = TextField()
)

class Event(Document):
    action = TextField()
    context = TextField()
    new = DictField(DEPARTURE_RECORD_MAPPING)
    old = DictField(DEPARTURE_RECORD_MAPPING)

    @staticmethod
    def changed_departure(context, old, new):
        return Event(action = 'changed', context = context, new = new.__dict__, old = old.__dict__)

    @staticmethod
    def dropped_departure(dropped):
        return Event(action = 'dropped', context = '_window', new = None, old = dropped.__dict__)

    @staticmethod
    def new_departure(added):
        return Event(action = 'added', context = '_window', new = added.__dict__, old = None)

class DepartureRecord(Document):
    departs_at = TextField()
    dest = TextField()
    track = TextField()
    line = TextField()
    train_id = TextField()
    status = TextField()
    at = DateTimeField()
    color = TextField()

    def __eq__(self, other):
        return self.__dict__ == other.__dict__

    def __hash__(self):
        return self.__dict__.__hash__()

    def __str__(self):
        return self.__dict__.__str__()

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
