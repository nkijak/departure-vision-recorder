import logging

class GraphRepo(object):
    def __init__(self, host, port):
        self.uri = 'bolt://{}:{}'.format(host, port)


    def save_event(self, obj):
        return obj.store(self.db)

