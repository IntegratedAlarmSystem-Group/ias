package org.eso.ias.sink.email

import java.util.{Date, Properties}

import org.eso.ias.logging.IASLogger
import com.typesafe.scalalogging.Logger
import javax.mail.internet.MimeMessage
import javax.mail.{Address, Message, Session, Transport}
import org.eso.ias.types.Alarm
import org.eso.ias.utils.ISO8601Helper



/**
  * The sender of notifications by emails.
  *
  * @param server The SMTP server to send notifications to the receipients
  * @param loginName the optional login name to pass to the server
  * @param pswd the optional passowrd to pass to the server
  */
class SmtpSender(val server: String, val loginName: Option[String], val pswd: Option[String]) extends Sender {

   /** The logger */
  private val logger: Logger = IASLogger.getLogger(classOf[SmtpSender])

  logger.info("SMTP server {}, user name {}, password =*****",server,loginName)

  /**
    * Sends the email
    *
    * @param recipients the receipints of the message
    * @param subject the subsject
    * @param body the text of the email
    */
  private def sendMessage(recipients: String, subject: String, body: String): Unit = {
    logger.debug("Sending a message with title '{}' to {}",subject,recipients)
    val smtpPropName="mail.smtp.host"
    val props: Properties = System.getProperties
    if (!props.contains(smtpPropName)) {
      props.put(smtpPropName,server)
    }
    val session: Session = Session.getInstance(props, null)

    val mimeMsg: MimeMessage = new MimeMessage(session)
    mimeMsg.addHeader("Content-type", "text/plain; charset=UTF-8")
    mimeMsg.addHeader("format", "flowed")
    mimeMsg.addHeader("Content-Transfer-Encoding", "8bit")
    import javax.mail.internet.InternetAddress
    mimeMsg.setFrom(new InternetAddress("no_reply@ias.org", "IAS notification system"))

    val replyToAddress: Array[Address] = InternetAddress.parse("no_reply@ias.org", false).asInstanceOf[Array[Address]]
    mimeMsg.setReplyTo(replyToAddress)

    mimeMsg.setSubject(subject, "UTF-8")
    mimeMsg.setText(body, "UTF-8")
    mimeMsg.setSentDate(new Date())
    mimeMsg.setRecipients(Message.RecipientType.TO, recipients)

    val tr: Transport = session.getTransport("smtp")
    if (loginName.isDefined) tr.connect(server, loginName.get , pswd.get)
    else tr.connect(server,null,null)
    mimeMsg.saveChanges();      // don't forget this
    tr.sendMessage(mimeMsg, mimeMsg.getAllRecipients);
    tr.close()
    logger.debug("Message sent to {}",recipients)
  }


  /**
    * Fromat the alarm state and th epriority (if the alarm is not cleared)
    * in a human readble
    * @param alarmState the alarm state
    * @return the activation state and the priority
    */
  private def formatAlarmSetAndPriority(alarmState: AlarmState): (String, String) = {
    if (alarmState.alarm==Alarm.CLEARED) {
      (Alarm.CLEARED.name(),"")
    } else {
      val p = alarmState.alarm.name().split("_")(1)
      ("SET",s"priority $p")
    }
  }


  /**
    * Notify the recipients with the summary of the state changes of the passed alarms
    *
    * @param recipient   the recipient to notify
    * @param alarmStates the states of the alarms to notify
    */
  override def digestNotify(recipient: String, alarmStates: List[AlarmStateTracker]): Unit = {
    require(Option(recipient).isDefined && recipient.nonEmpty,"No recipients given")
    require(Option(alarmStates).isDefined && alarmStates.nonEmpty,"No history to send")

    val subject = "Alarm digest"

    val text = new StringBuilder("Summary of the alarms changes since last notification\n")
    alarmStates.sortWith(_.id < _.id).foreach( state => {
      text.append(s"\n${state.id}")
      text.append("\n")
      state.stateChanges.sortWith(_.timestamp < _.timestamp).foreach(change => {
        val tStamp = ISO8601Helper.getTimestamp(change.timestamp)
        val (alarmActivation, priority)= formatAlarmSetAndPriority(change)
        text.append(s"\n\t* $tStamp $alarmActivation $priority (${change.validity.toString})")
      })
    })

    sendMessage(recipient,subject,text.toString())

  }

  /**
    * Send the notification to notify that an alarm has been set or cleared
    *
    * @param recipients the recipients to notify
    * @param alarmId the ID of the alarm
    * @param alarmState the state to notify
    */
  override def notify(recipients: List[String], alarmId: String, alarmState: AlarmState): Unit = {
    require(Option(recipients).isDefined && recipients.nonEmpty,"No recipients given")
    require(Option(alarmId).isDefined && !alarmId.trim.isEmpty,"Invalid alarm ID")
    require(Option(alarmState).isDefined,"No alarm state to notify")

    val (alarmActivation, priority)= formatAlarmSetAndPriority(alarmState)
    val time = ISO8601Helper.getTimestamp(alarmState.timestamp)
    val subject = s"Alarm notification  $alarmId: $alarmActivation $priority"
    val text = s"Notification of alarm state change\n\n* $alarmId\n\t* $alarmActivation $priority\n\t*$time\n\t*${alarmState.validity.toString}"
    val to=recipients.mkString(",")
    sendMessage(to,subject, text)
  }
}

