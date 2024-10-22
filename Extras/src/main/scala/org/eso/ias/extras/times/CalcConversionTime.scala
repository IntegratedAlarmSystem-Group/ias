package org.eso.ias.extras.times

import com.typesafe.scalalogging.Logger
import org.eso.ias.kafkautils.{KafkaHelper, KafkaStringsConsumer, SimpleKafkaIasiosConsumer}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{IASValue, Identifier, IdentifierType}

import java.util
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import scala.jdk.javaapi.CollectionConverters
import scala.util.Try

/**
  * CalcConversionTime continuosly gets IASIOs from the core topic
  * and evaluate and periodically log the average time spent by the Converter.
  *
  * The time is calculated by checking the timestamps of the IASIOs. To make calculation
  * more precise (in spite of memory consumption) the process keep in memory all the evaluated
  * times
  *
  * The tool logs 2 average times:
  * - total, end to end: from when the plugin pushed the value in the plugin topic
  *                      to the moment the converter push the IASIO in the core topic
  * - conversion time: from when the converter takes the value from the plugin topic
  *                    to the moment the IASIO i spushed in the BSDB
  *
  * The former takes into account the delay a value remains in the plugin topic before
  * being processed. If it is too long then it is probably the case to deploy another converter.
  *
  * The latter is pure converter calculation and should never grow substantially.
  *
  * @param servers Kafka brokers
  */
