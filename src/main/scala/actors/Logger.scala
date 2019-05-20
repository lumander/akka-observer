package actors

import akka.actor.{Actor, ActorLogging, Props}

object Logger {

  def props: Props = Props[Logger]

  case class NewFileArrived(path: String)

  case class DifferentMatch(noMatchFound: String)

  case class FileProcessed(newFileCompleted: String)

  case class StartProcessing(name: String, path: String)

  case class FileCreated(fileCreated: String)

  case class FileDeleted(fileDeleted: String)

  case class FileRenamed(fileMoved: String)

  case class ExceptionWhileRenaming(fileToMove: String)

  case class Counter(count: Int)

  case class MetricObject(name: String, counter: Int)

  case object UnexpectedMessage

  case object KafkaConnectionException


}

class Logger extends Actor
  with ActorLogging {

  import Logger._

  //TODO RIsolvere failed to load class org.slf4j.impl.StaticLoggerBinder
  def receive = {
    case NewFileArrived(path) =>
      log.info("\n\tMessage from " + sender())
      log.info("\n\tNew file arrived:" + path)
    case DifferentMatch(noMatchFound) =>
      log.info("\n\tMessage from " + sender())
      log.info("\n\tDifferent match found: " + noMatchFound)
    case FileProcessed(path) =>
      log.info("\n\tMessage from " + sender())
      log.info("\n\tFile processed: " + path)
    case FileCreated(fileCreated) =>
      log.debug("\n\tMessage from " + sender())
      log.debug("\n\tFile created: " + fileCreated)
    case FileDeleted(fileDeleted) =>
      log.info("\n\tMessage from " + sender())
      log.info("\n\tFile deleted: " + fileDeleted)
    case FileRenamed(fileRenamed) =>
      log.info("\n\tMessage from " + sender())
      log.info("\n\tFile renamed: " + fileRenamed)
    case ExceptionWhileRenaming(fileRenamed) =>
      log.error("\n\tMessage from " + sender())
      log.error("\n\tException while renaming: " + fileRenamed)
    case StartProcessing(name, fileName) =>
      log.info("\n\tMessage from " + sender())
      log.info("\n\tStart processing for " + name + " watcher - New file arrived: " + fileName)
    case KafkaConnectionException =>
      log.error("\n\tUnable to connect to Kafka")
    case _ =>
      log.info("\n\tUnexpected message {}")
  }
}