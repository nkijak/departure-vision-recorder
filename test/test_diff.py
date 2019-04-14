from botocore.stub import Stubber
from diff.handler import handle, DepartureDiff
import boto3

def test_handle_happy_path():
  client = boto3.client('s3')
  stubbed = Stubber(client)
  response = {
      'IsTruncated': False,
      'Contents': [
          {
              'Key': 'string',
              'LastModified': datetime(2015, 1, 1),
              'ETag': 'string',
              'Size': 123,
              'StorageClass': 'STANDARD' | 'REDUCED_REDUNDANCY' | 'GLACIER' | 'STANDARD_IA' | 'ONEZONE_IA' | 'INTELLIGENT_TIERING' | 'DEEP_ARCHIVE',
              'Owner': {
                  'DisplayName': 'string',
                  'ID': 'string'
              }
          },
      ],
      'Name': 'string',
      'Prefix': 'string',
      'Delimiter': 'string',
      'MaxKeys': 123,
      'CommonPrefixes': [
          {
              'Prefix': 'string'
          },
      ],
      'EncodingType': 'url',
      'KeyCount': 123,
      'ContinuationToken': 'string',
      'NextContinuationToken': 'string',
      'StartAfter': 'string'
  }
  expected_params = {
    'Bucket': 'com.kinnack.departure-vision-recorder',
    'Prefix': ''
  }
  stubbed.add_response('list_objects_v2', response, expected_params)

  with stubbed:
    req = open('test/ObjectCreated.json').read()
    response = handle(req, DepartureDiff(client))

  assert response == req
