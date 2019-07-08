const { diff } = require('deep-object-diff');

function determineChange(old, now, delta) {
  const changes = Object.keys(delta);
  const context = changes.join("::")
  return {action: 'changed', context: context, 'new': now, old: old};
};


module.exports = {
  kvDepartureTrack(departure) {
    const id = departure.train_id
    const at = new Date(departure.at);
    var month = at.getMonth()+1;
    if (month < 10) month = `0${month}`; 
    var day = at.getDate();
    if (day < 10) day = `0${day}`;
    return [`${at.getFullYear()}${month}${day}-${id}`, departure.track]; 
  },

  kvDeltaStateByTrainId(changeEvent) {
    if (changeEvent.action == 'dropped')
      return [changeEvent.old.train_id, null];

    return [changeEvent.new.train_id, changeEvent.new];
  },

  changesAsEvents(oldDepartures, newDepartures) {
      const added = [];
      const changed = [];
        
      newDepartures.forEach( d => {
        let found = oldDepartures.find( o => o.train_id == d.train_id );
        if (found) {
          let old = Object.assign({}, found); // copy
          el = Object.assign({}, d); //copy
          delete old.at;
          delete el.at;
          let difference = diff(old, el);
          if (Object.keys(difference).length > 0) {
            changed.push({old: found, now: d, diff: difference});
          }
        } 
        else added.push(d);
      });
      const dropped = oldDepartures.map( d => {
        if (!newDepartures.find( n => n.train_id == d.train_id )) return d;
      }).filter(d => !!d);

      let events = added.map( a => { return {action: 'added', context: '_window', new: a, old: null}});
      events = events.concat(dropped.map( d => { return {action: 'dropped', context: '_window', new: null, old: d}}));
      events = events.concat(changed.map( ({old, now, diff}) => determineChange(old, now, diff) ));
      return events
  }
};
