package example

import com.google.gson.Gson
import com.microsoft.azure.sdk.iot.device.{IotHubStatusCode, IotHubEventCallback, IotHubClientProtocol, DeviceClient, Message}
import java.util.concurrent.{Executors, ExecutorService, TimeUnit}
import scala.concurrent.{Future}
import scala.util.Random
import play.api.libs.json._
import play.api.libs.functional.syntax._

object Hello extends Greeting with App {
  final val protocol: IotHubClientProtocol = IotHubClientProtocol.MQTT
  if (args.length < 3) {
    println("Parameter length is lower 3.")
    println("Please input deviceId youriothubname yourdevicekey")
    sys.exit
  }
  
  final val deviceId: String = args(0)
  final val youriothubname: String = args(1)
  final val yourdevicekey: String = args(2)
  final val connString: String = s"HostName=$youriothubname.azure-devices.net;DeviceId=$deviceId;SharedAccessKey=$yourdevicekey"
  final val connStr:String = "Endpoint={youreventhubcompatibleendpoint};EntityPath={youreventhubcompatiblename};SharedAccessKeyName=iothubowner;SharedAccessKey={youriothubkey}"

  sendAsync
  println(greeting)
  
  def sendAsync(): Unit ={
    
    val client: DeviceClient = new DeviceClient(connString, protocol)
    client.open()
    
    val executor: ExecutorService = Executors.newFixedThreadPool(1)
    executor.execute(() =>{
      try {
        val minTemperature: Double = 20
        val minHumidity: Double = 60
        val rand: Random = new Random
        
        while (true) {
          val currentTemperature: Double = minTemperature + rand.nextDouble() * 15
          val currentHumidity: Double = minHumidity + rand.nextDouble() * 20          
          implicit val telemetryDataPointWrites = Json.writes[TelemetryDataPoint]
          val msgStr: String = Json.toJson(TelemetryDataPoint(deviceId, currentTemperature, currentHumidity)).toString()
          val msg: Message = new Message(msgStr)
          msg.setProperty("temperatureAlert", (if (currentTemperature > 30)  "true" else "false"))
          msg.setMessageId(java.util.UUID.randomUUID().toString())

          println("Sending: " + msgStr.toString());
          
          val lockobj: AnyRef = new AnyRef
          client.sendEventAsync(msg, (status, context) =>{
            println("IoT Hub responded to message with status: " + status.name)
            context match {
              case v:AnyRef => { v.synchronized{ v.notify } }
              case _ => println("unknown")
              }
          }, lockobj)
          
          lockobj.synchronized{
            lockobj.wait
          }
          Thread.sleep(30000);
        }
      } catch { case e: InterruptedException =>{
        println("Finished.")
        }
      }     
    })

    println("Press ENTER to exit.");
    System.in.read()
    executor.shutdownNow()
    client.closeNow()
  }
 
  case class TelemetryDataPoint(deviceId: String, temperature: Double, humidity: Double)

  // Create a receiver on a partition.
  import com.microsoft.azure.eventhubs.{RetryPolicy, EventHubClient, EventPosition, EventData}
  def receiveMessages(partitionId: String): EventHubClient = {
    
    import java.time.Instant
    import java.nio.charset.Charset
    val pool = java.util.concurrent.Executors.newFixedThreadPool(1)
    val client = EventHubClient.createSync(connStr, pool)
    client.createReceiver(EventHubClient.DEFAULT_CONSUMER_GROUP_NAME, partitionId, EventPosition.fromEndOfStream)
    .thenAccept((receiver) => {
      println("** Created receiver on partition " + partitionId);
      while(true){
        val recieveEvents = receiver.receive(100).get
        recieveEvents.forEach((eventData) =>{
        println("Offset: %s, SeqNo: %s, EnqueueTime: %s".format(eventData.getSystemProperties().getOffset(),
              eventData.getSystemProperties().getSequenceNumber,
              eventData.getSystemProperties().getEnqueuedTime))
        println("| Device ID: %s".format(eventData.getSystemProperties.get("iothub-connection-device-id")))
        println("| Message Payload: %s".format(eventData.getBytes, Charset.defaultCharset()))
        })
      }
    }
    )
    return client
  }
}
trait Greeting {
  lazy val greeting: String = "hello"
}
