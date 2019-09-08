import actors.Watcher
import actors.Watcher.Watch
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory


object AkkaQuickstart {

  //TODO TUTTI I TO DO DIVENTANO PUNTI DI UNA PRESENTAZIONE

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("akka-file-ingester")

    val watchers = ConfigFactory.load().getString("active-watchers").split(",")

    val actors = watchers.map{ name => system.actorOf(Watcher.props(name), name + "-watcher") }

    actors.foreach{ actor => actor ! Watch  }

  }

}