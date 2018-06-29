package org.eso.ias.sink.test

import java.nio.file.FileSystems
import java.time.temporal.TemporalUnit
import java.time.{Duration, ZoneOffset, ZonedDateTime}
import java.util.concurrent.{CountDownLatch, TimeUnit}

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.{CdbJsonFiles, JsonReader}
import org.eso.ias.cdb.pojos.{IasDao, IasioDao}
import org.eso.ias.logging.IASLogger
import org.eso.ias.sink.ValueListener
import org.eso.ias.sink.email.{AlarmState, AlarmStateTracker, NotificationsSender, Sender}
import org.eso.ias.types.IasValidity.UNRELIABLE
import org.eso.ias.types._
import org.scalatest.FlatSpec

import scala.collection.{JavaConverters, mutable}
import scala.collection.mutable.ListBuffer

// The following import is required by the usage of the fixture
import language.reflectiveCalls

class SenderTest extends Sender {

  /** The type of notification to send */
  class Notification(val recipients: List[String], val alarmId: String, val alarmState: AlarmState)

  /** The logger */
  private val logger = IASLogger.getLogger(classOf[SenderTest])

  /** The notifications sent */
  val notifications: ListBuffer[Notification] = ListBuffer()

  /** The type of the digest to send */
  class Digest(val recipient: String, val alarmStates: List[AlarmStateTracker])

  /** The digests sent */
  val digests: ListBuffer[Digest] = ListBuffer()

  /** The semaphore to detect to periodic notification */
  val countDownLatch = new CountDownLatch(2)


  /**
    * Notify the recipients of the state changes of the passed alarms
    *
    * @param recipient  the recipient to notify
    * @param alarmStates the states of the alarms to notify
    */
  override def digestNotify(recipient: String, alarmStates: List[AlarmStateTracker]): Unit = {
    require(Option(recipient).isDefined && recipient.nonEmpty)
    require(Option(alarmStates).isDefined && alarmStates.nonEmpty)
    logger.info("Sending digest of {} alarms to {}",alarmStates.length.toString,recipient)
    digests.append(new Digest(recipient,alarmStates))
    countDownLatch.countDown()
  }

  /**
    * Send the notification to notify that an alarm has been set or cleared
    *
    * @param recipients the recipients to notify
    * @param alarmId    the ID of the alarm
    * @param alarmState the state to notify
    */
  override def notify(recipients: List[String], alarmId: String, alarmState: AlarmState): Unit = {
    require(Option(recipients).isDefined && recipients.nonEmpty)
    require(Option(alarmId).isDefined && !alarmId.trim.isEmpty)
    require(Option(alarmState).isDefined)
    logger.info("Sending notification of alarm {} to {}",alarmId,recipients.mkString(","))
    notifications.append(new Notification(recipients,alarmId,alarmState))
  }
}

/**
  * Test the NotificationSender
  */
class NotificationSenderTest extends FlatSpec {
  /** The logger */
  private val logger = IASLogger.getLogger(classOf[NotificationSenderTest])

  /** Fixture to build same type of objects for the tests */
  def fixture =
    new {
      // Build the CDB reader
      val cdbParentPath = FileSystems.getDefault().getPath(".");
      val cdbFiles = new CdbJsonFiles(cdbParentPath)
      val cdbReader: CdbReader = new JsonReader(cdbFiles)

      val iasiosDaos: Set[IasioDao] = {
        val iasiosDaoJOpt = cdbReader.getIasios()
        assert(iasiosDaoJOpt.isPresent, "Error getting the IASIOs from the CDB")
        JavaConverters.asScalaSet(iasiosDaoJOpt.get()).toSet
      }

      val iasDao: IasDao = {
        val iasDaoJOpt=cdbReader.getIas
        assert(iasDaoJOpt.isPresent,"IAS config not found in CDB")
        iasDaoJOpt.get()
      }

      val iasioDaosMap: Map[String, IasioDao] = {
        val mutableMap: mutable.Map[String, IasioDao] = mutable.Map.empty
        iasiosDaos.foreach(iDao => mutableMap(iDao.getId)=iDao)
        mutableMap.toMap
      }

      val notificationSenderId = "NotSendID"

      val sender = new SenderTest

      val notificationsSender: NotificationsSender = new NotificationsSender(notificationSenderId,sender)

      val valueListener: ValueListener = notificationsSender
    }

