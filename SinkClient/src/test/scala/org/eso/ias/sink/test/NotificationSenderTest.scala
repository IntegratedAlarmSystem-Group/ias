package org.eso.ias.sink.test

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.{IasDao, IasioDao}
import org.eso.ias.cdb.structuredtext.json.{CdbTxtFiles, JsonReader}
import org.eso.ias.logging.IASLogger
import org.eso.ias.sink.ValueListener
import org.eso.ias.sink.email.{AlarmState, AlarmStateTracker, NotificationsSender, Sender}
import org.eso.ias.types.*
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.FileSystems
import java.time.{Duration, ZoneOffset, ZonedDateTime}
import java.util.concurrent.{CountDownLatch, TimeUnit}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.javaapi.CollectionConverters

// The following import is required by the usage of the fixture
import scala.language.reflectiveCalls

/** Sender mockup */
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
class NotificationSenderTest extends AnyFlatSpec {
  /** The logger */
  private val logger = IASLogger.getLogger(classOf[NotificationSenderTest])

  /** Fixture to build same type of objects for the tests */
  trait Fixture {
    // Build the CDB reader
    val cdbParentPath = FileSystems.getDefault().getPath("src/test");
    val cdbFiles = new CdbTxtFiles(cdbParentPath)
    val cdbReader: CdbReader = new JsonReader(cdbFiles)
    cdbReader.init()

    val iasiosDaos: Set[IasioDao] = {
      val iasiosDaoJOpt = cdbReader.getIasios()
      assert(iasiosDaoJOpt.isPresent, "Error getting the IASIOs from the CDB")
      CollectionConverters.asScala(iasiosDaoJOpt.get()).toSet
    }

