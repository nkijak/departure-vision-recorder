import logging
import couchdb
from couchdb.loader import load_design_doc
from couchdb.design import ViewDefinition

DB_NAME="nyp_departures"
DESIGN_ROOT="couchdb"

class Repo(object):
    def __init__(self, host, port):
        self.host = host
        self.port = port

    def connect(self, db_name=DB_NAME, design_root=DESIGN_ROOT, design_name=None):
        self.couch = couchdb.Server("http://%s:%d" % (self.host, self.port))
        self.db_name = db_name
        self.db = self.__get_db(db_name)
        self.sync_designs(design_root, design_name)

    def sync_designs(self, design_path, design_name=None):
        design_name = design_name or self.db_name
        logging.info("syncing design docs from %s/%s" % (design_path, design_name))
        try:
            designs = load_design_doc(design_path+"/"+design_name, strip=True)    
            view_defs = []
            logging.debug("Found designs: %s" % designs.keys())
            for name in designs:
                design = designs[name]
                views = design['views']
                for v_name in views:
                    view = views[v_name]
                    vd = ViewDefinition(
                            name, 
                            v_name, 
                            view['map'], 
                            view.get('reduce'))
                    logging.debug("Found view def ", vd)
                    view_defs.append(vd)
            return ViewDefinition.sync_many(self.db, view_defs)

        except OSError as ose:
            logging.error("Error syncing: %s" % ose)
        return None


    def __get_db(self, name):
        try:
            return self.couch[name]
        except couchdb.http.ResourceNotFound:
            return self.__init_db(name)
        except couchdb.http.ServerError as se:
            status, _ = se.args[0]
            if status == 400:
                return self.__init_db(name)
            raise se

    def __init_db(self, name):
        db = self.couch.create(name)
        return db

