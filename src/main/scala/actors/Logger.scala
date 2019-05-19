package actors

import akka.actor.{Actor, ActorLogging, Props}

object Logger {
  def props: Props = Props[Logger]

  case object Watch
  case class NewFileArrived(path: String)
  case class NoMatchFound(noMatchFound: String)
  case class NewFileCompleted(newFileCompleted: String)
  case class FileDeleted(fileDeleted: String)
  case class Publish(name: String,fileName: String)
  case class Counter(count: Int)
  case class MetricObject(name: String, counter: Int)
  case object UnexpectedMessage

}

class Logger extends Actor
  with ActorLogging {
  import Logger._
//TODO RIsolvere failed to load class org.slf4j.impl.StaticLoggerBinder
  def receive = {
    case NewFileArrived(path) =>
      log.info("Message from " + sender() )
      log.info("New file arrived:" + path)
    case NoMatchFound(noMatchFound) =>
      log.info("Message from " + sender() )
      log.info("No match found " + noMatchFound)
    case NewFileCompleted(path) =>
      log.info("Message from " + sender() )
      log.info("New file completed " + path)
    case FileDeleted(fileDeleted) =>
      log.info("Message from " + sender() )
      log.info("File deleted " + fileDeleted)
    case Publish(name,fileName) =>
      log.info("Message from " + sender() )
      log.info("Start processing for " + name + "- New file arrived: " + fileName)
    case _ => log.info("Unexpected message {}" )
  }
}