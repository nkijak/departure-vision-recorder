import urllib.request
import os
from datetime import datetime
import dateutil.parser as dup

STATIONS={
    'TRE': 'TR',
    'NYP': 'NY',
}
DV_BASE="http://dv.njtransit.com/mobile/tid-mobile.aspx?sid="
NYP="NY"
CACHE_PATH="dv_data"


def fetch_dv_page(station=NYP):
    url = DV_BASE + station
    with urllib.request.urlopen(url) as response:
        html = response.read()
    return html

def cache_dv_page(page, station=NYP, path=CACHE_PATH):
    now = datetime.today()
    if not os.path.isdir(path):
        os.mkdir(path)
    name = "dv_%s_%s.html" % (station, now.isoformat())
    with open(os.path.join(path, name), 'wb') as f:
        f.write(page)
    return os.path.join(path, name)

def get_dv_page(station=NYP, cache_path=CACHE_PATH, skip_cache=False):
    """Fetches the latest cached file or pulls from the web

    Args:
        cache_path (str): path for cache directory
        skip_cache (bool): just get from the web

    Returns:
        tuple(str, datetime): Tuple of the html contents as a string and the datetime of the fetch
    """
    if not skip_cache and os.path.exists(cache_path):
        caches = os.listdir(cache_path)
        try:
            caches.sort(reverse=True) 
            name = caches[0]
            latest = os.path.join(cache_path, name)
            with open(latest, 'r') as f:
                print("reading %s" % latest)
                return (f.read(), extract_timestamp(name))
        except IndexError:
            pass

    html = fetch_dv_page(station)
    latest = cache_dv_page(html, station=station, path=cache_path)
    return (html, datetime.today())

def extract_timestamp(filename):
    _, station, iso = filename.split("_")
    iso = iso.split(".")[0]
    return dup.parse(iso)


