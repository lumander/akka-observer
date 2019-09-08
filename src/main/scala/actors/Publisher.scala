package actors

import java.nio.file.Paths._
import java.nio.file.{Files, Paths, StandardCopyOption}

import actors.Publisher.Publish
import akka.actor.{Actor, ActorRef, Props}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.scaladsl.{Broadcast, FileIO, GraphDSL, RunnableGraph, Sink}
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.util.ByteString
import com.typesafe.config._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import spray.json.{DefaultJsonProtocol, JsValue, JsonWriter}

import scala.concurrent.ExecutionContext
import scala.util.{Failure => ScalaFailure, Success => ScalaSuccess}


object Publisher {

  def props(logger: ActorRef, name: String): Props =
    Props(new Publisher(logger, name))

  case class Publish(name: String, fileName: String)

}

class Publisher(logger: ActorRef, name: String) extends Actor
  with DefaultJsonProtocol {

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = context.dispatcher

  import CustomLogger._


  def receive = {


    case Publish(name, fileName) => {
      publish(name, fileName)

    }

    case _ => logger ! UnexpectedMessage

  }

  def publish(name: String, path: String)(implicit materializer: ActorMaterializer): Unit = {

    logger ! StartProcessing(name, path)

    val csvDelimiter = ConfigFactory.load().getConfig(name).getString("csv-delimiter")
    val csvQuoteChar = ConfigFactory.load().getConfig(name).getString("csv-quote-char")
    val csvEscapeChar = ConfigFactory.load().getConfig(name).getString("csv-escape-char")

    val producerConfig = ConfigFactory.load().getConfig(name).getConfig("akka.kafka.producer")
    val bootstrapServers = ConfigFactory.load().getConfig(name).getString("bootstrap-servers")
    val topic = ConfigFactory.load().getConfig(name).getString("topic")
    val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)


    val kafkaSink = Producer.plainSink(producerSettings)

    val counterSink = Sink.fold[Int, String](0)((counter, _) => counter + 1)

    val fanOutGraph = RunnableGraph.fromGraph(GraphDSL.create(kafkaSink, counterSink)((_, _)) { implicit builder =>
      (s1, s2) =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[String](2))
//TODO disaccoppiare la cartella di arrivo da quella di processing: FONDAMENTALE PER FILE GROSSI
        val source =
          FileIO
            .fromPath(get(path))
            .via(CsvParsing.lineScanner()) //TODO CAPIRE COME PARAMETRIZZARE I PARAMETRI DEL LINE SCANNER (BYTEs)
            .via(CsvToMap.toMap()) // TODO PARAMETRIZZARE LA PRESENZA DELL'HEADER
            .map(cleanseCsvData)
            .map(toJson)
            .map(_.compactPrint)


        source ~> broadcast.in

        broadcast.out(0)
          .map(value => new ProducerRecord[String, String](topic, value)) ~> s1

        broadcast.out(1) ~> s2

        ClosedShape
    })

    val (kafkaFuture, counterFuture) = fanOutGraph.run()

    kafkaFuture onComplete { // forse è un errore mettere un onComplete qui
      case ScalaSuccess(count) => {
        logger ! FileProcessed(path)
        renameFile(path: String)
      }
      case ScalaFailure(_) => logger ! KafkaConnectionException
    }

    counterFuture onComplete {
      case ScalaSuccess(count) => {
        println(s"Number of elements: $count")
        //pushMetrics()
      }
      case ScalaFailure(_) =>
    }


  }

  def cleanseCsvData(csvData: Map[String, ByteString]): Map[String, String] =
    csvData // TODO Gestire i numeri come tali e non come stringhe
      .filterNot { case (key, _) => key.isEmpty }
      .mapValues(_.utf8String)



  def toJson(map: Map[String, String])(
    implicit jsWriter: JsonWriter[Map[String, String]]): JsValue = jsWriter.write(map)

  def renameFile(sourceFilename: String): Unit = {
//TODO Rendere il job stateful: un file già processato non deve essere riprocessato
    val path = Files.move(
      Paths.get(sourceFilename),
      Paths.get(sourceFilename + ".COMPLETED"),
      StandardCopyOption.REPLACE_EXISTING //TODO oppure atomic moving
    )

    if (path != null) {
      logger ! FileRenamed(path.toString)
    } else {
      logger ! ExceptionWhileRenaming(path.toString)
    }

  }


}