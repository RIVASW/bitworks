package com.example

/**
  * Created by ivan on 22.02.16.
  */
import akka.testkit.{ TestProbe, ImplicitSender, TestKit }
import akka.actor.{ Props, ActorSystem }
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import akka.actor.FSM.{
Transition,
CurrentState,
SubscribeTransitionCallBack
}


class RequestServerTest extends TestKit(ActorSystem("RequestServerTest"))
  with WordSpecLike with BeforeAndAfterAll with MustMatchers
  with ImplicitSender with StopSystemAfterAll {

  import RequestServer._

  "RequestServer" must {
    "follow the flow" in {
      val stateProbe = TestProbe()
      val replyProbe = TestProbe()
      val testWorker = system.actorOf(TestWorker.props)
      val requestServer = system.actorOf(Props(new RequestServerWithTestWorker(replyProbe.ref, 3, testWorker)))

      requestServer ! new SubscribeTransitionCallBack(stateProbe.ref)
      stateProbe.expectMsg(
        new CurrentState(requestServer, WaitForRequests))

      //start test
      requestServer ! new CheckStatus("http://google.com")
      stateProbe.expectMsg(
        new Transition(requestServer, WaitForRequests, ProcessRequest))
      replyProbe.expectMsg(NextUrl)
      requestServer ! new CheckStatus("http://yahoo.com")
      replyProbe.expectMsg(NextUrl)
      requestServer ! new CheckStatus("http://facebook.com")
      stateProbe.expectMsg(
        new Transition(requestServer, ProcessRequest, WaitForFreeWorker))
      testWorker ! TestWorker.SendOk("http://google.com")
      replyProbe.expectMsg(Status("http://google.com", "200"))
      replyProbe.expectMsg(NextUrl)
      stateProbe.expectMsg(
        new Transition(requestServer, WaitForFreeWorker, ProcessRequest))
      testWorker ! TestWorker.SendOk("http://yahoo.com")
      replyProbe.expectMsg(Status("http://yahoo.com", "200"))
      testWorker ! TestWorker.SendOk("http://facebook.com")
      replyProbe.expectMsg(Status("http://facebook.com", "200"))
      stateProbe.expectMsg(
        new Transition(requestServer, ProcessRequest, WaitForRequests))
      
      system.stop(requestServer)
      system.stop(testWorker)
    }
  }

  "RequestServer" must {
    "prepare for shutdown" in {
      val stateProbe = TestProbe()
      val replyProbe = TestProbe()
      val testWorker = system.actorOf(TestWorker.props)
      val requestServer = system.actorOf(Props(new RequestServerWithTestWorker(replyProbe.ref, 3, testWorker)))

      requestServer ! new SubscribeTransitionCallBack(stateProbe.ref)
      stateProbe.expectMsg(
        new CurrentState(requestServer, WaitForRequests))

      //start test
      requestServer ! new CheckStatus("http://google.com")
      stateProbe.expectMsg(
        new Transition(requestServer, WaitForRequests, ProcessRequest))
      replyProbe.expectMsg(NextUrl)
      requestServer ! new CheckStatus("http://yahoo.com")
      replyProbe.expectMsg(NextUrl)
      requestServer ! new CheckStatus("http://facebook.com")
      stateProbe.expectMsg(
        new Transition(requestServer, ProcessRequest, WaitForFreeWorker))
      requestServer ! Shutdown
      stateProbe.expectMsg(
        new Transition(requestServer, WaitForFreeWorker, WaitForSutdonw))
      testWorker ! TestWorker.SendOk("http://google.com")
      replyProbe.expectMsg(Status("http://google.com", "200"))
      testWorker ! TestWorker.SendOk("http://yahoo.com")
      replyProbe.expectMsg(Status("http://yahoo.com", "200"))
      testWorker ! TestWorker.SendOk("http://facebook.com")
      replyProbe.expectMsg(Status("http://facebook.com", "200"))
      stateProbe.expectMsg(
        new Transition(requestServer, WaitForSutdonw, WaitForRequests))
      replyProbe.expectMsg(ReadyToShutdown)

      system.stop(requestServer)
      system.stop(testWorker)
    }
  }


}
