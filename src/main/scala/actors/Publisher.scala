package actors

import java.nio.file.Paths._

import actors.Logger.{Counter, NewFileCompleted, Publish}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.scaladsl.{Broadcast, FileIO, GraphDSL, RunnableGraph, Sink}
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.util.ByteString
import com.typesafe.config._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.Metric
import org.apache.kafka.common.serialization.StringSerializer
import spray.json.{DefaultJsonProtocol, JsValue, JsonWriter}

import scala.concurrent.ExecutionContext
import scala.util.{Failure => ScalaFailure, Success => ScalaSuccess}


object Publisher  {
  def props(logger: ActorRef,name: String)(implicit system: ActorSystem): Props = Props(new Publisher(logger,name))
}

class Publisher(logger: ActorRef,name: String)(implicit system: ActorSystem) extends Actor
  with DefaultJsonProtocol{

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = context.dispatcher

  import Logger._


  def receive = {


    case Publish(name,fileName) => {
      logger ! Publish(name,fileName)
        publish(name,fileName)
    }

  }

  def publish(name: String,path: String)(implicit materializer: ActorMaterializer): Unit = {

    val csvDelimiter = ConfigFactory.load().getConfig(name).getString("csv-delimiter")
    val csvQuoteChar = ConfigFactory.load().getConfig(name).getString("csv-quote-char")
    val csvEscapeChar = ConfigFactory.load().getConfig(name).getString("csv-escape-char")

    val producerConfig = ConfigFactory.load().getConfig(name).getConfig("akka.kafka.producer")
    val bootstrapServers = ConfigFactory.load().getConfig(name).getString("bootstrap-servers")
    val topic = ConfigFactory.load().getConfig(name).getString("topic")
    val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)
        .withBootstrapServers(bootstrapServers)


    val kafkaSink = Producer.plainSink(producerSettings)

    val counterSink = Sink.fold[Int, Map[String, ByteString]](0)((counter, _) => counter + 1)

    val g = RunnableGraph.fromGraph(GraphDSL.create(kafkaSink, counterSink)((_, _)) { implicit builder =>
      (s1, s2) =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[Map[String, ByteString]](2))

        val source =
          FileIO
            .fromPath(get(path))
            .via(CsvParsing.lineScanner())//TODO CAPIRE COME PARAMETRIZZARE I PARAMETRI DEL LINE SCANNER (BYTEs)
            .via(CsvToMap.toMap()) // TODO PARAMETRIZZARE LA PRESENZA DELL'HEADER


        source ~> broadcast.in

        broadcast.out(0)
          .map(cleanseCsvData)
          .map(toJson)
          .map(_.compactPrint)
          .map(value => new ProducerRecord[String, String](topic, value))~> s1

        broadcast.out(1) ~> s2

        ClosedShape
    }) // RunnableGraph[(Future[Done], Future[Int])]

    val (kafkaFuture, counterFuture) = g.run()

    kafkaFuture onComplete {
      case ScalaSuccess(count) => {
        logger ! NewFileCompleted(path)
      }
      case ScalaFailure(_) =>
    }

    counterFuture onComplete {
      case ScalaSuccess(count) => {
        logger ! NewFileCompleted(path)
        println(s"Number of elements: $count")
        sender() ! MetricObject(path,count)
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


}