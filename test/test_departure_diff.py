import json
from diff.models import Departure
from diff.departure_diff import DepartureDiff
import pytest
import boto3
from botocore.stub import Stubber

TRENTON_123=Departure("11:00", "Trenton", "", "NEC", "123")
BAY_HEAD_345=Departure("11:10", "Bay Head", "", "whatever", "345")

@pytest.fixture
def dd(request):
    return DepartureDiff()

def test_new_departures(dd):
    old = [TRENTON_123]
    new = [TRENTON_123, BAY_HEAD_345]
    events = dd.changes_as_events(old, new)
    assert len(events) == 1
    assert events[0].action == "added"
    assert events[0].context == "_window"
    assert events[0].new['train_id'] == "345"

def test_dropped_departures(dd):
    old = [TRENTON_123, BAY_HEAD_345]
    new = [BAY_HEAD_345]
    events = dd.changes_as_events(old, new)
    assert len(events) == 1
    assert events[0].action == "dropped"
    assert events[0].context == "_window"
    assert events[0].old['train_id'] == "123"

def test_track_assignment(dd):
    old = [TRENTON_123, BAY_HEAD_345]
    assignment = Departure(**TRENTON_123.__dict__)
    assignment.track = "1"
    new = [assignment, BAY_HEAD_345]
    events = dd.changes_as_events(old, new)
    assert len(events) == 1
    assert events[0].action == "changed"
    assert events[0].context == "track"
    assert events[0].new['track'] == "1"
    assert events[0].old['track'] == ""

@pytest.mark.parametrize('expected, key', [
    ('dv_data/TR/2019/04/09/dv_2019-04-09T23:58:10.010010.html','dv_data/TR/2019/04/10/dv_2019-04-10T00:00:10.765225.html'), # day border
    ('dv_data/TR/2019/03/31/dv_2019-03-31T23:58:10.010010.html','dv_data/TR/2019/04/01/dv_2019-04-01T00:00:10.765225.html'), # month
    ('dv_data/TR/2018/12/31/dv_2018-12-31T23:58:10.010010.html','dv_data/TR/2019/01/01/dv_2019-01-01T00:00:10.765225.html'), # year
])
def test_determine_last_since_on_day_border(expected, key):
    client = boto3.client('s3')
    stub = Stubber(client)
    bucket = 'com.kinnack.departure-vision-recorder' 
    prefix = expected[:-35]
    expected_list_req = {
        'Bucket': bucket,
        'Prefix': prefix
    }
    list_response = {
        'Contents': [
            {'Key': expected},
            {'Key': key}
        ]
        expected = {}
    }
    stub.add_response('list_objects_v2', list_response, expected_list_req)
    expected_get_req = {
        'Bucket': 'com.kinnack.departure-vision-recorder',
        'Key': list_response['Contents'][0]['Key']
    }
    with stub:
        dd = DepartureDiff(client)
        actual = dd.determine_last_since(bucket, key)
        assert expected == actual 

# def test_last_based_on_json():
#     client = boto3.client('s3')
#     stub = Stubber(client)
#     bucket = 'com.kinnack.departure-vision-recorder' 
#     key = 'dv_data/NY/2017/08/05/dv_2017-08-05T16:32:05.960042.html'
#     dd = DepartureDiff(client)
#     expected = {}
#     stub.add_response('')
#     with stub:
#         changes_json = dd.diff_last_based_on("{}/{}".format(bucket, key))
#         actual = json.loads(changes_json)
#         assert expected == actual