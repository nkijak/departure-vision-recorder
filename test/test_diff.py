from datetime import datetime
from botocore.stub import Stubber
from diff.handler import handle, DepartureDiff
from diff.models import Event, Departure
import json
import boto3

class DummyDepartureDiff(object):
    def diff_last_based_on(self, key):
        return [
            Event.new_departure(Departure('2019-04-14T00:00:00.000000', 'Trenton', '1', 'NEC', '3456', None, datetime(2019, 4, 14)))
        ]

def test_handle_returns_json():
    response = handle(json.dumps({"EventName": "wut","Key": ""}), DummyDepartureDiff())
    assert json.dumps([
        {"action": "added", 
        "context": "_window", 
        "new": {
            "departs_at": "2019-04-14T00:00:00.000000", 
            "dest": "Trenton", 
            "track": "1", 
            "line": "NEC", 
            "train_id": "3456", 
            "status": None, 
            "at": "2019-04-14T00:00:00", 
            "color": ""
        }, 
        "old": None
    }])  == response

# def test_handle_happy_path():
#   client = boto3.client('s3')
#   stubbed = Stubber(client)
#   response = {
#       'IsTruncated': False,
#       'Contents': [
#           {
#               'Key': 'string',
#               'LastModified': datetime(2015, 1, 1),
#               'ETag': 'string',
#               'Size': 123,
#               'StorageClass': 'STANDARD',
#               'Owner': {
#                   'DisplayName': 'string',
#                   'ID': 'string'
#               }
#           },
#       ],
#       'Name': 'string',
#       'Prefix': 'string',
#       'Delimiter': 'string',
#       'MaxKeys': 123,
#       'CommonPrefixes': [
#           {
#               'Prefix': 'string'
#           },
#       ],
#       'EncodingType': 'url',
#       'KeyCount': 123,
#       'ContinuationToken': 'string',
#       'NextContinuationToken': 'string',
#       'StartAfter': 'string'
#   }
#   expected_params = {
#     'Bucket': 'com.kinnack.departure-vision-recorder',
#     'Prefix': ''
#   }
#   stubbed.add_response('list_objects_v2', response, expected_params)

#   with stubbed:
#     req = open('test/ObjectCreated.json').read()
#     response = handle(req, DepartureDiff(client))

#   assert response == req
