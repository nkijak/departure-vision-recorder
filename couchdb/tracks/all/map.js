function(doc) {
  if (doc.track) {
    var when = new Date(doc.at);
    var ymd = (when.getYear()+1900)+"-"+(when.getMonth()+1)+"-"+when.getDate();
    var h = when.getHours() < 10? "0" + when.getHours(): when.getHours();
    var m = when.getMinutes() < 10? "0" + when.getMinutes(): when.getMinutes();
    emit([doc.dest, ymd + " " + doc.departs_at, doc.train_id], doc);
  }
}
