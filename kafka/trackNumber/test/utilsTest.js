const assert = require('assert');
const utils = require('../utils.js');

describe('utils', () => {
  describe('#changesAsEvents()', () => {
    it('should see all new as additions', () => {
      let departure = {train_id: '1', track: '', at: '2017-01-01T00:00:00'};
      let actual = utils.changesAsEvents([], [
        departure
      ]);
      assert.deepEqual(
        [{action: 'added', context: '_window', new: departure, old: null}],
        actual
      );
    });
    it('should see all old, no new as drops', () => {
      let departure = {train_id: '1', track: '2', at: '2017-01-01T00:00:00'};
      let actual = utils.changesAsEvents([ departure ], []);
      assert.deepEqual(
        [{action: 'dropped', context: '_window', new: null, old: departure}],
        actual
      );
    });
    it('should find differences', () => {
      let oldDeparture = {train_id: '1', track: '', at: '2017-01-01T00:00:00'};
      let newDeparture = {train_id: '1', track: '2', at: '2017-01-01T00:02:00'};
      let actual = utils.changesAsEvents([ oldDeparture ], [ newDeparture ]);
      assert.deepEqual(
        [{action: 'changed', context: 'track', new: newDeparture, old: oldDeparture}],
        actual
      );
    });
  });
  describe('#kvDepartureTrack()', () => {
    it('should key off train_id and day with track as value', () => {
      let actual = utils.kvDepartureTrack({train_id: 'A123', at: '2017-01-04T12:00:00Z', track: '3'}); // skirting timezone issues
      assert.deepEqual(['20170104-A123', '3'], actual);
    });
  });
  describe('#kvDeltaStateByTrainId()', () => {
    it('should return train_id and null for dropped events', () => {
      let actual = utils.kvDeltaStateByTrainId({action:"dropped", old: {train_id: 'A123', at: '2017-01-04T12:00:00Z', track: '3'}});
      assert.deepEqual(['A123', null], actual);
    });
    it('should return new train_id and track for changed events', () => {
      let latest = {train_id: 'A123', at: '2017-01-04T12:00:00Z', track: '3'};
      let actual = utils.kvDeltaStateByTrainId({
        action:"changed", 
        old: {train_id: 'A123', at: '2017-01-04T12:00:00Z', track: ''}, 
        new: latest 
      }); 
      assert.deepEqual(['A123', latest], actual);
    });
    it('should return new train_id and track for added events', () => {
      let latest = {train_id: 'A123', at: '2017-01-04T12:00:00Z', track: '3'};
      let actual = utils.kvDeltaStateByTrainId({
        action:"added", 
        old: null, 
        new: latest
      }); 
      assert.deepEqual(['A123', latest], actual);
    });
  });
});