class CalcConversionTime(servers: String)
  extends SimpleKafkaIasiosConsumer(servers, KafkaHelper.IASIOs_TOPIC_NAME, CalcConversionTime.id)
    with SimpleKafkaIasiosConsumer.IasioListener {

  /** Total number of IASIOs received so far */
  val iasiosProcessed: AtomicLong = new AtomicLong(0)

  /** Total number of IASIOs received in the past time interval */
  val iasiosProcessedPastTimeInt: AtomicLong = new AtomicLong(0)

  /**
    * The sum of the times spent by the converter converting the IASIOs
    * consumed by the plugin core into IASIOs
    * The sum is needed to alculate the mathematical average of all the sameples
    * whose number is stored in [[timesOfConverterSamples]]
    *
    * The times summed in this accumulatoe are the time between recv from plugin
    * to the sentToBsdbTStamop
    */
  val timesOfConverter: AtomicLong = new AtomicLong(0)

  /**
    * The number of sample to calculate the average of the time of the converter
    */
  val timesOfConverterSamples: AtomicLong = new AtomicLong(0)

  /**
    * The sum of all the total times to convert i.e from the sentToConverterTimestamp
    * to the sentToBsdTimestamp.
    * The sum is needed to calculate the mathematical average of all the sameples
    * whose number is stored in [[endToEndSamples]]
    *
    * The times summed in this accumulator are the time from when the plugin submitted
    * the value to the plugin topic (sentToConvertTStamp) till the moment the converter
    * pushes the IASIO in the core topic
    */
  val endToEndTimes: AtomicLong = new AtomicLong(0)

  /**
    * The number of sample to calculate the end to end average
    */
  val endToEndSamples: AtomicLong = new AtomicLong(0)

  /** The executor to periodically log the average time */
  val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

  /**
    * Max time difference between the actual time and the time when
    * a IASIOs has been put in the BSDB (i.e. sentToBsdTStamp) in the past time interval
    */
  val maxTimeShiftPastTimeInt: AtomicLong = new AtomicLong(0)

  /**
    * Max time difference between the actual time and the time when
    * a IASIOs has been put in the BSDB (i.e. sentToBsdTStamp) since the
    * begining of the execution
    */
  val maxTimeShift: AtomicLong = new AtomicLong(0)

  def start(): Unit = {
    CalcConversionTime.logger.info("Starting the log publisher thread to run every {} minute",
      CalcConversionTime.PERIODIC_LOG_MESSAGE_TIME)
    // Start the thread to log the message
    executor.scheduleWithFixedDelay(
      new Runnable {
        override def run(): Unit = logTimes()
      },
      CalcConversionTime.PERIODIC_LOG_MESSAGE_TIME,
      CalcConversionTime.PERIODIC_LOG_MESSAGE_TIME,
      TimeUnit.MINUTES
    )

    CalcConversionTime.logger.info("Initializing the kafka consumer")
    this.setUp()

    CalcConversionTime.logger.info("Start processing events from the core topic")
    this.startGettingEvents(KafkaStringsConsumer.StreamPosition.END,this)
    CalcConversionTime.logger.info("Initialization done")
  }

  /** Logs the average time */
  def logTimes() = {
    if (timesOfConverterSamples.get() > 0) {
      CalcConversionTime.logger.info("Average conversion time={}ms (from {} samples)",
        timesOfConverter.get() / timesOfConverterSamples.get(),
        timesOfConverterSamples.get())
    }
    if (endToEndSamples.get() > 0) {
      CalcConversionTime.logger.info("Average time from plugin to core topic={}ms (from {} samples)",
        endToEndTimes.get() / endToEndSamples.get(),
        endToEndSamples.get())
    }
    CalcConversionTime.logger.info("{} IASIOs processed so far (of which {} in the past {} minutes)",
      iasiosProcessed.get(),
      iasiosProcessedPastTimeInt.getAndSet(0),
      CalcConversionTime.PERIODIC_LOG_MESSAGE_TIME)

    CalcConversionTime.logger.info("Max time shift {} secs ({} in the past {} minutes)",
      maxTimeShift.get()/1000,
      maxTimeShiftPastTimeInt.getAndSet(0)/1000,
      CalcConversionTime.PERIODIC_LOG_MESSAGE_TIME)
  }

  /**
    * Process the IASIOs consumed from the core topic to extract times
    */
  override def iasiosReceived(events: util.Collection[IASValue[?]]): Unit = {
    val iasios: Iterable[IASValue[?]] = CollectionConverters.asScala(events)

    // Discard IASIOs produced by DASUs
    val iasiosFromPlugin = iasios.filter( iasValue =>
      Identifier(iasValue.fullRunningId).getIdOfType(IdentifierType.PLUGIN).isDefined)

    CalcConversionTime.logger.debug("Got {} events of which {} from plugins",
      iasios.size,
      iasiosFromPlugin.size)

    iasiosProcessed.addAndGet(iasios.size)
    if (iasiosProcessed.get()<0) iasiosProcessed.set(0)

    iasiosProcessedPastTimeInt.addAndGet(iasios.size)

    iasiosFromPlugin.foreach(iasio => {

      if (iasio.sentToBsdbTStamp.isPresent && iasio.sentToConverterTStamp.isPresent) {
        endToEndTimes.addAndGet(iasio.sentToBsdbTStamp.get-iasio.sentToConverterTStamp.get())
        endToEndSamples.incrementAndGet()
        if (endToEndTimes.get()<0 || endToEndSamples.get()<0) {
          endToEndTimes.set(0)
          endToEndSamples.set(0)
        }

      }
      if (iasio.sentToBsdbTStamp.isPresent && iasio.receivedFromPluginTStamp.isPresent) {
        timesOfConverter.addAndGet(iasio.sentToBsdbTStamp.get-iasio.receivedFromPluginTStamp.get)
        timesOfConverterSamples.incrementAndGet()
        if (timesOfConverter.get<0 || timesOfConverterSamples.get()<0) {
          timesOfConverter.set(0)
          timesOfConverterSamples.set(0)
        }
      }

      if (iasio.sentToBsdbTStamp.isPresent) {
        val timeShift = System.currentTimeMillis()-iasio.sentToBsdbTStamp.get()
        maxTimeShift.accumulateAndGet(timeShift, (a,b) => a.max(b))
        maxTimeShiftPastTimeInt.accumulateAndGet(timeShift, (a,b) => a.max(b))
      }

    })
  }

  def shutdown() = {
    CalcConversionTime.logger.info("Shutting down the logger thread")
    executor.shutdown()

    CalcConversionTime.logger.info("Closing the kafka IASIOs consumer")
    tearDown()
  }

}

/** Companion object */
object CalcConversionTime {

  /** The logger */
  val logger: Logger = IASLogger.getLogger(CalcConversionTime.getClass)

  /**
    * The name of the property to set the time interval to log the
    * conversion time
    */
  val PERIODIC_LOG_MESSAGE_TIME_PROPNAME = "org.eso.ias.test.times.logtime"

  /**
    * The default time interval (minutes) to log the average converion time
    */
  val PERIODIC_LOG_MESSAGE_TIME_DEFAULT = 1

  /**
    * The period to log the average time
    */
  val PERIODIC_LOG_MESSAGE_TIME: Long = Long.unbox(java.lang.Long.getLong(PERIODIC_LOG_MESSAGE_TIME_PROPNAME,PERIODIC_LOG_MESSAGE_TIME_DEFAULT))

  /** The ID of the kafka consumer */
  val id: String = CalcConversionTime.getClass.getName

  def main(args: Array[String]): Unit = {

    val calculator = new CalcConversionTime(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS)
    calculator.start()

    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = calculator.shutdown()
    }))

    Try(Thread.sleep(Long.MaxValue))
    logger.info("Done")
  }
}
