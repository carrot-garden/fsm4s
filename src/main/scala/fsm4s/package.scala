package object fsm4s {
  type StateFunction[S, D] = scala.PartialFunction[Event[D], State[S, D]]
  type TransitionAction[S] = PartialFunction[(S, S), Unit]
}
