function(doc) {
  if (doc.action == "changed") {
    var contexts = doc.context.split("::");
	
    for(i = 0; i < contexts.length; i++){
      var context = contexts[i];
      if (context != "track") continue;
      emit([doc.new.dest, doc.new.train_id, context], {
        was: doc.old[context],
        isNow: doc.new[context],
        train: doc.new.train_id
      });
    }
  }
}
