function(doc) {
  if (doc.track) {
    emit(doc.train_id, doc);
  }
}
