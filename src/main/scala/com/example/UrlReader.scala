package com.example

import akka.actor.{Actor, ActorLogging, Props}
import scala.io.Source

class UrlReader extends Actor with ActorLogging {
  import UrlReader._
  
  val webCrawler = context.actorOf(Props(new WebCrawler(self)), "webCrawler")
  val urls =  Source.fromFile("urls.txt").getLines()  
  def sendNext() = 
    if (!urls.hasNext) webCrawler ! UrlListComplete
    else webCrawler ! WebCrawler.CheckUrl(urls.next)	        

  def receive = {      
  	case Initialize => 
	    log.info("In UrlReader - starting")
      webCrawler ! Initialize
  	case NextUrl =>
  	  log.info("In UrlReader - next url")  	  
  	  sendNext()
  }	
}

object UrlReader {
  val props = Props[UrlReader]
  case object Initialize
  case object UrlListComplete
  case object NextUrl
}
