package actors

import java.nio.file._

import actors.Logger._
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.DirectoryChange
import akka.stream.alpakka.file.scaladsl.DirectoryChangesSource
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object Watcher {

  def props(name: String): Props =
    Props(new Watcher(name))

  case object Watch


}

class Watcher(name: String) extends Actor {

  //TODO MI PIACEREBBE AVERE UN FILE DI LOG PER OGNI FILE CSV

  import Watcher._

  val directory: String = ConfigFactory.load().getConfig(name).getString("directory")
  val matchingRegex: String = ConfigFactory.load().getConfig(name).getString("matching-regex")

  val logger: ActorRef = context.actorOf(props = Props[Logger], name + "-logger")
  val publisher: ActorRef = context.actorOf(Publisher.props(logger, name), name + "-publisher")


  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def receive = {

    case Watch =>

      val fs = FileSystems.getDefault
      val changes = DirectoryChangesSource(fs.getPath(directory), pollInterval = 1.second, maxBufferSize = 1000)
      changes.runForeach {
        case (path, change) => handleChanges(path.toString, change)
      }

    //case MetricObject(name,count) => user() ! MetricObject(name,count)

    case _ => logger ! UnexpectedMessage

  }


  def handleChanges(path: String, change: DirectoryChange): Unit = {

    import Logger._
    import Publisher._

    change match {

      case DirectoryChange.Creation =>
        logger ! FileCreated(path)

      case DirectoryChange.Modification => {
        val fileName = path.split("/").last
        if (fileName.matches(matchingRegex)) {
          logger ! NewFileArrived(path)
          publisher ! Publish(name, path)
        }
        else {
          logger ! DifferentMatch(path)
        }
      }

      case DirectoryChange.Deletion =>
        logger ! FileDeleted(path)
    }

  }

}