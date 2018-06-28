package org.eso.ias.sink.email

import java.util.{Date, Properties}

import org.eso.ias.logging.IASLogger
import com.typesafe.scalalogging.Logger
import javax.mail.internet.MimeMessage
import javax.mail.{Address, Message, Session, Transport}

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


  private def sendMessage(recipients: String, subject: String, body: String): Unit = {
    val smtpPropName="mail.smtp.host"
    val props: Properties = System.getProperties()
    if (!props.contains(smtpPropName)) {
      props.put(smtpPropName,server)
    }
    val session: Session = Session.getInstance(props, null)

    val mimeMsg: MimeMessage = new MimeMessage(session)
    mimeMsg.addHeader("Content-type", "text/plain; charset=UTF-8")
    mimeMsg.addHeader("format", "flowed")
    mimeMsg.addHeader("Content-Transfer-Encoding", "8bit")
    import javax.mail.internet.InternetAddress
    mimeMsg.setFrom(new InternetAddress("no_reply@ias.org", "NoReply-IAS"))

    val replyToAddress: Array[Address] = InternetAddress.parse("no_reply@ias.org", false).asInstanceOf[Array[Address]]
    mimeMsg.setReplyTo(replyToAddress)

    mimeMsg.setSubject(subject, "UTF-8")
    mimeMsg.setText(body, "UTF-8")
    mimeMsg.setSentDate(new Date())
    mimeMsg.setRecipients(Message.RecipientType.TO, recipients)

    val tr: Transport = session.getTransport("smtp");
    if (loginName.isDefined) tr.connect(server, loginName.get , pswd.get)
    else tr.connect(server,null,null)
    mimeMsg.saveChanges();      // don't forget this
    tr.sendMessage(mimeMsg, mimeMsg.getAllRecipients());
    tr.close();
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
  }

  /**
    * Send the notification to notify that an alarm has been set or cleared
    *
    * @param recipients the recipients to notify
    * @param alarmId the ID of the alarm
    * @param alarmState the state to notify
    */
  override def notify(recipients: List[String], alarmId: String, alarmState: AlarmState) = {
    require(Option(recipients).isDefined && recipients.nonEmpty,"No recipients given")
    require(Option(alarmId).isDefined && !alarmId.trim.isEmpty,"Invalid alarm ID")
    require(Option(alarmState).isDefined,"No alarm state to notify")
  }
}

object SmtpSender {
  def main(args: Array[String]): Unit = {
    val server="smtp-internal.osf.alma.cl"
    val user =""
    val paswd=""

    val smtpSender = new SmtpSender(server,None,None)


  }
}
