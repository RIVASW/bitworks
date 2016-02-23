package com.example

import akka.actor.ActorRef

/**
  * Created by ivan on 23.02.16.
  */
class RequestServerWithTestWorker(webCrawler: ActorRef, maxWorkers: Int, testWorker: ActorRef)
  extends RequestServer(webCrawler, maxWorkers){

  override def createWorker(name:String) = {
    testWorker
  }

}
