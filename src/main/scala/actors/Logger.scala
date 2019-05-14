package actors

import akka.actor.{Actor, ActorLogging, Props}

object Logger {
  def props: Props = Props[Logger]
  final case class NewFileArrived(newFileArrived: String)
  final case class NoMatchFound(noMatchFound: String)
  final case class NewFileCompleted(newFileCompleted: String)
  final case class FileDeleted(newFileCompleted: String)
  case object UnexpectedMessage

}

class Logger extends Actor with ActorLogging {
  import Logger._
//TODO RIsolvere failed to load class org.slf4j.impl.StaticLoggerBinder
  def receive = {
    case _ => log.info("Unexpected message {}", _ )
    case NewFileArrived(newFileArrived) =>
      log.info("New file arrived (from " + sender() + "): " + newFileArrived)
    case NoMatchFound(noMatchFound) =>
      log.info("No match found (from " + sender() + "): " + noMatchFound)
    case NewFileCompleted(newFileCompleted) =>
      log.info("New file completed (from " + sender() + "): " + newFileCompleted)
  }
}