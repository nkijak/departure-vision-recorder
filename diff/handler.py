import json
from datetime import datetime
import os

from .departure_diff import DepartureDiff
from .models import Encoder

def handle(req, departure_diff=None):
    event = json.loads(req)
    action = event['EventName']
    key = event['Key']
    if not departure_diff:
        departure_diff=DepartureDiff()
    changes = departure_diff.diff_last_based_on(key)

    changes_as_json = json.dumps(changes, cls=Encoder)
    # parts=key.split('/')
    # now = datetime.utcnow()
    # departure_diff.store_diff(parts[0], "dv_diffs/{}/{}".format(
    #     now.strftime('%Y/%m/%d/%H'), 
    #     'diff_{}.json'.format(now.isoformat())
    # ), changes_as_json)

    return changes_as_json

