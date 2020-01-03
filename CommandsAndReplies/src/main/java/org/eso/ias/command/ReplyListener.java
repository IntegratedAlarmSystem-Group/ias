package org.eso.ias.command;

/**
 * The listener of replies
 */
public interface ReplyListener {

    /**
     * Invoked when a new reply has been produced
     *
     * @param reply The not-null reply read from the topic
     */
    public void  newReply(ReplyMessage reply);

}