  /**
    * Build a IASValue to send to the processor
    *
    * @param id The identifier of the monitor point
    * @param d the alarm value
    * @param validity the validity
    * @param tStamp plugin production time stamp
    * @return the IASValue
    */
  def buildValue(id: String, d: Alarm, validity: IasValidity, tStamp: Long): IASValue[_] = {

    // The identifier of the monitored system
    val monSysId = new Identifier("ConverterID",IdentifierType.MONITORED_SOFTWARE_SYSTEM,None)

    // The identifier of the plugin
    val pluginId = new Identifier("ConverterID",IdentifierType.PLUGIN,Some(monSysId))

    // The identifier of the converter
    val converterId = new Identifier("ConverterID",IdentifierType.CONVERTER,Some(pluginId))

    // The ID of the monitor point
    val inputId = new Identifier(id, IdentifierType.IASIO,converterId)

    IASValue.build(
      d,
      OperationalMode.OPERATIONAL,
      validity,
      inputId.fullRunningID,
      IASTypes.ALARM,
      tStamp,
      tStamp+5,
      tStamp+10,
      tStamp+15,
      tStamp+20,
      null,
      null,
      null,
      null)
  }

  behavior of "The notification sender"

  it must "read the times from the defaults" in {
    val f = fixture
    assert(f.notificationsSender.timeIntervalToSendEmails==1440)
    assert(f.notificationsSender.timeTostart==(8,0))
  }

  it must "Get the list of alarms to notify from CDB after init" in {
    val f = fixture
    f.valueListener.setUp(f.iasDao,f.iasioDaosMap)

    assert(f.notificationsSender.alarmsToTrack.keys.size==3)
    assert(f.notificationsSender.alarmsToTrack.keySet.contains("Dasu1-OutID"))
    assert(f.notificationsSender.alarmsToTrack.keySet.contains("Dasu2-OutID"))
    assert(f.notificationsSender.alarmsToTrack.keySet.contains("Dasu3-OutID"))

    val alarmsForRec1 = f.notificationsSender.alarmsForUser("recp1@web.site.org")
    assert(alarmsForRec1.toSet==List("Dasu1-OutID","Dasu3-OutID").toSet)
    val alarmsForRec2 = f.notificationsSender.alarmsForUser("recp2@web.site.org")
    assert(alarmsForRec2.toSet==List("Dasu2-OutID").toSet)
    val alarmsForRec3 = f.notificationsSender.alarmsForUser("recp3@web.site.org")
    assert(alarmsForRec3.toSet==List("Dasu3-OutID").toSet)

    f.valueListener.tearDown()
  }

