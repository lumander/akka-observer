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
  case object UnexpectedMessage

}

class Logger extends Actor
  with ActorLogging {
  import Logger._
//TODO RIsolvere failed to load class org.slf4j.impl.StaticLoggerBinder
  def receive = {
    case _ => log.info("Unexpected message {}", _ )
    case NewFileArrived(path) =>
      log.info("Message from " + sender() )
      log.info("New file arrived:" + path)
    case NoMatchFound(noMatchFound) =>
      log.info("Message from " + sender() )
      log.info("No match found " + noMatchFound)
    case NewFileCompleted(newFileCompleted) =>
      log.info("Message from " + sender() )
      log.info("New file completed " + newFileCompleted)
    case FileDeleted(fileDeleted) =>
      log.info("Message from " + sender() )
      log.info("File deleted " + fileDeleted)
    case Publish(name,fileName) =>
      log.info("Message from " + sender() )
      log.info("Start publishing for " + name + "- New file arrived: " + fileName)
  }
}