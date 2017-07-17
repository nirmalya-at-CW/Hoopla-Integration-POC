package com.huddle.integration.hoopla

import com.huddle.integration.hoopla.GameSessionHandlingServiceProtocol.ExternalAPIParams.REQStartAGameWith
import com.huddle.integration.hoopla.GameSessionHandlingServiceProtocol.HooplaIntegration.{MetricResource, MetricValue, MetricUser}
import org.json4s.{DefaultFormats, Formats, ShortTypeHints}

import org.json4s.native.Serialization
import de.heikoseeberger.akkahttpjson4s.Json4sSupport

/**
  * Created by nirmalya on 5/6/17.
  */

object GameSessionHandlingServiceProtocol {

  object HooplaIntegration {

    import org.json4s.JsonDSL._

    case class ServerAuthConnection(
                   
                      access_token: String,
                      refresh_token: String,
                      role: String ,
                      customer_state: String,
                      user_id: String,
                      token_type: String,
                      customer_id: String,
                      expires_in: Int,
                      email: Option[String]
               )

    case class UserAuthResult(
                   access_token: String,
                   refresh_token: String,
                   token_type: String,
                   expires_in: Int,
                   user_id: Option[String],
                   email: Option[String],
                   customer_id: String,
                   customer_state: String,
                   role: String
               )

    case class User(
                 href: String,
                 first_name: String,
                 last_name: String,
                 email: String,
                 confirmed_email: Option[String],
                 updated_at: String
               )

    case class Link(rel: String, href: String)
    case class MetricResource(
                 href: String,
                 name: String,
                 `type`: String,
                 format_rounded_to: Int,
                 currency_code: Option[String],
                 updated_at: String,
                 links: List[Link]
               ) {

    }
    case class AvailableMetrics(metrics: List[MetricResource])
    case class MetricID(href: String) {
      def toJSON = ("href" -> href)
    }
    case class MetricUser(kind: String, href: String) {
      def toJSON = ("kind" -> kind) ~ ("href" -> href)
    }



    case class MetricValue(val href: String,
                           val metric: MetricID,
                           val owner: MetricUser,
                           val value: Int,
                           val updated_at: String
               ) {
      def toJSON = (
              ("href" -> href) ~
              ("metric" -> (
                              "href" -> metric.href
                           )
              ) ~
              ("owner" ->
                ("kind" -> owner.kind) ~ ("href" -> owner.href)
              ) ~
              ("value" -> value) ~
              ("updated_at" -> updated_at)
          )

    }
  }

  object ExternalAPIParams {

    case class REQStartAGameWith(company: String, manager: String, playerID: String, gameName: String, gameUUID: String) {
      override def toString = company + "." + manager + "." + "playerID" + "." + gameName + "." + gameUUID
    }
    case class REQPlayAGameWith(sessionID: String, questionID: String, answerID: String, isCorrect: Boolean, score: Int)
    case class REQPauseAGameWith(sessionID: String)
    case class REQEndAGameWith(sessionID: String)
    case class RESPFromGameSession(desc: String)
  }

  sealed trait GameEndingReason
  case object  GameEndedByPlayer extends GameEndingReason
  case object  GameEndedByTimeOut extends GameEndingReason


  case class GameChosen(company: String, manager: String, playerID: String, gameName: String)
  case class QuestionAnswerTuple(questionID: Int, answerID: Int, isCorrect: Boolean, points: Int)

  case class GameSession(sessionID: String, playerID: String) {
    override def toString = sessionID
  }

  sealed trait GameInfoTupleInREDIS

  case class GameCreatedTupleInREDIS  (flag: String) extends GameInfoTupleInREDIS
  case class GameStartedTupleInREDIS  (t: Long) extends GameInfoTupleInREDIS
  case class GamePlayTupleInREDIS     (t: Long, questionAnswer: QuestionAnswerTuple) extends GameInfoTupleInREDIS
  case class GamePausedTupleInREDIS   (t: Long) extends GameInfoTupleInREDIS
  case class GameEndedTupleInREDIS    (t:Long, gameEndingReason: String) extends GameInfoTupleInREDIS

  case class CompleteGamePlaySessionHistory(elems: List[GameInfoTupleInREDIS])
  object NonExistingCompleteGamePlaySessionHistory extends CompleteGamePlaySessionHistory(elems = List.empty)

  implicit val formats_2 = Serialization.formats(
    ShortTypeHints(
      List(
        classOf[GameCreatedTupleInREDIS],
        classOf[GameStartedTupleInREDIS],
        classOf[GamePlayTupleInREDIS],
        classOf[GamePausedTupleInREDIS],
        classOf[QuestionAnswerTuple],
        classOf[GameEndedTupleInREDIS],
        classOf[GameChosen],
        classOf[REQStartAGameWith],
        classOf[RecordingStatus],
        classOf[MetricResource],
        classOf[MetricUser],
        classOf[MetricValue]
      )
    )
  )

  object HuddleGame
  case class RecordingStatus(details: String)

  case class EmitWhenGameSessionIsFinished(contents: String)


  trait RedisSessionStatus
  case class  FailedRedisSessionStatus(reason: String) extends RedisSessionStatus
  case object OKRedisSessionStatus extends RedisSessionStatus
}