    val iasDao: IasDao = {
      val iasDaoJOpt=cdbReader.getIas
      assert(iasDaoJOpt.isPresent,"IAS config not found in CDB")
      iasDaoJOpt.get()
    }
    cdbReader.shutdown()

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
      tStamp+1,
      tStamp+5,
      tStamp+10,
      tStamp+15,
      null,
      null,
      null,
      null)
  }

  behavior of "The notification sender"

  it must "read the times from the defaults" in new Fixture {
    assert(notificationsSender.timeIntervalToSendEmails==1440)
    assert(notificationsSender.timeTostart==(8,0))
  }

  it must "Get the list of alarms to notify from CDB after init" in new Fixture {
    logger.debug("Started")
    valueListener.setUp(iasDao,iasioDaosMap)

    val temp = iasioDaosMap.values.filter(i => Option(i.getEmails).isDefined && i.getEmails.nonEmpty)
    logger.debug("IASIOS in map with emails = {}",temp.map(t => t.getId).mkString(","))

    logger.debug("keys in alarmsToTrack= {}",notificationsSender.alarmsToTrack.keySet.mkString(","))

    assert(notificationsSender.alarmsToTrack.keys.size==4)
    assert(notificationsSender.alarmsToTrack.keySet.contains("Dasu1-OutID"))
    assert(notificationsSender.alarmsToTrack.keySet.contains("Dasu2-OutID"))
    assert(notificationsSender.alarmsToTrack.keySet.contains("Dasu3-OutID"))
    assert(notificationsSender.alarmsToTrack.keySet.contains("TemplatedId"))

    val alarmsForRec1 = notificationsSender.alarmsForUser("recp1@web.site.org")
    assert(alarmsForRec1.toSet==List("Dasu1-OutID","Dasu3-OutID").toSet)
    val alarmsForRec2 = notificationsSender.alarmsForUser("recp2@web.site.org")
    assert(alarmsForRec2.toSet==List("Dasu2-OutID").toSet)
    val alarmsForRec3 = notificationsSender.alarmsForUser("recp3@web.site.org")
    assert(alarmsForRec3.toSet==List("Dasu3-OutID").toSet)
    val alarmsForRec4 = notificationsSender.alarmsForUser("template@web.site.org")
    assert(alarmsForRec4.toSet==List("TemplatedId").toSet)

    valueListener.tearDown()
    logger.debug("Done")
  }

  it must "not send notification for unrecognized alarms" in new Fixture {
    valueListener.setUp(iasDao,iasioDaosMap)

    val alarm = buildValue("TheID",Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    notificationsSender.processIasValues(List(alarm))

    assert(sender.notifications.isEmpty)

    valueListener.tearDown()
  }

  it must "send notification for alarm being SET" in new Fixture {
    valueListener.setUp(iasDao,iasioDaosMap)

    val alarm = buildValue("Dasu2-OutID",Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    notificationsSender.processIasValues(List(alarm))

    assert(sender.notifications.length==1)

    val notification = sender.notifications(0)
    assert(notification.alarmId=="Dasu2-OutID")
    assert(notification.alarmState.alarm==Alarm.SET_CRITICAL)
    assert(notification.alarmState.validity==IasValidity.RELIABLE)
    assert(notification.recipients==List("recp2@web.site.org"))

    valueListener.tearDown()
  }


  it must "send notification for templated alarm being SET" in new Fixture {
    valueListener.setUp(iasDao,iasioDaosMap)

    val templatedId = Identifier.buildIdFromTemplate("TemplatedId",7)

    val alarm = buildValue(templatedId,Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    notificationsSender.processIasValues(List(alarm))

    assert(sender.notifications.length==1)

    val notification = sender.notifications(0)
    assert(notification.alarmId==templatedId)
    assert(notification.alarmState.alarm==Alarm.SET_CRITICAL)
    assert(notification.alarmState.validity==IasValidity.RELIABLE)
    assert(notification.recipients==List("template@web.site.org"))

    valueListener.tearDown()
  }


  it must "not send notification if the state did not change" in new Fixture {
    valueListener.setUp(iasDao,iasioDaosMap)

    val alarm = buildValue("Dasu2-OutID",Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    notificationsSender.processIasValues(List(alarm))
    notificationsSender.processIasValues(List(alarm))
    notificationsSender.processIasValues(List(alarm))
    notificationsSender.processIasValues(List(alarm))
    notificationsSender.processIasValues(List(alarm))

    assert(sender.notifications.length==1)

    val notification = sender.notifications(0)
    assert(notification.alarmId=="Dasu2-OutID")
    assert(notification.alarmState.alarm==Alarm.SET_CRITICAL)
    assert(notification.alarmState.validity==IasValidity.RELIABLE)
    assert(notification.recipients==List("recp2@web.site.org"))

    valueListener.tearDown()
  }

  it must "not send notifications if the state changes many times" in new Fixture {
    valueListener.setUp(iasDao,iasioDaosMap)

    val alarm1 = buildValue("Dasu2-OutID",Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    val alarm2 = buildValue("Dasu2-OutID",Alarm.SET_LOW,IasValidity.UNRELIABLE,System.currentTimeMillis())
    val alarm3 = buildValue("Dasu2-OutID",Alarm.CLEARED,IasValidity.UNRELIABLE,System.currentTimeMillis())
    val alarm4 = buildValue("Dasu2-OutID",Alarm.SET_MEDIUM,IasValidity.RELIABLE,System.currentTimeMillis())
    notificationsSender.processIasValues(List(alarm1,alarm2,alarm3,alarm4))

    assert(sender.notifications.length==4)

    valueListener.tearDown()
  }

  it must "not send notifications if the state remains the same but validity changes" in new Fixture {
    valueListener.setUp(iasDao,iasioDaosMap)

    val alarm1 = buildValue("Dasu2-OutID",Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    val alarm2 = buildValue("Dasu2-OutID",Alarm.SET_CRITICAL,IasValidity.UNRELIABLE,System.currentTimeMillis())
    val alarm3 = buildValue("Dasu2-OutID",Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    val alarm4 = buildValue("Dasu2-OutID",Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    notificationsSender.processIasValues(List(alarm1,alarm2,alarm3,alarm4))

    assert(sender.notifications.length==1)

    valueListener.tearDown()
  }

  it must "send notifications to all the recipients" in new Fixture {
    valueListener.setUp(iasDao,iasioDaosMap)

    val alarm = buildValue("Dasu3-OutID", Alarm.SET_CRITICAL,IasValidity.RELIABLE,System.currentTimeMillis())
    notificationsSender.processIasValues(List(alarm))

    assert(sender.notifications.length==1)

    val notification = sender.notifications(0)
    assert(notification.alarmId=="Dasu3-OutID")
    assert(notification.alarmState.alarm==Alarm.SET_CRITICAL)
    assert(notification.alarmState.validity==IasValidity.RELIABLE)
    assert(notification.recipients.toSet==Set("recp1@web.site.org", "recp3@web.site.org"))

    valueListener.tearDown()
  }

  it must "periodically send digests" in {

    // We cannot use the Fixture in the name of the test message as in the other cases
    // // because it is built before entering the body of the test so it is not possible to customize
    // the time interval to send digests neither the starting time of the sender
    //
    // To avoid this problem, this test builds the fixture in the body of the test, after
    // setting the properties

    val localNow: ZonedDateTime  = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)
    val nextZone: ZonedDateTime = localNow.plus(Duration.ZERO.plusMinutes(2))
    val hour = nextZone.getHour
    val minute = nextZone.getMinute
    logger.info("Scheduling NotificationSender start time at {}:{} UTC",hour,minute)
    System.getProperties.put(NotificationsSender.StartTimeOfPeriodicNotificationsPropName,s"$hour:$minute")
    System.getProperties.put(NotificationsSender.SendEmailsTimeIntervalPropName,"1")

    logger.info("Properties set by the test: {}={}, {}={}",
      NotificationsSender.StartTimeOfPeriodicNotificationsPropName,
      System.getProperties.get(NotificationsSender.StartTimeOfPeriodicNotificationsPropName),
      NotificationsSender.SendEmailsTimeIntervalPropName,
      System.getProperties.get(NotificationsSender.SendEmailsTimeIntervalPropName))

    // Build the fixture after setting the properties
    class TestFixture extends Fixture
    val f = new TestFixture

    assert(f.notificationsSender.timeIntervalToSendEmails==1,"Time mismatch")

    f.valueListener.setUp(f.iasDao, f.iasioDaosMap)

    logger.info("Waiting for reception of 2 digests")
    val ret = f.sender.countDownLatch.await(4,TimeUnit.MINUTES)
    f.valueListener.tearDown()

    logger.debug("Digest sents to recipeients: {}", f.sender.digests.map(d => d.recipient).mkString(","))

    f.sender.digests.foreach(d => {
      logger.debug("Digest sent to {}, for alarms {}",d.recipient,d.alarmStates.map(_.id).mkString(","))
    })

    assert(f.sender.digests.length>=2,s"Not all expected events has been received: ${f.sender.digests.length} out of 2")

  }
}
