from thespian.actors import *
import lib.page_utils as page
import lib.parse_utils as parse
from lib.repo import Event
import logging

class Message(object):
    UNKNOWN='unknown'
    def __init__(self, msg=UNKNOWN, payload=None):
        self.msg = msg
        self.payload = payload

    def __repr__(self):
        return self.__dict__.__str__()

class Networking(Actor):
    LIST_DEPARTURES='list_departures'

    @staticmethod
    def list_departures(station):
        return Message(Networking.LIST_DEPARTURES, station)

    def receiveMessage(self, message, sender):
        if message.msg == Networking.LIST_DEPARTURES: 
            logging.info("getting departures for " + message.payload)
            print("getting departures for " + message.payload)
            html, timestamp = page.get_dv_page(station=message.payload, skip_cache=True)
            departures = parse.list_departures(html, timestamp)

            self.send(sender, Message(Station.UPDATE, departures))
        else: 
            self.send(sender, Message())

class Dispatcher(Actor):
    UPDATE='update'

    #TODO is there a way to iterate over children?

    def receiveMessage(self, message, sender):
        print("Dispatcher:{}".format(message))
        if message.msg == Dispatcher.UPDATE:
            id = message.payload.train_id
            logging.info("Updating train {}".format(id))
            print("Updating train {}".format(id))
            train = self.createActor(Train, globalName=id)
            self.send(train, message.payload)
        else:
            self.send(sender, Message())

class Station(Actor):
    WATCH='watch'
    UPDATE='update'
    networking = None
    dispatcher = None
    
    def receiveMessage(self, message, sender):
        if message.msg == Station.WATCH:
            self.send(Station.networking, Networking.list_departures(message.payload))
        elif message.msg == Station.UPDATE:
            for departure in message.payload:
                self.send(Station.dispatcher, Message(Dispatcher.UPDATE, departure))
        else:
            self.send(sender, Message())

class Train(ActorTypeDispatcher):
    def __init__(self):
        self.last_departure = None
        self.id = None
        self.color = None
        self.carrier = None
        self.line = None

    def determine_change(old, new):
        changes = []
        for i, k in enumerate(old.__dict__):
            if k == 'at':
                continue
            if new.__dict__.get(k) != old.__dict__.get(k):
                changes.append(k)
        if len(changes) == 0:
            return None
        context = "::".join(changes)
        return Event.changed_departure(context, old, new)

    def receiveMessage(self, departure, sender):
        ''' should only receive Departure messages '''
        #if isinstance(departure, ActorExitMessage):
        #    #TODO how do i know if this was because train is idle or if shutdown?
        #    pass
        #elif not self.last_departure:
        print("{} Last departure? {}".format(self.myAddress, self.last_departure))
        if not self.last_departure:
            self.id = departure['train_id']
            self.color = departure['color']
            self.line = departure['line']
            new = Event.new_departure(departure)
            logging.info("New departure: {}".format(new))
            print("New departure: {}".format(new))
        elif self.last_departure.changed(departure):
            change = determine_change(self.last_departure, departure)
            logging.info("change: {}".format(change))
            print("change: {}".format(change))
        else:
            pass

        #TODO need to publish a Dropped event if no messages in n minutes
        # Event.dropped_

        self.last_departure = departure
        
