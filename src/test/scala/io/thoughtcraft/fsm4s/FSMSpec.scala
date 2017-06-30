package io.thoughtcraft.fsm4s

import fsm4s.Event
import fsm4s.Sendable

/**
  * References from:
  * a. FSMTransitionSpec.scala - https://github.com/akka/akka/blob/master/akka-actor-tests/src/test/scala/akka/actor/FSMTransitionSpec.scala
  * b. FSMActorSpec.scala - https://github.com/akka/akka/blob/master/akka-actor-tests/src/test/scala/akka/actor/FSMActorSpec.scala
  *
  */
// scalastyle:off multiple.string.literals
class FSMSpec extends UnitSpec {
  trait WithTester {
    val tester = new TestProbe()
  }

  "A FSM transition" must {
    "trigger onTransition as expected" in new WithTester {
      val fsm = new TransitionFSM()

      fsm.send(SubscribeTransitionCallBack(tester))
      tester.expectMsg(CurrentState(fsm, 0))

      fsm.send("tick")
      tester.expectMsg(Transition(fsm, 0, 1))

      fsm.send("tick")
      tester.expectMsg(Transition(fsm, 1, 0))
    }

    class TransitionFSM extends FSM[Int, Unit] {
      startWith(0, Unit)
      when(0) {
        case Event("tick", _) ⇒ goto(1)
      }
      when(1) {
        case Event("tick", _) ⇒ goto(0)
      }
      initialize()
    }

    "not trigger onTransition for stay" in new WithTester {
      val fsm = new StayFSM()
      fsm.send(SubscribeTransitionCallBack(tester))
      tester.expectMsg(CurrentState(fsm, 0))

      fsm.send("stay")
      tester.expectNoMsg()
    }

    class StayFSM extends FSM[Int, Unit] {
      startWith(0, Unit)
      when(0) {
        case Event("stay", _) ⇒ stay()
        case _ ⇒ goto(0)
      }

      initialize()
    }

    "trigger transition event when goto() the same state" in new WithTester {
      val fsm = new GoToFSM()
      fsm.send(SubscribeTransitionCallBack(tester))
      tester.expectMsg(CurrentState(fsm, 0))

      fsm.send("goto")
      tester.expectMsg(Transition(fsm, 0, 0))
    }

    class GoToFSM extends FSM[Int, Unit] {
      startWith(0, Unit)
      when(0) {
        case Event("goto", _) ⇒ goto(0)
        case _ ⇒ stay()
      }

      initialize()
    }

    "stop receiving transition event when un-subscribed" in new WithTester {
      val fsm = new GoToFSM()
      fsm.send(SubscribeTransitionCallBack(tester))
      tester.expectMsg(CurrentState(fsm, 0))

      fsm.send(UnsubscribeTransitionCallBack(tester))

      fsm.send("goto")
      tester.expectNoMsg()
    }
  }

  "A FSM state" must {
    "make previous and next state data available in onTransition" in new WithTester {
      val fsm = new StateFMS(tester)
      fsm.send("tick")
      tester.expectMsg((0, 1))
    }

    class StateFMS(receiver: Sendable) extends FSM[Int, Int] {
      startWith(0, 0)
      when(0) {
        case Event("tick", _) ⇒ goto(1).using(1)
        case _ ⇒ stay()
      }
      when(1) {
        case _ ⇒ stay()
      }
      onTransition {
        case 0 → 1 ⇒ receiver.send((stateData, nextStateData))
      }

      initialize()
    }
  }
}
// scalastyle:on multiple.string.literals