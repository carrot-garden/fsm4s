package fsm4s

final case class CurrentState[S](fsm: Sendable, state: S)

final case class Transition[S](fsm: Sendable, from: S, to: S)


/**
  * Subscribing or un-subscribing to transition events.
  */
final case class SubscribeTransitionCallBack(subscriber: Sendable)

final case class UnsubscribeTransitionCallBack(subscriber: Sendable)
