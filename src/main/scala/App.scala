import actors.Watcher.Watch
import actors.{Exporter, Watcher}
import akka.actor.{ActorContext, ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory


object AkkaQuickstart extends App {

  implicit val system: ActorSystem = ActorSystem("akka-file-ingester")

  val exporter = system.actorOf(Exporter.props(), "exporter")

exporter ! "test"





}