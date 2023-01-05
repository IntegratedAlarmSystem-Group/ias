package org.eso.ias.types;

/** 
 * The state and state machine of an Alarm
 */
public enum AlarmState {

    /** Alarm clear and acknowledged */
	CLEAR_ACK,

	/** Alarm clear and unacknowledged */
	CLEAR_UNACK,

	/** Alarm set and unacknowledged */
	SET_UNACK,

	/** Alarm set and acknowledged */
	SET_ACK;
	
	/**
	 * 
	 * @return <code>true</code> if the alarm is set;
	 *         <code>false</code> otherwise
	 */
	public final boolean isSet() {
		return this==SET_ACK || this==SET_UNACK;
	}

	/**
	 *
	 * @return true if the alarm has been acknowledged
	 * 		   false otherwise
	 */
	public final boolean isAcked() {
		return this==SET_ACK || this==CLEAR_ACK;
	}

    /** The alarm has been Acked (by operator) */
    public AlarmState ack() {
		switch (this) {
			case SET_ACK:
			case CLEAR_ACK: return this;
			case SET_UNACK: return SET_ACK;
			case CLEAR_UNACK: return CLEAR_ACK;
			default: throw new UnsupportedOperationException("Unknown alarm state "+this);
		}
	}

    /** The alarm is set (by the TF) */
    public AlarmState set() {
		switch (this) {
			case SET_ACK:
			case SET_UNACK: return this;
			case CLEAR_ACK:
			case CLEAR_UNACK: return SET_UNACK;
			default: throw new UnsupportedOperationException("Unknown alarm state "+this);
		}
	}

    /** The alarm is clear (by the TF) */
    public AlarmState clear() {
		switch (this) {
			case CLEAR_ACK:
			case CLEAR_UNACK: return this;
			case SET_ACK: return CLEAR_ACK;
			case SET_UNACK: return CLEAR_UNACK;
			default: throw new UnsupportedOperationException("Unknown alarm state "+this);
		}
	}
}
