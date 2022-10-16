from rx.core import Scheduler
from lib.repo import Repo
from lib import tracks

import sys
import os

def db_host_port():
    host = os.environ.get('DB_HOST', "localhost")
    port = os.environ.get('DB_PORT', "5984")
    print(f"db at {host}:{port}")
    return (host, port) 

if __name__=="__main__":
    db_host, port = db_host_port()
    repo = Repo(db_host, port)
    station = os.environ.get('STATION', 'NY')
    repo.connect(
            db_name="{}_departure_events".format(station.lower()), 
            design_name='nyp_departure_events')

    source = tracks.pipeline(Scheduler.thread_pool) 

    Scheduler.thread_pool.executor.shutdown() 
