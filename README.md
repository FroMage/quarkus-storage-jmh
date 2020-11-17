# Compile:

```shell
$ mvn clean package
$ ./shade_benchmark.sh
```

# Run with storage:

```shell
$ java -Dquarkus.vertx.storage=true -jar target/quarkus-jmh-1.0.0-SNAPSHOT.jar
```

# Run without storage:

```shell
$ java -Dquarkus.vertx.storage=false -jar target/quarkus-jmh-1.0.0-SNAPSHOT.jar
```
