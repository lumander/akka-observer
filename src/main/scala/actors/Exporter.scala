package actors

import actors.Watcher.Watch
import akka.actor.{Actor, ActorContext, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.io.StdIn
import scala.io.StdIn

object Exporter {
  def props()(implicit system: ActorSystem): Props = Props(new Exporter())

}

class Exporter()(implicit system: ActorSystem) extends Actor{
  import Logger._

  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher


  def receive = {

    case Counter(count) => expose(count)
    case "test" => startWatchers()

  }

  def expose(count: Int): Unit ={
    val route =
      path("metrics") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Counted events"+count+"</h1>"))
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  def startWatchers(): Unit = {

    val watchers = ConfigFactory.load().getString("active-watchers").split(",")

    val actors = watchers.map{ name => context.actorOf(Watcher.props(name), name + "-watcher") }

    actors.foreach{ actor => actor ! Watch  }

  }

}
