from rx import Observable, Observer

import lib.page_utils as page
import lib.parse_utils as parse
from lib.repo import Repo

class DepartureStorage(Observer):
    def on_next(self, value):
        html, timestamp = page.get_dv_page(skip_cache=True)
        departures = parse.list_departures(html, timestamp)
        for dep in departures:
            print(repo.save_departure(dep))

    def on_completed(self):
        print("Done")

    def on_error(self, error):
        print("Error getting departures: %s" % error)


repo = Repo("localhost", 5984)
repo.connect()

source = Observable.timer(200, 120000)

source.subscribe(DepartureStorage())

input("Press any key to quit\n")
