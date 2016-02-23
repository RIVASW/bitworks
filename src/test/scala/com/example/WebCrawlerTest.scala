package com.example

/**
  * Created by ivan on 20.02.16.
  */

import org.scalatest.WordSpecLike
import org.scalatest.MustMatchers
import akka.testkit.{ TestActorRef, TestKit }
import akka.actor._

class WebCrawlerTest extends TestKit(ActorSystem("testsystem"))
  with WordSpecLike
  with MustMatchers
  with StopSystemAfterAll {

  "A WebCrawler Actor" must {
    "send a NextUrl message to a UrlReader when it has finished initialization" in {
      val props = WebCrawler.props(testActor)
      val webCrawler = system.actorOf(props, "WebCrawler01")
      webCrawler ! UrlReader.Initialize
      expectMsg(UrlReader.NextUrl)
    }

    "send a CheckStatus message to a RequestServer when url matches domain zone or domain zone is not set" in {
      val props = WebCrawler.props(testActor)
      val webCrawler = system.actorOf(props, "WebCrawler02")
      webCrawler ! WebCrawler.Config(None, testActor)
      webCrawler ! WebCrawler.CheckUrl("https://google.com")
      expectMsg(RequestServer.CheckStatus("https://google.com"))
      webCrawler ! WebCrawler.Config(Some("google.com"), testActor)
      webCrawler ! WebCrawler.CheckUrl("https://mail.google.com")
      expectMsg(RequestServer.CheckStatus("https://mail.google.com"))
    }

    "send a NextUrl message to a UrlReader when url doesn't match domain zone or url is not correct" in {
      val props = WebCrawler.props(testActor)
      val webCrawler = system.actorOf(props, "WebCrawler03")
      webCrawler ! WebCrawler.Config(Some("google.com"), testActor)
      webCrawler ! WebCrawler.CheckUrl("https://mail.yahoo.com")
      expectMsg(UrlReader.NextUrl)
      webCrawler ! WebCrawler.CheckUrl("htt://mail.google.com")
      expectMsg(UrlReader.NextUrl)
    }
  }

}

