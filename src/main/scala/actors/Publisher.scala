package actors

import java.nio.file.Paths._

import akka.actor.{Actor, ActorRef, Props}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.scaladsl.FileIO
import akka.util.ByteString
import com.typesafe.config._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import spray.json.{DefaultJsonProtocol, JsValue, JsonWriter}


object Publisher  {
  def props: Props = Props[Publisher]
}

class Publisher(logger: ActorRef) extends Actor
  with DefaultJsonProtocol
  with ActorMaterializer {

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import Logger._


  def receive = {


    case Publish(name,fileName) => {
      logger ! Publish(name,fileName)
        publish(name,fileName)
    }

  }

  def publish(name: String,path: String)(implicit materializer: ActorMaterializer): Unit = {


    val csvDelimiter = ConfigFactory.load().getConfig(name).getString("csv.delimiter")
    val csvQuoteChar = ConfigFactory.load().getConfig(name).getString("csv-quote-char")
    val csvEscapeChar = ConfigFactory.load().getConfig(name).getString("csv-escape-char")

    val producerConfig = ConfigFactory.load().getConfig(name).getConfig("akka.kafka.producer")
    val bootstrapServers = ConfigFactory.load().getConfig(name).getString("bootstrap-servers")
    val topic = ConfigFactory.load().getConfig(name).getString("topic")
    val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)
        .withBootstrapServers(bootstrapServers)


    FileIO
      .fromPath(get(path))
      .via(CsvParsing.lineScanner(csvDelimiter,csvQuoteChar,csvEscapeChar))
      .via(CsvToMap.toMap()) // TODO PARAMETRIZZARE LA PRESENZA DELL'HEADER
      .map(cleanseCsvData)
      .map(toJson)
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