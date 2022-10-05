function(doc) {
  if (doc.track) {
    var loc = Math.ceil(doc.track / 2);
    var when = new Date(doc.at);
    var ymd = (when.getYear()+1900)+"-"+(when.getMonth()+1)+"-"+when.getDate();
    var h = when.getHours() < 10? "0" + when.getHours(): when.getHours();
    var m = when.getMinutes() < 10? "0" + when.getMinutes(): when.getMinutes();
    var ampm = when.getHours() < 12? "am" : "pm";
    emit([doc.dest, ampm+" "+doc.departs_at, doc.train_id], loc);
  }
}
