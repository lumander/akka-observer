# akka-observer

This is an akka-based file watcher.
An actor is watching on a folder. When a new file arrives, it triggers another actor which converts its content in JSON format
and publishes its events on Kafka.

For each file, a separate log file is created.
The container needs two external mounts.
One for files to be watched and the other for logs to be read.
This is important for deploy environments in which containers
have a dedicated sandbox (e.g. DC/OS cluster) 

If you want to run it locally, open a terminal and run

```
./start_observer.sh
```

## TODO

Adding tests
Adding a simple web-server interface
Adding metrics ( both for JVM and for the application )
Adding architecture design
