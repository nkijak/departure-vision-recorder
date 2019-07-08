import json

def handle(req, couch=None):
    event = json.loads(req)
    print(req)
