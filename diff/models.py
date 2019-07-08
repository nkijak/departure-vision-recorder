from json import JSONEncoder
from datetime import datetime

class Event(object):#Document):
    def __init__(self, action, context, new, old):
      self.action = action
      self.context = context
      self.new = new
      self.old = old
    #action = TextField()
    #context = TextField()
    #new = DictField(DEPARTURE_RECORD_MAPPING)
    #old = DictField(DEPARTURE_RECORD_MAPPING)

    @staticmethod
    def changed_departure(context, old, new):
        return Event(action = 'changed', context = context, new = new.__dict__, old = old.__dict__)

    @staticmethod
    def dropped_departure(dropped):
        return Event(action = 'dropped', context = '_window', new = None, old = dropped.__dict__)

    @staticmethod
    def new_departure(added):
        return Event(action = 'added', context = '_window', new = added.__dict__, old = None)

class Departure(object):
    def __init__(self, departs_at, dest, track, line, train_id, status=None, at=datetime.today(), color=""):
        self.departs_at = departs_at
        self.dest = dest
        self.track = track
        self.line = line
        self.train_id = train_id
        self.status = status
        self.at = at
        self.color = color

    def __eq__(self, other):
        return self.train_id == other.train_id

    def changed(self, other):
        this = self.__dict__.copy()
        del(this['at'])
        that = other.__dict__.copy()
        del(that['at'])
        return this != that

    def __str__(self):
        return self.__dict__.__str__()


class Encoder(JSONEncoder):
    def default(self, obj):
        if isinstance(obj, datetime):
            return obj.isoformat()

        if isinstance(obj, Departure):
            return obj.__dict__

        if isinstance(obj, Event):
            return obj.__dict__