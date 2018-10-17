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
        print("telling station to watch NY")
        print(actorSystem.ask(station, Message(Station.WATCH, 'NY')))
        time.sleep(30)
        print("telling station to watch NY")
        print(actorSystem.ask(station, Message(Station.WATCH, 'NY')))
    finally:
        print("shutting down")
        actorSystem.shutdown()
