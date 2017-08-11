from bs4 import BeautifulSoup

from datetime import datetime
import lib.page_utils as page

from json import JSONEncoder

import re

STYLE_REGEX=re.compile('.+background-color:(\w+);')

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
