import AkkaQuickstart.system
import actors.Logger.Watch
import actors.Watcher
import akka.actor
import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory


object AkkaQuickstart extends App {

  implicit val system: ActorSystem = ActorSystem("akka-file-ingester")


    startWatchers()




def startWatchers()(implicit system: ActorSystem): Unit = {

  val watchers = ConfigFactory.load().getString("active-watchers").split(",")

  val actors = watchers.map{ name => system.actorOf(Watcher.props(name), name + "-watcher") }

  actors.foreach{ actor => actor ! Watch  }

}

}