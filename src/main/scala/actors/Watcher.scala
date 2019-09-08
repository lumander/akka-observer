package actors

import java.nio.file._
import java.text.SimpleDateFormat

import actors.CustomLogger._
import akka.actor.{Actor, ActorRef, Props}
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

  import Watcher._

  val directory: String = ConfigFactory.load().getConfig(name).getString("directory")
  val matchingRegex: String = ConfigFactory.load().getConfig(name).getString("matching-regex")

  val logger: ActorRef = context.actorOf(CustomLogger.props(name), name + "-logger")
  val publisher: ActorRef = context.actorOf(Publisher.props(logger, name), name + "-publisher")


  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def receive = {

    case Watch =>

      val fs = FileSystems.getDefault
      val changes = DirectoryChangesSource(fs.getPath(directory), pollInterval = 1.second, maxBufferSize = 1000)
      changes.runForeach {
        case (path, change) => handleChanges(fs,path.toString, change)
      }

    //case MetricObject(name,count) => user() ! MetricObject(name,count)

    case _ => logger ! UnexpectedMessage

  }


  def handleChanges(fs: FileSystem, path: String, change: DirectoryChange): Unit = {

    import CustomLogger._
    import Publisher._

    change match {

      case DirectoryChange.Creation =>
         logger ! FileCreated(path)


      case DirectoryChange.Modification => {
        val fileName = path.split("/").last
        if (fileName.matches(matchingRegex)) {
          if (checkDuplicates(fs,path)){
            val date = getLastModificationTime(fs,path)
            logger ! FileAlreadyExists(path+".COMPLETED",date)
          }
          else{
            logger ! NewFileArrived(path)
            publisher ! Publish(name, path)
          }

        }
        else {
          logger ! DifferentMatch(path)
        }
      }

      case DirectoryChange.Deletion =>
        logger ! FileDeleted(path)
    }

  }

  def checkDuplicates(fs: FileSystem, path: String): Boolean = {

    Files.exists(fs.getPath(s"$path.COMPLETED"))

  }

  def getLastModificationTime(fs: FileSystem, path: String): String = {

    val df = new SimpleDateFormat("yyyy-MM-dd")
    df.format(Files.getLastModifiedTime(fs.getPath(s"$path.COMPLETED")).toMillis)

  }

}