## akka-observer

This module is intended as a file watcher.
An actor is watching on a folder. When a new file arrives, it triggers another actor which converts its content in JSON format
and publishes its event on Kafka.


