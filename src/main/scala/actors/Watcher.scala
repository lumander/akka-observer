  package actors

  import java.nio.file.FileSystems

  import akka.actor.{Actor, ActorRef, ActorSystem, Props}
  import akka.stream.ActorMaterializer
  import akka.stream.alpakka.file.DirectoryChange
  import akka.stream.alpakka.file.scaladsl.DirectoryChangesSource

  import scala.concurrent.duration._

  object Watcher {

    def props(name: String)(implicit system: ActorSystem): Props =
      Props(new Watcher(name))

    case object Watch

  }

  class Watcher(name: String)(implicit system: ActorSystem) extends Actor {

    import Logger._
    import Publisher._
    import Watcher._

    val directory = "/home/alessandro/observer" // TODO leggere da file di conf dato il nome del watcher
    val matchingRegex = "observed_[0-9]{8}.csv" //TODO leggere da file di conf dato il nome del watcher
    val topic = "akka-publisher-test" //TODO leggere da file di conf dato il nome del watcher

    val logger = context.actorOf(props = Props[Logger], name + "-logger")
    val publisher = context.actorOf(props = Props[Publisher], name + "-publisher")


    implicit val materializer: ActorMaterializer = ActorMaterializer()

    def receive = {

      case Watch =>

        val fs = FileSystems.getDefault
        val changes = DirectoryChangesSource(fs.getPath(directory), pollInterval = 1.second, maxBufferSize = 1000)
        changes.runForeach {
         case (path,change) =>
          change match {
            case DirectoryChange.Creation =>
              dispatch(logger, publisher, path.toString(), matchingRegex)
            case DirectoryChange.Modification =>
              dispatch(logger, publisher, path.toString(), matchingRegex)
            case DirectoryChange.Deletion =>
              logger ! FileDeleted(path.toString())
          }
        }

      case _ => logger ! UnexpectedMessage

    }

    def dispatch(logger: ActorRef, publisher: ActorRef, path: String, matchingRegex: String): Unit = {

      if (path.split("/").last.matches(matchingRegex)) {
        logger ! NewFileArrived(path)
        publisher ! Publish }
      else {
        logger ! NoMatchFound(path)
      }

    }

  }