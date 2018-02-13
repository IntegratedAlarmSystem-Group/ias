package org.eso.ias.asce

/**
 * The state machine for the ASCE
 * 
 * The ASCE transitions through 6 different states during its 
 * life time 
 */
abstract class FiniteStateMachineState[T <: Enumeration](val state: T)

/**
 * The state of the ASCE
 */
object AsceStates extends Enumeration {
  type State = Value
  
  val Initializing = Value("Initializing") // Initializing: start state
  val InputsUndefined = Value("InputsUndefined") // Some of the inputs have a null value and the TF cannot be run
  val Healthy = Value("Running") // Everything OK
  val TFBroken = Value("TF broken") // The transfer function is broken
  val TFSlow = Value("TF slow") // The transfer function is slow
  val ShuttingDown = Value("Shutting down") // The ASCE is shutting down
  val Closed = Value("Closed") // Closed i.e. shutdown complete
  
  /**
   * When the state of the ASCE is in
   * this list, the transfer function is not executed
   */
  val inhibitorStates = Set(Initializing, InputsUndefined, TFBroken, ShuttingDown, Closed)
}

/**
 *  The events to switch the state of the ASCE
 */
trait ASCEStateEvent

/**
 *  The ASCE has been initialized:
 *  the TF class has been loaded and intialized
 *  and the ASCE is ready to produce the output
 */
case class Initialized() extends ASCEStateEvent

/**
 *  All the possible inputs of the ASCE have been initialized:
 *  the TF can run and the ASCE can produce the putput
 */
case class InputsInitialized() extends ASCEStateEvent


/**
 * The user provided TF is broken 
 * i.e it typically threw an exception.
 */
case class Broken() extends ASCEStateEvent

/**
 * The user provided TF is too slow
 */
case class Slow() extends ASCEStateEvent

/**
 * The user provided TF is back to normal
 */
case class Normal() extends ASCEStateEvent

/**
 *  The ASCE has been shutdown
 *  This state shuts down the TF
 */
case class Shutdown() extends ASCEStateEvent

/**
 *  The TF has been shutdown and the ASCE is now closed 
 */
case class Close() extends ASCEStateEvent

/**
 * The exception thrown when the actual state does not accept a transition
 */
class InvalidAsceStateTransitionException(
    actualState: AsceStates.State,
    transition: ASCEStateEvent) extends Exception(
       "Invalid transition "+transition+" from "+actualState+" state"
    )

/**
 * The ASCE state 
 */
class ComputingElementState(val actualState: AsceStates.State = AsceStates.Initializing) {
  
  /**
   * @return true if the TF can be executed in the current state
   */
  def canRunTF(): Boolean = !AsceStates.inhibitorStates.contains(actualState)
  
  override def toString() = actualState.toString()
  
}

/**
 * The implementation of the ASCE state machine transitions
 */
object ComputingElementState {
  
  /**
   * The transition of the state of a ASCE as a result of an event
   * 
   * @param asceState: the ASCE state that receives the event
   * @param e: the event to apply to the current state of the ASCE
   * @result the new state of the ASCE after applying the event to the current state
   */
  def transition(asceState: ComputingElementState, e: ASCEStateEvent): ComputingElementState = {
    asceState.actualState match {
      case AsceStates.Initializing =>
        e match {
          case Initialized() => new ComputingElementState(AsceStates.InputsUndefined)
          case Broken() => new ComputingElementState(AsceStates.TFBroken)
          case Shutdown()  => new ComputingElementState(AsceStates.ShuttingDown)
          case _ => throw new InvalidAsceStateTransitionException(asceState.actualState,e)
        }
      case AsceStates.InputsUndefined => 
        e match {
          case InputsInitialized() => new ComputingElementState(AsceStates.Healthy)
          case Shutdown()  => new ComputingElementState(AsceStates.ShuttingDown)
          case _ => throw new InvalidAsceStateTransitionException(asceState.actualState,e)
        }
      case AsceStates.Healthy =>
        e match {
          case Broken()  => new ComputingElementState(AsceStates.TFBroken)
          case Slow()  => new ComputingElementState(AsceStates.TFSlow)
          case Shutdown()  => new ComputingElementState(AsceStates.ShuttingDown)
          case Normal() => asceState
          case _ => throw new InvalidAsceStateTransitionException(asceState.actualState,e)
        }
      case AsceStates.TFBroken =>
        e match {
          case Shutdown()  => new ComputingElementState(AsceStates.ShuttingDown)
          case _ => throw new InvalidAsceStateTransitionException(asceState.actualState,e)
        }
        case AsceStates.TFSlow =>
        e match {
          case Normal()  => new ComputingElementState(AsceStates.Healthy)
          case Shutdown()  => new ComputingElementState(AsceStates.ShuttingDown)
          case Slow() => asceState
          case _ => throw new InvalidAsceStateTransitionException(asceState.actualState,e)
        }
      case AsceStates.ShuttingDown =>
        e match {
          case Close()  => new ComputingElementState(AsceStates.Closed)
          case _ => throw new InvalidAsceStateTransitionException(asceState.actualState,e)
        }
      case AsceStates.Closed =>
        // Final state
        e match {
          case _ => throw new InvalidAsceStateTransitionException(asceState.actualState,e)
        }
    }
  }
}
