package actors

import java.nio.file.Paths._
import java.nio.file.{FileSystems, Paths}

import actors.Publisher.Publish
import akka.NotUsed
import akka.actor.{Actor, ActorLogging, Props}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.typesafe.config._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import spray.json.{DefaultJsonProtocol, JsValue, JsonWriter}

import scala.concurrent.Future
import scala.concurrent.duration._


object Publisher  {
  def props: Props = Props[Publisher]
  case object Publish
}

class Publisher extends Actor with ActorLogging with DefaultJsonProtocol with ActorMaterializer {

  implicit val materializer: ActorMaterializer = ActorMaterializer()


  def receive = {
    case Publish => {
      log.info("Start publishing to " + topic )
        publish(topic) }

  }

  def publish(topic: String)(implicit materializer: ActorMaterializer): Unit = {


    val config = ConfigFactory.load().getConfig("akka.kafka.producer")
    val producerSettings = ProducerSettings(config, new StringSerializer, new StringSerializer)
        .withBootstrapServers("localhost:9092")



    FileIO
      .fromPath(get("/home/alessandro/observer/observed_20190878.csv"))
      .via(CsvParsing.lineScanner())
      .via(CsvToMap.toMap())
      .map(cleanseCsvData)                                         //: Map[String, String]     (6)
      .map(toJson)                                                 //: JsValue                 (7)
      .map(_.compactPrint)
      .map(value => new ProducerRecord[String, String](topic, value))
      .runWith(Producer.plainSink(producerSettings))

  }

  def cleanseCsvData(csvData: Map[String, ByteString]): Map[String, String] =
    csvData
      .filterNot { case (key, _) => key.isEmpty }
      .mapValues(_.utf8String)

  def toJson(map: Map[String, String])(
    implicit jsWriter: JsonWriter[Map[String, String]]): JsValue = jsWriter.write(map)

}