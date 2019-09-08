package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.slf4j.LoggerFactory

object CustomLogger {


  def props(name: String): Props =
    Props(new CustomLogger(name))

  case class NewFileArrived(path: String)

  case class DifferentMatch(noMatchFound: String)

  case class FileProcessed(newFileCompleted: String)

  case class StartProcessing(name: String, path: String)

  case class FileCreated(fileCreated: String)

  case class FileAlreadyExists(fileDuplicated: String, date: String)

  case class FileDeleted(fileDeleted: String)

  case class FileRenamed(fileMoved: String)

  case class ExceptionWhileRenaming(fileToMove: String)

  case class Counter(count: Int)

  case class MetricObject(name: String, counter: Int)

  case object UnexpectedMessage

  case object KafkaConnectionException


}

class CustomLogger(name: String) extends Actor
  with ActorLogging {

  import CustomLogger._

  val customLogger = LoggerFactory.getLogger(name+".log")

  def receive = {

    case NewFileArrived(path) =>
      customLogger.info("Message from " + sender())
      customLogger.info("New file arrived:" + path)
    case DifferentMatch(noMatchFound) =>
      customLogger.info("Message from " + sender())
      customLogger.info("Different match found: " + noMatchFound)
    case FileProcessed(path) =>
      customLogger.info("Message from " + sender())
      customLogger.info("File processed: " + path)
    case FileCreated(fileCreated) =>
      customLogger.debug("Message from " + sender())
      customLogger.debug("File created: " + fileCreated)
      customLogger.debug("Beware, files should not be created in this folder!")
    case FileAlreadyExists(fileDuplicated, date) =>
      customLogger.info("Message from " + sender())
      customLogger.info("File already processed. " + fileDuplicated + " already processed on " + date)
    case FileDeleted(fileDeleted) =>
      customLogger.info("Message from " + sender())
      customLogger.info("File deleted: " + fileDeleted)
    case FileRenamed(fileRenamed) =>
      customLogger.info("Message from " + sender())
      customLogger.info("File renamed: " + fileRenamed)
    case ExceptionWhileRenaming(fileRenamed) =>
      customLogger.error("Message from " + sender())
      customLogger.error("Exception while renaming: " + fileRenamed)
    case StartProcessing(name, fileName) =>
      customLogger.info("Message from " + sender())
      customLogger.info("Start processing for " + name + " watcher - New file arrived: " + fileName)
    case KafkaConnectionException =>
      customLogger.error("Unable to connect to Kafka")
    case _ =>
      customLogger.info("Unexpected message {}")
  }
}