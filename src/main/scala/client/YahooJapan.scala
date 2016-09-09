package client

import Service._
import java.util.Date

import org.jsoup.Jsoup

import spray.client.pipelining._
import spray.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait YahooJapanAuctions extends Sniper {
  implicit private val url: Uri = "http://page2.auctions.yahoo.co.jp"

  override def timeLeft(auction_id: String): Future[Try[Int]] = {
    val endpoint: Uri = "now"

    val finalEndpoint: Uri =
      endpoint.withQuery(Map(
        "aID" -> auction_id,
        "nowtime" -> new Date().getTime.toString))

    implicit val request = Get(uri(finalEndpoint)) ~> addHeaders(headers)

    requestToResponse map {
      response =>
        try {
          Success(response.entity.asString.toInt) // toInt throws exception
        }
        catch {
          case e: Exception => Failure(e)
        }
    }
  }

  /**
    * Bid at the minimum increment
    *
    * @param auction_id
    * @param offer
    */
  def incrementalSnipe(auction_id: String, offer: Int) = {
    // TODO
  }

  /**
    * Get the minimum increment in bid. Not exposed by YHJ API (since we can't login to YHJ directly)
    * so get this from the deputy service.
    *
    * @param auction_id
    * @param cookies Session cookies
    * @return
    */
  def getMinimumIncrement(auction_id: String)(implicit cookies: Cookies): Future[Try[Short]]
}

trait YahooJapanShopping extends Shopper {
  implicit private val url: Uri = "http://store.shopping.yahoo.co.jp"

  /**
    * Get the price of the item and buy it
    *
    * @param item_url
    */
  def buyYhj(item_url: Uri): Future[Try[Boolean]] = {
    getItemInfo(item_url) flatMap {
      case Success(itemInfo) =>
        buy(itemInfo)
      case Failure(e) =>
        Future.failed(e)
    }
  }

  private def getItemInfo(item_url: Uri): Future[Try[ItemInfo]] = {
    implicit val request = Get(item_url) ~> addHeaders(headers)

    requestToResponse map { response =>
      try {
        val root = Jsoup.parse(response.entity.asString)
        val name = root.select("title").text()
        val price = root.select("meta[itemprop=price]").attr("content").toInt
        Success(ItemInfo(name, price, item_url))
      }
      catch {
        case e: Exception => Failure(e)
      }
    }
  }
}
