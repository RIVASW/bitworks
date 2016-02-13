package com.example

import akka.actor.{ ActorRef, Actor, FSM }

class RequestServer(webCrawler: ActorRef, maxWorkers: Int) extends Actor
  with FSM[RequestServer.State, RequestServer.StateData] {
  import RequestServer._

  var reserveId = 0
  startWith(WaitForRequests, new StateData(0, 0))

  when(WaitForRequests) {
    case Event(request: CheckStatus, data: StateData) => {
      val newStateData = StateData( data.workerId + 1, data.workers + 1)
      val webWorker = context.actorOf(WebWorker.props, "webWorker_" + data.workerId)
      webWorker ! request      
      if (newStateData.workers < maxWorkers) {
        webCrawler ! NextUrl
        goto(ProcessRequest) using newStateData
      } else {
        goto(WaitForFreeWorker) using newStateData
      }
    }
    case Event(Shutdown, data: StateData) => {
      webCrawler ! ReadyToShutdown
      stay
    }
  }

  when(ProcessRequest) {
    case Event(request: CheckStatus, data: StateData) => {
      val newStateData = StateData( data.workerId + 1, data.workers + 1)
      val webWorker = context.actorOf(WebWorker.props, "webWorker_" + data.workerId)
      webWorker ! request      
      if (newStateData.workers < maxWorkers) {
        webCrawler ! NextUrl
        goto(ProcessRequest) using newStateData
      } else {
        goto(WaitForFreeWorker) using newStateData
      }
    }
    case Event(status: Status, data: StateData) => {
      val newStateData = data.copy( workers = data.workers - 1)
      webCrawler ! status
      if (newStateData.workers > 0){
        goto(ProcessRequest) using newStateData
      } else {
        goto(WaitForRequests) using newStateData
      }      
    }
    case Event(Shutdown, data: StateData) => {
      goto(WaitForSutdonw) using data
    }
  }

  when(WaitForFreeWorker) {
    case Event(status: Status, data: StateData) => {
      val newStateData = data.copy(workers = data.workers - 1)
      webCrawler ! status
      log.info("In RequestServer - got free worker")
      if(newStateData.workers > 0){
        goto(ProcessRequest) using newStateData  
      }else{
        goto(WaitForRequests) using newStateData  
      }
      
    }
    case Event(Shutdown, data: StateData) => {
      goto(WaitForSutdonw) using data
    }
  }

  when(WaitForSutdonw) {
    case Event(status: Status, data: StateData) => {
      val newStateData = data.copy(workers = data.workers - 1)
      webCrawler ! status
      if(newStateData.workers > 0){
        goto(WaitForSutdonw) using newStateData
      } else {
        goto(WaitForRequests) using newStateData
      }
    }
  }
  
  whenUnhandled {
    case Event(e, s) => {
      log.warning("received unhandled request {} in state {}/{}",
        e, stateName, s)
      stay
    }
  }

  onTransition {
    case WaitForFreeWorker -> WaitForRequests => {
      webCrawler ! NextUrl
    }
    case WaitForFreeWorker -> ProcessRequest => {
      webCrawler ! NextUrl
    }
    case WaitForSutdonw -> WaitForRequests => {
      webCrawler ! ReadyToShutdown
    }
  }
  
  initialize
}

object RequestServer {
  case class StateData(workerId: Int, workers:Int)  
  // events
  case class CheckStatus(url: String)
  case class Status(url: String, status: String)
  case object Shutdown

  //responses
  case object NextUrl
  case object ReadyToShutdown

  //states
  sealed trait State
  case object WaitForRequests extends State
  case object ProcessRequest extends State
  case object WaitForFreeWorker extends State
  case object WaitForSutdonw extends State  
}


