# Kafka Streams Application
From a stream of departure boards creates a diff stream.
```
DB--DB-DB---DB-DB--DB
|      |           |
diff   diff        diff
```

## Utils
### jshell with classpath
```
mvn jshell:run
```
Similar to `sbt console`, it launches a jshell with the classpath set to the same as maven.

Can be used with test classes if run 
```
mvn jshell:run -DtestClasspath
```

### Docker
Will build a docker image during the `mvn package` target. To disable use
```
mvn package -Ddockerfile.skip=true
```