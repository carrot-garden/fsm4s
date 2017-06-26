package fsm4s

trait Sendable {
  def send(msg: Any): Unit
}
