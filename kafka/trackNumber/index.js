const program = require('commander');
const {changesAsEvents, kvDeltaStateByTrainId} = require('./utils');

function currentState(deltaTopic) {
  const {table} = require('table');

  const ktable = factory.getKTable(null, message => {
      const value = message.value.toString('utf8');
      const dvr = JSON.parse(value);
      const [k, v] = kvDeltaStateByTrainId(dvr); 
      return {key: k, value: v};
    })
    .from(deltaTopic)
  const key = (r) => {
    let departs = r.departs_at.split(":");
    return departs.join('') * 1 + 10000;
  };

  ktable
    .consumeUntilMs(2000, () => {
      ktable.getTable()
	.then(results => {
	  let data = Object.values(results)
	    .filter(r => !!r)
	    .sort((a, b) => {
	      let keya = key(a);
	      let keyb = key(b);
	      return keya - keyb;
	    })
	    .map(d => [d.departs_at, d.dest, d.track, d.line, d.train_id, d.status]);

	  console.log(table(data));
	})
    })
    ;
  return ktable.start();
}

function startStream(prevState, topic, deltaTopic) {
  var prev = prevState;

  const stream = factory
    .getKStream()
    .from(topic)
    .mapJSONConvenience();

  stream
    .map( msg => msg.value.body.departures )
    .concatMap( (curr) => {
      let changes = changesAsEvents(prev, curr); 
      prev = curr;
      return stream.getNewMostFrom(changes);
    })
    .wrapAsKafkaValue()
    .tap( x => console.log(x))
    .to(deltaTopic, 1, 'buffer')
  ;

  return stream.start();
}

//---- main
program
  .option('-t, --topic [value]', "Topic to consume")
  .option('-b, --brokers [value]', "Broker list")
  .option('--no-stream', "Show current state")
  .parse(process.argv);


const {KafkaStreams} = require('kafka-streams');
// FIXME doesn't start from beginning of stream 
const config = require('./config.json');
if (program.brokers) config.noptions['metadata.broker.list'] = program.brokers;


var station, topic;
if (program.topic) {
  station = program.topic.split('.')[0];
  topic = program.topic;
} else {
  station = "ny";
  topic = "ny.dvr";
}
config.noptions['client.id'] += `-${station}`;
config.noptions['group.id'] += `-${station}`;
if (!program.stream) 
  config.noptions['group.id'] += `-peek`;

config.noptions['offset_commit_cb'] = function(err, topicPartitions) {
      if (err) {
        // There was an error committing
         console.error(err);
      } else {
       // Commit went through. Let's log the topic partitions
             console.log(topicPartitions);
     }
}

const factory = new KafkaStreams(config);
factory.on('error', err => {
  console.error(`Major error: ${err}`);
});

const deltaTopic =`${station}.delta`;

if (program.stream) startStream([], topic, deltaTopic);
else currentState(deltaTopic);
