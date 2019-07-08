from datetime import datetime, timedelta
import re
import os

import boto3
from bs4 import BeautifulSoup
from .models import Departure, Event


STYLE_REGEX=re.compile('.+background-color:(\w+);')
S3_HOST=os.environ.get('S3_HOST', 'http://192.168.1.211:9000')
S3_ACCESS_ID=os.environ.get('AWS_ACCESS_KEY_ID', 'minio-supersixfour')
S3_SECRET_KEY=os.environ.get('AWS_SECRET_ACCESS_ID', 'roufxisrepus-oinim')


class DepartureDiff(object):
    def __init__(self, client=None):
        self.client=client if client else \
            boto3.client('s3',
                    endpoint_url=S3_HOST,
                    aws_access_key_id=S3_ACCESS_ID,
                    aws_secret_access_key=S3_SECRET_KEY)

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

    def determine_last_since(self, bucket, path):
        org, station, year, month, day, filename = path.split('/')
        when = datetime.strptime(filename[3:-5], '%Y-%m-%dT%H:%M:%S.%f')
        start_search = when - timedelta(hours=1) 
        prefix = '/'.join([org, station, start_search.strftime('%Y/%m/%d')])
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
        return prev['Key']

    
    def diff_last_based_on(self, key):
        parts = key.split('/')
        bucket = parts[0]
        path = '/'.join(parts[1:])
        last_path = self.determine_last_since(bucket, path)
        prev_obj = self.client.get_object(Bucket=bucket, Key=last_path)
        current_obj = self.client.get_object(Bucket=bucket, Key=path)
        
        prev_dep = self.list_departures(prev_obj['Body'].read())
        current_dep = self.list_departures(current_obj['Body'].read())
        changes = self.changes_as_events(prev_dep, current_dep)

        return changes

    def store_diff(self, bucket, key, diff_as_json):
        self.client.put_object(Bucket=bucket, Key=key, Body=diff_as_json.encode('utf-8'))