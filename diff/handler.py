import json
import boto3


from bs4 import BeautifulSoup

from datetime import datetime
#import lib.page_utils as page

from json import JSONEncoder

import re

STYLE_REGEX=re.compile('.+background-color:(\w+);')

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

class Departure(JSONEncoder):
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

def json_serializer(obj):
    if isinstance(obj, datetime):
        return obj.isoformat()

    if isinstance(obj, Departure):
        return obj.__dict__

class DepartureDiff(object):
    def __init__(self, client=None):
        self.client=client if client else \
            boto3.client('s3',
                    endpoint_url='http://192.168.1.211:9000',
                    aws_access_key_id='minio-supersixfour',
                    aws_secret_access_key='roufxisrepus-oinim')

    def list_departures(self, html, at=datetime.today()):
        """The web scraping function.  When things go wrong, look here.

        Args:
            html (str): html string to parse
            at (datetime): time html was pulled/generated

        Returns:
            array of Departure instances from the parse html

        """
        soup = BeautifulSoup(html, 'html.parser')
        trs = soup(attrs = {'class': 'table-row'})
        departures = []
        for tr in trs:
            style = tr.find('tr')['style']
            color = self.__extract_row_color__(style)
            # get the values in the table. 
            a = [td.text.strip() for td in tr('td')]
            dep = Departure(*a)
            dep.at = at
            dep.color = color
            departures.append(dep)
        return departures

    def __extract_row_color__(self, style):
        match = STYLE_REGEX.match(style)
        if match:
            return match.groups()[0]
        else:
            return ""

            
    def determine_change(self, old, new):
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

    def changes_as_events(self, old_departures, new_departures):
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
        events += [self.determine_change(old, new) for old, new in changed]
        return events
    
    def diff_last_based_on(self, key):
        parts = key.split('/')
        bucket = parts[0]
        path = '/'.join(parts[1:])
        prefix = '/'.join(parts[1:-2])
        print("Search prefix {}".format(prefix))
        objects = self.client.list_objects_v2(
                Bucket=bucket,
                Prefix=prefix)
        i = 0
        for obj in objects['Contents']:
            if obj['Key'] == path:
                break
            i += 1
        prev = objects['Contents'][i - 1]
        print("compairing {} to {}".format(path, prev['Key']))
        prev_obj = self.client.get_object(Bucket=bucket, Key=prev['Key'])
        current_obj = self.client.get_object(Bucket=bucket, Key=path)
        
        prev_dep = self.list_departures(prev_obj['Body'].read())
        current_dep = self.list_departures(current_obj['Body'].read())
        changes = self.changes_as_events(prev_dep, current_dep)

        return changes



def handle(req, departure_diff=None):
    event = json.loads(req)
    action = event['EventName']
    key = event['Key']
    if not departure_diff:
        departure_diff=DepartureDiff()
    changes = departure_diff.diff_last_based_on(key)

    return req
