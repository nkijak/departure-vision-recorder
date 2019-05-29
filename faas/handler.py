import urllib.request
import os
from datetime import datetime
import time
import dateutil.parser as dup
from bs4 import BeautifulSoup
from json import JSONEncoder
import json

import re

import boto3

STATIONS={
    'TR': 'tre',
    'NY': 'nyp',
}
DV_BASE="http://dv.njtransit.com/mobile/tid-mobile.aspx?sid="
NYP="NY"
CACHE_PATH="dv_data"
BUCKET="com.kinnack.departure-vision-recorder"
S3_HOST=os.environ.get('S3_HOST', 'http://192.168.1.211:9000')

STYLE_REGEX=re.compile('.+background-color:(\w+);')

class DepartureEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, Departure):
            return obj.__dict__
        elif isinstance(obj, datetime):
            return obj.isoformat()
        else:
            return json.JSONEncoder.default(self, obj)

class Departure():
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
    
    def __repr__(self):
        self.__str__()

def json_serializer(obj):
    if isinstance(obj, datetime):
        return obj.isoformat()

    if isinstance(obj, Departure):
        return obj.__dict__

def handle(req):
    if req:
        req = req.strip()
    req = req if req else NYP
    event = get_dv_page(req) 
    data = json.dumps(event, cls=DepartureEncoder) 
    print(data)
    r = urllib.request.Request('http://gateway:8080/function/kafka-proxy/topic/{}.dvr'.format(req.lower()), headers={'Content-Type': 'application/json'}, data=data.encode('utf-8'))
    with urllib.request.urlopen(r) as post:
        print(post.read().decode('utf-8'))
    return data

def fetch_dv_page(station=NYP):
    url = DV_BASE + station
    with urllib.request.urlopen(url) as response:
        html = response.read()
    return html

def cache_dv_page(page, station=NYP, path=CACHE_PATH, bucketname=BUCKET):
    s3 = boto3.resource('s3',
      endpoint_url=S3_HOST
    )
    bucket = s3.Bucket(bucketname)
    now = datetime.today()
    name = "{}/{}/{}/dv_{}.html".format(path, station, now.strftime('%Y/%m/%d'), now.isoformat())
    bucket.put_object(Key=name, Body=page)
    return os.path.join(bucketname, name)


def list_departures(html, at=datetime.today()):
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
        color = __extract_row_color__(style)
        # get the values in the table. 
        a = [td.text.strip() for td in tr('td')]
        dep = Departure(*a)
        dep.at = at
        dep.color = color
        departures.append(dep)
    return departures

def __extract_row_color__(style):
    match = STYLE_REGEX.match(style)
    if match:
        return match.groups()[0]
    else:
        return ""

def get_dv_page(station=NYP, cache_path=CACHE_PATH, skip_cache=False):
    """Fetches the latest cached file or pulls from the web

    Args:
        cache_path (str): path for cache directory
        skip_cache (bool): just get from the web

    Returns:
        tuple(str, datetime): Tuple of the html contents as a string and the datetime of the fetch
    """
    # if not skip_cache and os.path.exists(cache_path):
    #     caches = os.listdir(cache_path)
    #     try:
    #         caches.sort(reverse=True) 
    #         name = caches[0]
    #         latest = os.path.join(cache_path, name)
    #         with open(latest, 'r') as f:
    #             print("reading %s" % latest)
    #             return (f.read(), extract_timestamp(name))
    #     except IndexError:
    #         pass

    html = fetch_dv_page(station)
    when = datetime.utcnow()
    latest = cache_dv_page(html, station=station, path=cache_path)
    departures = list_departures(html, at=when)

    return {
        'headers': {
            'id': "{}{}".format(station, time.mktime(when.utctimetuple())),
            'eventTimestamp': when.isoformat()+'Z',
            'action': 'UPSERT',
            'type': 'dvr.departures'
        },
        'body':{
            'departures': departures,
            'objectKey': latest
        }    
    }

def extract_timestamp(filename):
    _, station, iso = filename.split("_")
    iso = iso.split(".")[0]
    return dup.parse(iso)

if __name__ == '__main__':
    import sys 

    station = NYP
    if len(sys.argv) > 1:
        station = sys.argv[1]
    result = handle(req=station)
    print(result)