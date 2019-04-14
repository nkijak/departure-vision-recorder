import json
import os
from .repo import Repo 


def handle(req, couch=None):
    if not couch:
        host = os.environ.get('DB_HOST', '192.168.1.211')
        port = int(os.environ.get('DB_PORT', '5984'))
        couch = Repo(host, port) 
    couch.connect(design_root="/home/app/function/couchdb") #TODO figure out how to do this correctly with relative roots later