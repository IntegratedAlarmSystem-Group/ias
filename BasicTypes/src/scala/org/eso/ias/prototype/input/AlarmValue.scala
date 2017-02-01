package org.eso.ias.prototype.input

/**
 * The state machine for an alarm.
 * 
 * At the beginning of this prototype there were 4 states:
 * ActiveAndNew, ActiveAndAcknowledged, Cleared and Unknown.
 * This did not cover the case of an alarm being cleared but not yet
 * acknowledged. We realized that being ACK or not is a property and 
 * in that case there are only three states and only 2 really used.
 * Probably a state machine for this very simple case is not needed.
 */
abstract class FiniteStateMachineState[T <: Enumeration](val state: T)

/**
 * The state of an alarm
 */
object AlarmState extends Enumeration {
  type State = Value
  // The alarm has been set by the alarm source
  val Active = Value("Active") 
  // The alarm has been cleared by the alarm source
  val Cleared = Value("Cleared") 
  // Unknown state is the initial state of the alarm
  // when it is built
  val Unknown = Value("Unknown")
}

/**
 * An alarm can be still ignored or already acknowledged by the operator.
 * 
 * We could express such property with a Boolean but the state is 
 * more verbose and explained better the meaning of this property.
 */
object AckState extends Enumeration {
  // New is an alarm that the operator has not yet acknowledged
  val New = Value("New")
  // An acknowledged alarm is one that the operator has acknowledged
  // (he/she is also supposed to have taken the proper counter-action)
  val Acknowledged  = Value("Acknowledged")
}

/**
 * The immutable value of an alarm. 
 * 
 * The AlarmValue has a state (@see AlarmState) plus a acknowledgementt and 
 * a shelved property.
 * 
 * In the design of the IAS, the Alarm is a special monitor point so
 * that at a certain level it is possible to use indifferently alarms and monitor points
 * 
 * Objects from this class shall not be used directly:
 * <code>org.eso.ias.prototype.input.typedmp</code> provides a Alarm class.
 * 
 * @constructor Build a AlarmValue
 * @param state The state of the alarm
 * @param shelved True if the alarm is shelved, false otherwise
 * @param acknowledgement Tell is the operator acknowledged the alarm
 * @see org.eso.ias.prototype.input.typedmp.Alarm
 * 
 * @author acaproni
 */
case class AlarmValue(
    alarmState: AlarmState.State,  
    shelved: Boolean,
    acknowledgement: AckState.Value) {
  require(Option[AlarmState.State](alarmState).isDefined)
  require(Option[Boolean](shelved).isDefined)
  require(Option[AckState.Value](acknowledgement).isDefined)
  
  def this() {
    this(AlarmState.Unknown,false,AckState.Acknowledged)
  }
  
  /**
   * Shelve/Unshelve an alarm
   * 
   * @param s If True shelve the alarm otherwise, unshelve
   * @return This same alarm with the shelved property updated 
   */
  def shelve(s: Boolean): AlarmValue = AlarmValue(alarmState, s,  acknowledgement)
  
  /**
   * Acknowledge the Alarm.
   * 
   * This method is called in response of an operator action.
   * Apart the case when the operator explicitly acknowledges an Alarm,
   * state transitions set the acknowledgement state too.
   */
  def acknowledge(): AlarmValue = this.copy(acknowledgement=AckState.Acknowledged)
  
  override def toString(): String = {
    alarmState.toString() + " " + acknowledgement.toString() +
      (if (shelved) " shelved" else " not shelved")
  }
  
}


/**
 *  The events to switch state
 */
trait Event

/**
 *  A new or acknowledged alarm became inactive
 */
case class Clear() extends Event

/**
 *  A cleared alarm became active again
 */
case class Set() extends Event

/**
 * The exception thrown when the actual state does not accept a transition
 */
class InvalidStateTransitionException(
    actualState: AlarmState.State,
    transition: Event) extends Exception(
       "Invalid transition "+transition+" from "+actualState+" state"
    )

/**
 * The AlarmValue companion implements the state class.
 */
object AlarmValue {

  /**
   * The transition of the state of an alarm as a result of an event
   * 
   * @param a: the alarm that receives the event
   * @param e: the event to apply to the alarm
   * @result the alarm after the event has been processed, or the
   * 				 exception thrown in case of a unallowed transition
   */
  def transition(a: AlarmValue, e: Event): Either[Exception,AlarmValue] = {
    require(Option[AlarmValue](a).isDefined)
    a.alarmState match {
      case AlarmState.Active =>
        e match {
          case Clear() => Right(a.copy(alarmState = AlarmState.Cleared))
          case Set() => Right(a)
        }
      case AlarmState.Cleared =>
        e match {
          case Set() =>  Right(a.copy(alarmState = AlarmState.Active,acknowledgement=AckState.New))
          case _ => Right(a)
        }
      case AlarmState.Unknown =>
        e match {
          case Clear() => Right(a.copy(alarmState = AlarmState.Cleared))
          case Set() => Right(a.copy(alarmState = AlarmState.Active,acknowledgement=AckState.New))
        }
      case _ => Left(new InvalidStateTransitionException(a.alarmState,e))
    }
  }
}
