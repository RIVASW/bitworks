package com.example

import akka.actor.{Actor, ActorLogging, Props}
import java.io._

class FileWriter extends Actor with ActorLogging {
  import FileWriter._
  
  val writer = new PrintWriter(new File("results.csv" ))

  def receive = {      
  	case RequestServer.Status(url, status) => {
      writer.write(url + "," + status + "\n")
  	}
   	case Shutdown => {
   	  writer.close
      context.system.shutdown()  
   	}

  }	
}

object FileWriter {
  case object Shutdown
  val props = Props[FileWriter]
}
