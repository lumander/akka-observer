import AkkaQuickstart.system
import actors.Logger._
import actors.Watcher
import akka.actor.{ActorLogging, ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory


object AkkaQuickstart extends App {

  implicit val system: ActorSystem = ActorSystem("akka-file-ingester")

  startWatchers()

}

def startWatchers(): Unit = {

  val watcherList = ConfigFactory.load().getString("active-watchers").split(",")

  val actors: Array[ActorRef] = watcherList.map{ line => system.actorOf(Watcher.props(line), line)}

  actors.foreach( actor => actor ! Watch )

}

