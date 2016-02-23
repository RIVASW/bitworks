package com.example

import akka.actor.{Props, ActorRef, ActorLogging, Actor}

/**
  * Created by ivan on 22.02.16.
  */
class TestWorker  extends Actor with ActorLogging {

  import TestWorker._
  val tasks = new scala.collection.mutable.HashMap[String, ActorRef]()

  def receive = {
    case RequestServer.CheckStatus(url) => {
      log.info("In TestWorker - site to check: {}", url)
      tasks += url -> sender
    }
    case SendOk(url) =>{
      val sendTo = tasks(url)
      tasks -= url
      sendTo ! RequestServer.Status(url, "200")
    }

  }
}

object TestWorker{
  val props = Props[TestWorker]
  case class SendOk(url:String)
}