import urllib.request
import os
from datetime import datetime
import dateutil.parser as dup

import boto3

STATIONS={
    'TRE': 'TR',
    'NYP': 'NY',
}
DV_BASE="http://dv.njtransit.com/mobile/tid-mobile.aspx?sid="
NYP="NY"
CACHE_PATH="dv_data"
BUCKET="com.kinnack.departure-vision-recorder"
S3_HOST=os.environ.get('S3_HOST', 'http://192.168.1.211:9000')

def handle(req):
    if req:
        req = req.strip()
    req = req if req else NYP
    print("handling request for >{}<".format(req))
    html, when, where = get_dv_page(req) 
    return where 

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
    latest = cache_dv_page(html, station=station, path=cache_path)
    return (html, datetime.today(), latest)

def extract_timestamp(filename):
    _, station, iso = filename.split("_")
    iso = iso.split(".")[0]
    return dup.parse(iso)


