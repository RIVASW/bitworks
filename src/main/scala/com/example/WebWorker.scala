package com.example

import akka.actor.{Actor, ActorLogging, Props, PoisonPill}
import scalaj.http._

class WebWorker extends Actor with ActorLogging {
  import WebWorker._
  
  val rnd = scala.util.Random

  def receive = {
  	case RequestServer.CheckStatus(url) => 
  	  log.info("In WebWorker - site to check: {}", url)
      val status =
      try{
        Http(url).asString.code.toString
      }
      catch{
        case e:Throwable => "Error"
      }         
  	  sender ! RequestServer.Status(url, status)
      self ! PoisonPill
  }	
}

object WebWorker {
  val props = Props[WebWorker]
  case class UrlStatusMessage(url: String, status: String)
}
