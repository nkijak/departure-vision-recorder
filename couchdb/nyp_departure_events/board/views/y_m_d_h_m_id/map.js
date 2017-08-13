function(doc) {
  var when = new Date(doc.new? doc.new.at: doc.old.at);
  var train_id = doc.new? doc.new.train_id: doc.old.train_id;
  var id = [when.getYear()+1900, when.getMonth()+1, when.getDate(), when.getHours(), when.getMinutes(), train_id];
  emit(id, doc);
}
