package org.eso.ias.types;

import java.util.Objects;
import java.util.Optional;

/**
 * <code>Alarm</code> represents an alarm generated by a remote monitored system.
 *
 * An alarm has
 * - a state, {@link AlarmState}, of set/clear plus acknowledgement;
 * - a priority, {@link Priority}.
 *
 * The alarm corresponding to the initial state is returned by the empty constructor;
 * all other alarm states can be reached by applying methods to the start state or other alarm states.
 *
 * For a description of the state machine,
 * see the documentation of <A href="https://github.com/IntegratedAlarmSystem-Group/ias/issues/167">Issue #167 on github</A>.
 *
 * <P>An Alarm is immutable</P>
 * 
 * @author acaproni
 *
 */
public class Alarm {

	/** The state of the alarm */
	public final AlarmState alarmState;

	/** The priority of the alarm */
	public final Priority priority;

	/**
	 * Build and return the alarm that corresponds to the start state
	 * of the alarm state machine (i.e. cleared, and unacked) with default priority.
	 *
	 * @return the alarm of the start state of the state machine
	 */
	public static Alarm getInitialAlarmState() {
		return new Alarm(AlarmState.CLEAR_UNACK, Priority.getDefaultPriority());
	}

	/**
	 * Build and return the alarm that corresponds to the start state
	 * of the alarm state machine (i.e. cleared, and unacked) with the passed priority.
	 *
	 * @param priority the priority of the alarm
	 * @return the alarm of the start state of the state machine
	 */
	public static Alarm getInitialAlarmState(Priority priority) {
		return new Alarm(AlarmState.CLEAR_UNACK, priority);
	}

	/**
	 * Build an alarm with the given priority and state.
	 *
	 * @param state the state of the alarm
	 * @param priority the priority of the alarm
	 */
	private Alarm(AlarmState state, Priority priority) {
		this.alarmState=state;
		this.priority=priority;
	}

	@Override
	public int hashCode() {
		return Objects.hash(alarmState, priority);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Alarm))
			return false;
		Alarm other = (Alarm)o;
		return this.alarmState == other.alarmState && this.priority==other.priority;
	}

	/**
	 * ACK an alarm avoiding to build a new Alarm if the state did not change
	 *
	 * @return the acked alarm or this if the alarm was already ackec
	 */
	public Alarm ack() {
		AlarmState as = alarmState.ack();
		if (as==alarmState) {
			return this;
		} else {
			return new Alarm(as, priority);
		}
	}

	/**
	 * @return the alarm just set
	 */
	public Alarm set() {
		AlarmState newState = alarmState.set();
		if (newState==alarmState) {
			return this;
		} else {
			return new Alarm(newState, priority);
		}
	}

	/**
	 * @return true if the alarm is set, false otherwise
	 */
	public boolean isSet() {
		return alarmState.isSet();
	}

	/**
	 * @return true if the alarm is acknowledged, false otherwise
	 */
	public boolean isAcked() {
		return alarmState.isAcked();
	}

	/**
	 * @return the alarm just cleared
	 */
	public Alarm clear() {
		AlarmState newState = alarmState.clear();
		if (newState==alarmState) {
			return this;
		} else {
			return new Alarm(newState, priority);
		}
	}
	
	/**
	 * Return an alarm of an increased priority if it exists,
	 * otherwise return the same alarm.
	 * 
	 * Increasing the priority of a {@link #CLEARED} 
	 * alarm is not allowed and the method throws an exception
	 * 
	 * @return the alarm with increased priority
	 */
	public Alarm increasePriority() {
		return new Alarm(alarmState, priority.getHigherPrio());
	}
	
	/**
	 * Return an alarm of a lowered priority if it exists,
	 * otherwise return the same alarm.
	 * 
	 * Lowering the priority of a {@link #CLEARED} 
	 * alarm is not allowed and the method throws an exception
	 * 
	 * @return the alarm with increased priority
	 */
	public Alarm lowerPriority() {
		return new Alarm(alarmState, priority.getLowerPrio());
	}

	/**
	 * Assign a priority to the alarm
	 *
	 * @param priority the priority to assign to the alarm
	 * @return the alarm with the new priority or
	 *         this if its priority matches with the passed priority
	 */
	public Alarm setPriority(Priority priority) {
		if (this.priority==priority) {
			return this;
		} else {
			return new Alarm(this.alarmState, priority);
		}
	}

	@Override
	public String toString() {
		return alarmState.toString()+":"+priority.toString();
	}

	/**
	 * Build an alarm from the passed string
	 *
	 * @param value The string encoding the alarm
	 * @return the alarm buit from the string
	 */
	public static Alarm valueOf(String value) {
		if (value==null || value.isBlank()) {
			throw new IllegalArgumentException("Empty or null string to convert to an Alarm");
		}
		System.out.println("ALARM STRING="+value);
		String[] parts = value.split(":");
		if (parts.length!=2) {
			throw new IllegalArgumentException("Malformed alarm string to parse: "+value);
		}
		AlarmState state = AlarmState.valueOf(parts[0]);
		Priority priority = Priority.valueOf(parts[1]);
		return new Alarm(state, priority);
	}
}
