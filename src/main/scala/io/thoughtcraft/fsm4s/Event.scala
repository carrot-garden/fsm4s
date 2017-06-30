package io.thoughtcraft.fsm4s

/**
  * Created by cwei on 26/5/17.
  */
final case class Event[D](event: Any, stateData: D)
