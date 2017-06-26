package fsm4s

import scala.collection.mutable
import language.implicitConversions

/**
  * Reference from FSM.scala - https://github.com/akka/akka/blob/master/akka-actor/src/main/scala/akka/actor/FSM.scala
  *
  * Created by cwei on 23/5/17.
  */
object FSM {
  private[fsm4s] object NullFunction extends PartialFunction[Any, Nothing] {
    def isDefinedAt(o: Any): Boolean = false
    def apply(o: Any): Nothing = sys.error("undefined")
  }

  private[fsm4s] def notInitializedError(): Nothing = {
    sys.error("FSM not initialized, please call `FSM.initialize`")
  }


  private[fsm4s] sealed trait TransitionActions[S] {
    def trigger(prev: S, next: S): Unit
    def :+ (action: TransitionAction[S]): TransitionActions[S] // scalastyle:ignore
  }

  private[fsm4s] class NullTransitionActions[S] extends TransitionActions[S] {
    def trigger(prev: S, next: S): Unit = notInitializedError()
    def :+ (action: TransitionAction[S]): TransitionActions[S] = { // scalastyle:ignore
      notInitializedError()
    }
  }

  private[fsm4s] class SomeTransitionActions[S](
    private val actions: Seq[TransitionAction[S]] = Nil) extends TransitionActions[S] {

    def trigger(prev: S, next: S): Unit = {
      val pair = (prev, next)
      for (action ← actions) { if (action.isDefinedAt(pair)) action(pair) }
    }

    def :+ (action: TransitionAction[S]): TransitionActions[S] = {  // scalastyle:ignore
      new SomeTransitionActions[S](actions :+ action)
    }
  }


  private[fsm4s] case class Builder[S, D](
    private val startState: Option[(S, D)] = None,
    private val stateFunctions: Map[S, StateFunction[S, D]] = Map.empty[S, StateFunction[S, D]],
    private val transitionActions: TransitionActions[S] = new SomeTransitionActions[S](),
    private val unhandledStateFunction: StateFunction[S, D] = NullFunction
  ) {
    type StateFunctionT = StateFunction[S, D]
    type StateT = State[S, D]
    type TransitionActionT = TransitionAction[S]
    type TransitionActionsT = TransitionActions[S]

    def when(stateName: S)(stateFunction: StateFunctionT): Builder[S, D] = {
      val newStateFunctions = register(stateName, stateFunction)
      this.copy(stateFunctions = newStateFunctions)
    }

    private def register(name: S, function: StateFunctionT): Map[S, StateFunctionT] = {
      if (stateFunctions.contains(name)) {
        stateFunctions + (name -> stateFunctions(name).orElse(function))
      } else {
        stateFunctions + (name -> function)
      }
    }

    def startWith(stateName: S, stateData: D): Builder[S, D] = {
      this.copy(startState = Some((stateName, stateData)))
    }

    def whenUnhandled(stateFunction: StateFunctionT): Builder[S, D] = {
      this.copy(unhandledStateFunction = stateFunction)
    }

    def onTransition(transitionAction: TransitionActionT): Builder[S, D] = {
      this.copy(transitionActions = transitionActions :+ transitionAction)
    }

    def build(handleEventDefault: StateFunctionT): (StateT, TransitionActionsT) = startState match {
      case None =>
        throw new IllegalArgumentException("You must call `startWith` before calling `initialize`")
      case Some((stateName, stateData)) =>
        (SomeState(
          name = stateName,
          data = stateData,
          notifies = true,
          stateFunctions = stateFunctions,
          unhandledStateFunction = unhandledStateFunction),
          transitionActions)
    }
  }

  object `->` {  // scalastyle:ignore
    def unapply[S](in: (S, S)): Option[(S, S)] = Some(in)
  }
  val `→` = `->`
}

trait FSM[S, D] extends Sendable {
  type State = fsm4s.State[S, D]
  type StateFunction = fsm4s.StateFunction[S, D]
  type TransitionAction = fsm4s.TransitionAction[S]
  private type TransitionActions = FSM.TransitionActions[S]
  private type NullState = fsm4s.NullState[S, D]
  private type SomeState = fsm4s.SomeState[S, D]

  private val nullState = new NullState()
  private var builder = new FSM.Builder[S, D]()

  private var currentState: State = nullState
  private var nextState: State = nullState
  private var transitionActions: TransitionActions = new FSM.NullTransitionActions[S]()
  private val transitionListeners: mutable.Set[Sendable] = mutable.Set[Sendable]()

  final override def send(msg: Any): Unit = msg match {
    case SubscribeTransitionCallBack(subscriber) =>
      transitionListeners += subscriber
      subscriber.send(CurrentState(this, currentState.name))
    case UnsubscribeTransitionCallBack(subscriber) =>
      transitionListeners -= subscriber
    case _: Any =>
      processMsg(msg)
  }

  private def processMsg(msg: Any): Unit = {
    nextState = currentState.transit(msg)
    try {
      makeTransition(currentState, nextState)
      currentState = nextState
    } finally {
      nextState = nullState
    }
  }

  private def makeTransition(currentState: State, nextState: State): Unit = {
    if (currentState.name != nextState.name || nextState.notifies) {
      transitionActions.trigger(currentState.name, nextState.name)
      notifyTransitionListeners(Transition(this, currentState.name, nextState.name))
    }
  }

  private def notifyTransitionListeners(msg: Any): Unit = {
    transitionListeners.foreach(_.send(msg))
  }

  final def goto(nextStateName: S): State = {
    currentState.goto(nextStateName)
  }

  final def stay(): State = {
    currentState.stay()
  }

  final def startWith(stateName: S, stateData: D): Unit = {
    builder = builder.startWith(stateName, stateData)
  }

  final def when(stateName: S)(stateFunction: StateFunction): Unit = {
    builder = builder.when(stateName)(stateFunction)
  }

  final def onTransition(transitionAction: TransitionAction): Unit = {
    builder = builder.onTransition(transitionAction)
  }

  final def whenUnhandled(stateFunction: StateFunction): Unit = {
    builder = builder.whenUnhandled(stateFunction)
  }

  final def initialize(): Unit = {
    val (startState, newTransitionActions) = builder.build(handleEventDefault)
    currentState = startState
    transitionActions = newTransitionActions
  }

  private val handleEventDefault: StateFunction = {
    case event: Event[D] ⇒
      unhandledEvent(event)
      stay()
  }

  /**
    * Override to log unhandled events instead of throwing as an error.
    */
  protected def unhandledEvent(event: Event[D]): Unit = {
    sys.error(s"unhandled event '${event.event}' in state '${stateName}'")
  }

  final def stateName: S = currentState match {
    case _: NullState =>
      throw new IllegalStateException("You must call `startWith` and `initialize` before using `stateName`")
    case _: SomeState =>
      currentState.name
  }

  final def stateData: D = currentState match {
    case _: NullState =>
      throw new IllegalStateException("You must call `startWith` and `initialize` before using `stateData`")
    case _: SomeState =>
      currentState.data
  }

  final def nextStateData: D = nextState match {
    case _: NullState =>
      throw new IllegalStateException("nextStateData is only available during onTransition")
    case _: SomeState =>
      nextState.data
  }
}
