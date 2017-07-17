package com.huddle.integration.hoopla


import akka.actor.{Actor, ActorLogging, Props}
import com.huddle.integration.hoopla.GameSessionHandlingServiceProtocol.HooplaIntegration
import com.huddle.integration.hoopla.GameSessionHandlingServiceProtocol.HooplaIntegration._
import com.mashape.unirest.http.Unirest
import org.json4s._
import org.json4s.jackson.JsonMethods.{parse => _, _}
import org.json4s.native.JsonMethods._
import org.json4s.jackson.Serialization.{read, write}

import scala.concurrent.Future
import scala.util.{Failure, Success}

sealed trait HooplaExchangeMessage
case class AllUsers(l: List[User]) extends HooplaExchangeMessage
case class MetricChosen(m: MetricResource) extends HooplaExchangeMessage
case class MetricValuesRetrieved(l: List[MetricValue])
case class UpdateScore(email: String, newScore: Int) extends  HooplaExchangeMessage

/**
  * Created by nirmalya on 5/6/17.
  */
class HooplaExchangeActor(
         val company: String, val leaderBoard: String, serverConnection: ServerAuthConnection
      ) extends Actor with ActorLogging {

  case class UserScoreDescriptorPair(u: HooplaIntegration.User, desc: Option[String] = None)

  val bearerToken: Option[String] = None
  import scala.concurrent.ExecutionContext.Implicits.global
  var userByEmail: Map[String,HooplaIntegration.User] = Map.empty
  var metricValueByUserScoreHref: Map[String,MetricValue] = Map.empty
  var metricResourceForScore: Option[MetricResource] = None

  getListOfUsers

  override def receive: Receive = {

    case AllUsers(l) =>
      // TODO [NS]: Handle empty list here.
      this.userByEmail = populateEmailSearchableUserDB(l)
      log.info(s"userByEmail loaded: ${userByEmail.mkString(" | ")}")
      grabMetricDetails

    case MetricChosen(m)  =>
      this.metricResourceForScore = Some(m)
      log.info(s"metric resource for score: $metricResourceForScore")
      grabMetricValues

    case MetricValuesRetrieved(l) =>

      metricValueByUserScoreHref = l.foldLeft(Map[String,MetricValue]())((m,nextMetricValue) => {
        m + (nextMetricValue.owner.href -> nextMetricValue)
      })

      log.info(s"metricValues loaded: ${metricValueByUserScoreHref.mkString(" | ")}")

    case UpdateScore(e,s)  =>

      val userInfo = userByEmail(e)
      val metricValueID = metricValueByUserScoreHref(userInfo.href)

      log.info(s"user-email: ${userInfo.email}, user-metric: ${userInfo.href}")
      log.info(s"user-owner-href: ${metricValueID.owner.href}")

      postUpdateScore(metricValueID, s)
  }

  private def postUpdateScore(currentMetricValueID: MetricValue, newScore: Int) = {

    val metriValueWithNewScore = currentMetricValueID.copy(value = newScore)

    val jsonifiedBody = prepareBody(metriValueWithNewScore)

    Future {
          val k = Unirest.put(currentMetricValueID.href)

          k.header("Authorization",   "Bearer "+serverConnection.access_token)
          k.header("Content-type",    "application/vnd.hoopla.metric-value+json")
          k.body(jsonifiedBody)
          k.asJson()

    }.onComplete{
    case Success(r) =>
      log.info(s"r = ${r.getBody}, status = ${r.getStatusText}")
      val l = (parse(r.getBody.toString).extract[HooplaIntegration.MetricValue])
      log.info(s"Score $newScore, updated for user ${l.owner.href}")
    case Failure(ex) =>
      log.error(s"Hoopla: failed to update new score $newScore for ${currentMetricValueID.href}, for user ${currentMetricValueID.owner.href}, ${ex.getMessage}")
  }


  }

  private def grabMetricValues:  Unit = {

    Future {

      Unirest
        .get(metricResourceForScore.get.href +"/values")
        .header("Authorization", "Bearer "+serverConnection.access_token)
        .header("Accept","application/vnd.hoopla.metric-value-list+json")
        .asJson()

    }.onComplete{
      case Success(r) =>
        val l = (parse(r.getBody.toString).extract[List[HooplaIntegration.MetricValue]])
        self ! MetricValuesRetrieved(l)
      case Failure(ex) =>
        log.error(s"Hoopla: no metric value retrieved, ${ex.getMessage}")
        self ! MetricValuesRetrieved(List.empty)
    }

  }
  private def grabMetricDetails: Unit = {

    Future {

      Unirest
        .get("https://api.hoopla.net/metrics")
        .header("Authorization", "Bearer "+serverConnection.access_token)
        .header("Accept","application/vnd.hoopla.metric-list+json")
        .queryString("name","Score")
        .asJson()

    }.onComplete{
      case Success(r) =>
        val l = (parse(r.getBody.toString).extract[List[HooplaIntegration.MetricResource]])
        self ! MetricChosen(l.head)
      case Failure(ex) =>
        log.error(s"Hoopla:no metric retrieved, ${ex.getMessage}")
        //self ! MetricChosen(None)
    }

  }

  private def populateEmailSearchableUserDB(l: List[HooplaIntegration.User]): Map[String, HooplaIntegration.User] = {

    l.foldLeft(Map[String, HooplaIntegration.User]())((m, nextU) => {
      m + (nextU.email -> nextU)
    })
  }
  private def getListOfUsers: Unit = {

    Future {

      Unirest
        .get("https://api.hoopla.net/users")
        .header("Authorization", "Bearer "+serverConnection.access_token)
        .header("Accept","application/vnd.hoopla.user-list+json")
        .asJson()

    }.onComplete{
      case Success(r) =>
        val l = (parse(r.getBody.toString).extract[List[HooplaIntegration.User]])
        self ! AllUsers(l)
      case Failure(ex) =>
        log.error(s"Hoopla:no user retrieved, ${ex.getMessage}")
        self ! AllUsers(List.empty)
    }

  }

  private def prepareBody(metricValueID: MetricValue): String = {
    import JSONUtil._

    val jsonified = JSONUtil.toJson(metricValueID)
    log.info(s"JSONified body: $jsonified")
    jsonified

  }

}

object HooplaExchangeActor {
  def apply(company: String, leaderBoard: String, serverConnection: ServerAuthConnection): Props =
    Props(new HooplaExchangeActor(company: String, leaderBoard: String, serverConnection: ServerAuthConnection))
}
