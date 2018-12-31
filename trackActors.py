from thespian.actors import ActorSystem
from lib.actors import Networking, Dispatcher, Station, Message
import time

if __name__ == '__main__':
    #actorSystem = ActorSystem('multiprocTCPBase')
    actorSystem = ActorSystem('simpleSystemBase')
    try:
        print("starting...")
        Station.networking = actorSystem.createActor(Networking)
        Station.dispatcher = actorSystem.createActor(Dispatcher)
        station = actorSystem.createActor(Station)
        # FIXME this doesn't work because each train gets updated by adjoining stations. So trains are constantly changing
        # FIXME needs to be like the RxPy version where each station updates its own tables
        # FIXME this changes the model of a `Train` tracking itself
        # Updated parse_utils.Departure to store the station and only note change on same station but
        #   trains will only keep the first station they've ever seen and not update after departure
        for i in range(0,10):
            for code in ['NY','ND', 'PJ', 'TR']:
                print("telling station to watch {}".format(code))
                print(actorSystem.ask(station, Message(Station.WATCH, code)))
                time.sleep(4)
            time.sleep(30)
    finally:
        print("shutting down")
        actorSystem.shutdown()