  it must "not send notification for unrecognized alarms" in {
    val f = fixture
    f.valueListener.setUp(f.iasDao,f.iasioDaosMap)

    val alarm = buildValue("TheID",Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    f.notificationsSender.processIasValues(List(alarm))

    assert(f.sender.notifications.isEmpty)

    f.valueListener.tearDown()
  }

  it must "send notification for alarm being SET" in {
    val f = fixture
    f.valueListener.setUp(f.iasDao,f.iasioDaosMap)

    val alarm = buildValue("Dasu2-OutID",Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    f.notificationsSender.processIasValues(List(alarm))

    assert(f.sender.notifications.length==1)

    val notification = f.sender.notifications(0)
    assert(notification.alarmId=="Dasu2-OutID")
    assert(notification.alarmState.alarm==Alarm.SET_CRITICAL)
    assert(notification.alarmState.validity==IasValidity.RELIABLE)
    assert(notification.recipients==List("recp2@web.site.org"))

    f.valueListener.tearDown()
  }

  it must "not send notification if the state did not change" in {
    val f = fixture
    f.valueListener.setUp(f.iasDao,f.iasioDaosMap)

    val alarm = buildValue("Dasu2-OutID",Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    f.notificationsSender.processIasValues(List(alarm))
    f.notificationsSender.processIasValues(List(alarm))
    f.notificationsSender.processIasValues(List(alarm))
    f.notificationsSender.processIasValues(List(alarm))
    f.notificationsSender.processIasValues(List(alarm))

    assert(f.sender.notifications.length==1)

    val notification = f.sender.notifications(0)
    assert(notification.alarmId=="Dasu2-OutID")
    assert(notification.alarmState.alarm==Alarm.SET_CRITICAL)
    assert(notification.alarmState.validity==IasValidity.RELIABLE)
    assert(notification.recipients==List("recp2@web.site.org"))

    f.valueListener.tearDown()
  }

  it must "not send notifications if the state changes many times" in {
    val f = fixture
    f.valueListener.setUp(f.iasDao,f.iasioDaosMap)

    val alarm1 = buildValue("Dasu2-OutID",Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    val alarm2 = buildValue("Dasu2-OutID",Alarm.SET_LOW,IasValidity.UNRELIABLE,System.currentTimeMillis())
    val alarm3 = buildValue("Dasu2-OutID",Alarm.CLEARED,IasValidity.UNRELIABLE,System.currentTimeMillis())
    val alarm4 = buildValue("Dasu2-OutID",Alarm.SET_MEDIUM,IasValidity.RELIABLE,System.currentTimeMillis())
    f.notificationsSender.processIasValues(List(alarm1,alarm2,alarm3,alarm4))

    assert(f.sender.notifications.length==4)

    f.valueListener.tearDown()
  }

  it must "not send notifications if the state remains the same but validity changes" in {
    val f = fixture
    f.valueListener.setUp(f.iasDao,f.iasioDaosMap)

    val alarm1 = buildValue("Dasu2-OutID",Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    val alarm2 = buildValue("Dasu2-OutID",Alarm.SET_CRITICAL,IasValidity.UNRELIABLE,System.currentTimeMillis())
    val alarm3 = buildValue("Dasu2-OutID",Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    val alarm4 = buildValue("Dasu2-OutID",Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    f.notificationsSender.processIasValues(List(alarm1,alarm2,alarm3,alarm4))

    assert(f.sender.notifications.length==1)

    f.valueListener.tearDown()
  }

  it must "send notificatiomns to all the recipients" in {
    val f = fixture
    f.valueListener.setUp(f.iasDao,f.iasioDaosMap)

    val alarm = buildValue("Dasu3-OutID", Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    f.notificationsSender.processIasValues(List(alarm))

    assert(f.sender.notifications.length==1)

    val notification = f.sender.notifications(0)
    assert(notification.alarmId=="Dasu3-OutID")
    assert(notification.alarmState.alarm==Alarm.SET_CRITICAL)
    assert(notification.alarmState.validity==IasValidity.RELIABLE)
    assert(notification.recipients.toSet==Set("recp1@web.site.org", "recp3@web.site.org"))

    f.valueListener.tearDown()
  }

  it must "periodically send digests" in {

    val localNow: ZonedDateTime  = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)
    val nextZone: ZonedDateTime = localNow.plus(Duration.ZERO.plusMinutes(2))
    val hour = nextZone.getHour
    val minute = nextZone.getMinute
    logger.info("Scheduling NotificationSender start time at {}:{} UTC",hour,minute)
    System.getProperties.put(NotificationsSender.StartTimeOfPeriodicNotificationsPropName,s"$hour:$minute")
    System.getProperties.put(NotificationsSender.SendEmailsTimeIntervalPropName,"1")
    val f = fixture
    f.valueListener.setUp(f.iasDao,f.iasioDaosMap)

    logger.info("Waiting for reception of 2 digests")
    val ret = f.sender.countDownLatch.await(4,TimeUnit.MINUTES)
    f.valueListener.tearDown()
    assert(f.sender.digests.length>=2,s"Not all expected events has been received: ${f.sender.digests.length} out of 2")

  }


}
