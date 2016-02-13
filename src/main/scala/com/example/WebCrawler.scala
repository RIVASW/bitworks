package com.example

import akka.actor.{Actor, ActorLogging, Props, ActorRef }
import scala.io.Source

class WebCrawler(urlReader: ActorRef) extends Actor with ActorLogging {
  import WebCrawler._
  
  var config = Map[String, String]()
  
  var requestServer:ActorRef = null
  val resultWriter = context.actorOf(FileWriter.props, "resultWriter")
  var domainZone:Option[String] = None
  var maxWorkers:Int = 1
  val defaultMaxWorkers = 2
  
  def getHost(url: String) ={
    try{
      val jurl = new java.net.URL(url)
      Some(jurl.getHost) 
    }
    catch{
      case e:Throwable => None
    }
  }
  def readConfig() ={
      val lines =  Source.fromFile("webCrawler.conf").getLines()
      config = lines.toList.filter(!_.startsWith(";")).map(_.split("=")).
                filter(_.size == 2).map((c) => c(0) -> c(1)).toMap
      domainZone = config.get("domain_zone")
      maxWorkers = config.get("max_workers").map(_.toInt).
                    getOrElse(defaultMaxWorkers)                    
      log.info("In WebCrawler - config domain_zone: {}, max_workers: {}", domainZone, maxWorkers)
  }

  def receive = {
    case UrlReader.Initialize =>{
      readConfig()
      requestServer = context.actorOf(Props(new RequestServer(self, maxWorkers))
                                      ,"requestServer")
      sender ! UrlReader.NextUrl
    }
  	case CheckUrl(url) => {
      getHost(url).map((host) => {
          domainZone.map((dz) =>{
            if(host.endsWith(dz)){
              	  log.info("In WebCrawler - site to check: {}", url)
                  requestServer ! RequestServer.CheckStatus(url)  	          
            }else{
              urlReader ! UrlReader.NextUrl  
            }                  
          }).getOrElse(requestServer ! RequestServer.CheckStatus(url))
      }).getOrElse(urlReader ! UrlReader.NextUrl)
  	}
    case RequestServer.NextUrl => {
      urlReader ! UrlReader.NextUrl   
    }
    case UrlReader.UrlListComplete => {
      requestServer ! RequestServer.Shutdown
    }
    case RequestServer.ReadyToShutdown => {
  	  log.info("In WebCrawler - shutdown")        	        
      resultWriter ! FileWriter.Shutdown
    }
    case RequestServer.Status(url, status) => {
  	  log.info("In WebCrawler - reply url {}, status {}", url, status)
      resultWriter ! RequestServer. Status(url, status)
    }

  }	
}

object WebCrawler { 
  case class CheckUrl(url: String)
}  
