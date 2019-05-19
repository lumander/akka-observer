  package actors

  import java.nio.file.FileSystems

  import akka.actor.{Actor, ActorRef, ActorSystem, Props}
  import akka.stream.ActorMaterializer
  import akka.stream.alpakka.file.DirectoryChange
  import akka.stream.alpakka.file.scaladsl.DirectoryChangesSource
  import com.typesafe.config.ConfigFactory

  import scala.concurrent.duration._

  object Watcher {

    def props(name: String)(implicit system: ActorSystem): Props =
      Props(new Watcher(name))


  }

  class Watcher(name: String)(implicit system: ActorSystem) extends Actor {

    //TODO MI PIACEREBBE AVERE UN FILE DI LOG PER OGNI FILE CSV

    import Logger._

    val directory = ConfigFactory.load().getConfig(name).getString("directory")
    val matchingRegex = ConfigFactory.load().getConfig(name).getString("matching-regex")

    val logger = context.actorOf(props = Props[Logger], name + "-logger")
    val publisher = context.actorOf(Publisher.props(logger,name), name + "-publisher")


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

      //case MetricObject(name,count) => user() ! MetricObject(name,count)

      case _ => logger ! UnexpectedMessage

    }

    def dispatch(logger: ActorRef, publisher: ActorRef, path: String, matchingRegex: String): Unit = {

      val fileName = path.split("/").last
      if (fileName.matches(matchingRegex)) {
        logger ! NewFileArrived(path)
        publisher ! Publish(name,path) }
      else {
        logger ! NoMatchFound(path) //TODO VOGLIO ESSERE INFORMATO DI EVENTUALI ARRIVI CHE NON CORRISPONDONO ALLA REGEX
      }

    }

  }