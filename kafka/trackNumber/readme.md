# Kafka Node Streaming application
Attempts to replicate kafka-streams in node instead of Java

## Consuming
Much pain went into figuring out how to get this to read from the beginning of a stream by default.  The configuration listed in the [quick start](https://github.com/nodefluent/kafka-streams/blob/master/docs/quick-start.md) is *not* the default. As specified on the [native client wiki](https://github.com/edenhill/librdkafka/wiki/Manually-setting-the-consumer-start-offset)(which using `noptions` in the config enables automatically) when a group and topic don't have an offset then it will default to the **topic** configuration's `auto.offset.reset` value.   Again, the quick start is not the default, to that needed to be added under **tconf** (topic config) sub-object.

To list consumer groups use
```
kafka.consumer-groups --list --bootstrap-server mac-mini:9092
```

To look at consumer groups' offsets use
```
watch -d kafka.consumer-groups --describe --group $groupid --bootstrap-server mac-mini:9092
``


And finally, to reset a consumer group to the earliest available (see `kafka.consumer-groups --help` for other options)
```
kafka.consumer-groups --reset-offsets --group $groupid --to-earliest --bootstrap-server=mac-mini:9092 --all-topics --execute
```

### Production use
The configuration of `noption['enable.auto.commit']` should be set to `true` so that in the case of a restart the system will pick up where it left off. Caviat here, the first records seen will all be assumed to be adds until the code is corrected.

## Producing
A diff stream is produced to `$station.delta` using the `#to()` method of the `KStream`. The tricky part here is that this will allow you to specify *just* a topic name and fail silently if that topic doesn't exist. 

In order to create a topic, the partition count and, in this case, type of `buffer` (because its not a byte array) need to be specifie
