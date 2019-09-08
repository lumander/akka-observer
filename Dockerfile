FROM openjdk:8
COPY ./target/scala-2.12/akka-observer-assembly-0.1.jar /akka-observer/akka-observer.jar
WORKDIR /akka-observer
CMD ["java", "-jar","akka-observer.jar"]
