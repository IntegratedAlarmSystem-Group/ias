package org.eso.ias.command.kafka;

import org.eso.ias.command.ReplyJsonSerializer;
import org.eso.ias.command.ReplyListener;
import org.eso.ias.command.ReplyMessage;
import org.eso.ias.command.ReplyStringSerializer;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaUtilsException;
import org.eso.ias.kafkautils.SimpleStringConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An helper class to easily consume replies from the kafka reply topic.
 *
 * The object must be initialized invoking {@link #setUp()} before starting to get replies.
 * Once terminated, {@link #tearDown()} must be invoked.
 */
public class ReplyKafkaConsumer
        extends SimpleStringConsumer
implements SimpleStringConsumer.KafkaConsumerListener {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(ReplyKafkaConsumer.class);

    /** The object to de-serialize replies from JSON */
    private static final ReplyStringSerializer repliesSerializer = new ReplyJsonSerializer();

    /** The listener to be notified of incoming replies */
    private ReplyListener replyListener;

    /**
     * Constructor
     *
     * @param brokers Kafka brokers
     * @param replyConsumerId The id of the consumer
     */
    public ReplyKafkaConsumer(String brokers, String replyConsumerId) {
        super(brokers, KafkaHelper.REPLY_TOPIC_NAME,replyConsumerId);
    }

    /**
     * A string has bene received from the reply topic
     *
     * @param event The string received in the topic
     * @see org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener
     */
    @Override
    public void stringEventReceived(String event) {
       if (event==null || event.isEmpty()) {
           logger.warn("Empty or null string received from the kafka reply topic");
           return;
       }
       logger.debug("New event received [{}]",event);
       ReplyMessage reply;
       try {
           reply = repliesSerializer.valueOf(event);
       } catch (Exception e) {
           logger.error("Error deserializing string [{}]: reply lost",event);
           return;
       }
       try {
           logger.debug("Notifying the listener of a new reply....");
           replyListener.newReply(reply);
           logger.info("Reply processed by the listener");
       } catch (Exception e) {
           logger.error("Error from the listener while processing a reply",e);
       }
    }

    /**
     * Start getting events from the repluy topic
     *
     * @param startReadingFrom Starting position in the kafka partition
     * @param listener The listener of events published in the topic
     * @throws KafkaUtilsException in case of timeout subscribing to the kafkatopic
     */
    public synchronized void startGettingReplies(StreamPosition startReadingFrom, ReplyListener listener)
	throws KafkaUtilsException {
        System.out.println("\n\n===> reply listener startGettingEvents for topic "+topicName);
        replyListener=listener;
	    super.startGettingStrings(startReadingFrom,this);
	}
}

