const {KafkaConsumer} = require('node-rdkafka');

const consumer = new KafkaConsumer({
    "group.id": "manual-test-t",
    "metadata.broker.list": "mac-mini:9092",
    "auto.offset.reset": "earliest",
    offset_commit_cb(err, tp) {
      if (err){
        console.error('error commiting: ', err);
      } else {
        console.log('offset committed', tp);
      }
    }
}, {
    "auto.offset.reset": "earliest",
});

consumer
  .on('ready', () => {
    consumer.subscribe(['tr.dvr']);
    consumer.consume();
  })
  .on('data', (msg) => {
    console.log(msg.value.toString());
  })
  .on('event.error', (err) => {
    console.error(err);
  })
;

consumer.connect();
