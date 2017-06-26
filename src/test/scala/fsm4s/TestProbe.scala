package fsm4s

import org.scalactic.source
import org.scalatest.Assertion
import org.scalatest.Matchers
import org.scalatest.Succeeded

import scala.collection.immutable.Queue
import org.scalatest.compatible.Assertion
import org.scalatest.exceptions
import org.scalatest.exceptions.TestFailedException

/**
  * Created by cwei on 24/5/17.
  */
class TestProbe extends Sendable with Matchers {
  private var queue = Queue.empty[Any]

  override def send(msg: Any): Unit = {
    queue = queue.enqueue(msg)
  }

  def expectMsg(msg: Any): Assertion = {
    if(queue.nonEmpty) {
      val (actualMsg, newQueue) = queue.dequeue
      actualMsg should equal(msg)
      queue = newQueue
      Succeeded
    } else {
      indicateFailure(s"expected ${msg}, but no message was received", None, source.Position.here)
    }
  }

  private def indicateFailure(failureMessage: => String, optionalCause: Option[Throwable], pos: source.Position): Assertion = {
    val message: String = failureMessage
    throw new TestFailedException((sde: exceptions.StackDepthException) => Some(message), optionalCause, pos)
  }

  def expectNoMsg(): Assertion = {
    if (queue.isEmpty) {
      Succeeded
    } else {
      indicateFailure(s"expected no message, but there are ${queue.size} messages", None, source.Position.here)
    }
  }
}

