package fsm4s

private[fsm4s] sealed trait State[S, D] {
  private type State = fsm4s.State[S, D]

  def name: S
  def data: D
  def notifies: Boolean
  def using(nextStateData: D): State
  def withNotification(notifies: Boolean): State
  def transit(msg: Any): State
  def goto(nextStateName: S): State
  def stay(): State
}

private[fsm4s] class NullState[S, D] extends State[S, D] {
  private type State = fsm4s.State[S, D]

  def name: S = FSM.notInitializedError()
  def data: D = FSM.notInitializedError()
  def notifies: Boolean = FSM.notInitializedError()
  def using(nextStateData: D): State  = FSM.notInitializedError()
  def withNotification(notifies: Boolean): State  = FSM.notInitializedError()
  def transit(msg: Any): State = FSM.notInitializedError()
  def goto(nextStateName: S): State = FSM.notInitializedError()
  def stay(): State = FSM.notInitializedError()
}

private[fsm4s] case class SomeState[S, D](
  override val name: S,
  override val data: D,
  override val notifies: Boolean,
  private val stateFunctions: Map[S, StateFunction[S, D]],
  private val unhandledStateFunction: StateFunction[S, D]) extends State[S, D] {

  private type State = fsm4s.State[S, D]

  override def using(nextStateData: D): State = {
    copy(data = nextStateData)
  }

  override def withNotification(notifies: Boolean): State = {
    this.copy(notifies = notifies)
  }

  override def transit(msg: Any): State = {
    val event = Event(msg, data)

    val stateFunc = stateFunctions(name)
    if (stateFunc.isDefinedAt(event)) {
      stateFunc(event)
    } else {
      unhandledStateFunction(event)
    }
  }

  def goto(nextStateName: S): State = {
    if (!stateFunctions.contains(nextStateName)) {
      throw new IllegalStateException(s"Next state ${nextStateName} does not exist")
    } else {
      this.copy(name = nextStateName, notifies = true)
    }
  }

  def stay(): State = {
    this.copy(notifies = false)
  }
}
