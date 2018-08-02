from rx import Observable, Observer
from rx.core import Scheduler

import lib.page_utils as page
import lib.parse_utils as parse
from lib.repo import Repo, Event

import sys
import os

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

def find_offset(old, new):
    for i, dep in enumerate(new):
        if dep.train_id == old[0].train_id:
            return i
    return len(new)

def changes_as_events(old_departures, new_departures):
    added = []
    dropped = []
    changed = []
    for d in new_departures:
        try:
            index = old_departures.index(d)
            old = old_departures[index]
            if old.changed(d):
                changed.append((old, d))
        except ValueError:
            added.append(d)

    for d in old_departures:
        try:
            index = new_departures.index(d)
        except ValueError:
            dropped.append(d)

    events = [Event.new_departure(a) for a in added]
    events += [Event.dropped_departure(d) for d in dropped]
    events += [determine_change(old, new) for old, new in changed]
    return events


class DepartureStorage(Observer):
    def __init__(self):
        self.last_departures = []

    def on_next(self, departures):
        events = changes_as_events(self.last_departures, departures)
        for event in events:
            repo.save_event(event)
        self.last_departures = departures

    def on_completed(self):
        print("Done")

    def on_error(self, error):
        print("Error getting departures: %s" % error)
        sys.exit(1)



def get_departures(station):
    html, timestamp = page.get_dv_page(station=station, skip_cache=True)
    departures = parse.list_departures(html, timestamp)
    return departures

def db_host_port():
    try:
        host = os.environ['DB_HOST']
    except KeyError:
        host = "localhost"
    try:
        port = os.environ['DB_PORT']
    except KeyError:
        port = 5984
    return (host, port) 

if __name__=="__main__":
    db_host, port = db_host_port()
    repo = Repo(db_host, port)
    station = os.environ.get('STATION', 'NY')
    repo.connect(
            db_name="{}_departure_events".format(station.lower()), 
            design_name='nyp_departure_events')

    source = Observable\
        .timer(200, 120000, Scheduler.thread_pool)\
        .map(lambda x, y: get_departures(station=station))\
        .subscribe(DepartureStorage())

    Scheduler.thread_pool.executor.shutdown() 
