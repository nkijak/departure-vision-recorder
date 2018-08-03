from rx import Observable, Observer

import sys
import os
import json

import lib.page_utils as page
import lib.parse_utils as parse
from lib.repo import Repo, Event
from nypTracks import determine_change, changes_as_events

class DepartureStorage(Observer):
    def __init__(self):
        self.last_departures = []

    def on_next(self, departures):
        events = changes_as_events(self.last_departures, departures)
        for event in events:
            print(event.__dict__)
            #print(repo.save_event(event))
        self.last_departures = departures

    def on_completed(self):
        True

    def on_error(self, error):
        print("Error getting departures: %s" % error)


if __name__=="__main__":
    repo = Repo("localhost", 5984)
    repo.connect("nyp_departure_testing", design_name='nyp_departure_events')

    f = os.listdir("dv_data")
    f.sort()
    files = Observable.from_(f)
    files\
        .map(lambda f: (f, open('dv_data/'+f).read()))\
        .map(lambda pair: parse.list_departures(pair[1], page.extract_timestamp(pair[0])))\
        .subscribe(DepartureStorage())

