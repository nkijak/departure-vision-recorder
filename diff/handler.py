import json
from datetime import datetime

from .departure_diff import DepartureDiff

def handle(req, departure_diff=None):
    event = json.loads(req)
    action = event['EventName']
    key = event['Key']
    if not departure_diff:
        departure_diff=DepartureDiff()
    changes = departure_diff.diff_last_based_on(key)

    return req
