package org.eso.ias.converter.corepublisher;

import java.util.Objects;

import org.eso.ias.prototype.input.java.IASValue;
import org.eso.ias.prototype.input.java.IasValueSerializerException;
import org.eso.ias.prototype.input.java.IasValueStringSerializer;

/**
 * An implementation of the {@link CoreFeeder} that 
 * publishes values to a listener.
 * <P> 
 * This implementation allows custom implementations of publishers
 * and is useful for testing
 * 
 * @author acaproni
 */
public class ListenerPublisher implements CoreFeeder {
	
	/**
	 * The interface to get the values to be sent 
	 * to the core of the IAS
	 * 
	 * @author acaproni
	 *
	 */
	public interface CoreFeederListener {
		
		/**
		 * Returns the string representation
		 * of the object to be sent to the core of the IAS.
		 * <P>
		 * This string is what the converter really 
		 * publishes in the IASIO queue
		 * 
		 * @param strRepresentation The string to be sent to the core of the IAS
		 */
		public void rawDataPublished(String strRepresentation);
		
		/**
		 * The value to be sent to the core of the IAS, after being
		 * serialized in a string.
		 * 
		 * @param iasValue the value to be sent to the IAS
		 */
		public void dataPublished(IASValue<?> iasValue);
	}
	
	/**
	 * The listener of events to be sent to the core of the IAS
	 */
	private final CoreFeederListener listener;
	
	/**
	 * The serializer to convert IasValue to/from strings
	 */
	private final IasValueStringSerializer valueSerializer;
	
	/**
	 * Constructor
	 * 
	 * @param listener The listener of events to be sent to the core of the IAS
	 */
	public ListenerPublisher(CoreFeederListener listener, IasValueStringSerializer valueSerializer) {
		Objects.requireNonNull(listener);
		this.listener=listener;
		Objects.requireNonNull(valueSerializer);
		this.valueSerializer=valueSerializer;
	}
	
	/**
	 * Send the passed value to the core of the IAS for processing
	 * 
	 * @param iasValue The not <code>null</code> to send to the core of the IAS
	 * @throws CoreFeederException in case of error pushing the value
	 * @see CoreFeeder
	 */
	@Override
	public void push(IASValue<?> iasValue) throws CoreFeederException {
		listener.dataPublished(iasValue);
		try {
			listener.rawDataPublished(valueSerializer.iasValueToString(iasValue));
		} catch (IasValueSerializerException ivse) {
			throw new CoreFeederException("Error publishing "+iasValue.id,ivse);
		}
	}

}
