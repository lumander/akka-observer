import AkkaQuickstart.system
import actors.Watcher
import akka.actor
import akka.actor.{ActorRef, ActorSystem}


object AkkaQuickstart extends App {

  implicit val system: ActorSystem = ActorSystem("akka-file-ingester")

  val watchers = Array("barclay","voucher") //TODO INSERIRE LETTURA DA FILE DI CONFIGURAZIONE
  startWatchers(watchers)

}

def startWatchers(watchers: Array[String]): Unit = {

  import Watcher._

  val actors: Array[ActorRef] = watchers.map{ line => system.actorOf(Watcher.props(line), line)}

  actors.map{ actor.asInstanceOf[ActorRef] ! Watch }

}

