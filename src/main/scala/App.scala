import AkkaQuickstart.system
import actors.Logger.Watch
import actors.{Exporter, Watcher}
import akka.actor
import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory


object AkkaQuickstart extends App {

  implicit val system: ActorSystem = ActorSystem("akka-file-ingester")

  val exporter = system.actorOf(Exporter.props(), "")

    startWatchers(exporter)




def startWatchers(exporter: ActorRef)(implicit system: ActorSystem): Unit = {

  val watchers = ConfigFactory.load().getString("active-watchers").split(",")

  val actors = watchers.map{ name => context.actorOf(Watcher.props(name), name + "-watcher") }

  actors.foreach{ actor => actor ! Watch  }

}

}