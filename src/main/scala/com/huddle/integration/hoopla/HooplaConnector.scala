package com.huddle.integration.hoopla

import java.util.Base64

import akka.actor.ActorSystem
import com.mashape.unirest.http.Unirest
import org.json4s._

import com.huddle.integration.hoopla.GameSessionHandlingServiceProtocol.HooplaIntegration

import com.huddle.integration.hoopla.GameSessionHandlingServiceProtocol.formats_2
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import com.huddle.integration.hoopla.GameSessionHandlingServiceProtocol.HooplaIntegration._


/**
  * Created by nirmalya on 13/7/17.
  */
object HooplaConnector extends App {

  val clientKey = "ffd07524-8f9a-4d78-95bc-3aaa0a3c4bff"
  val clientSecret = "VoAn8LQjsn4z9/FXQg6Qa+BBTZ6eWBXp96udbaXULpY="

  val keyAndSecret = new StringBuffer().append(clientKey).append(":").append(clientSecret).toString
  val keyAndSecretEncoded = Base64.getEncoder.encodeToString(keyAndSecret.getBytes("utf-8"))

  val actorSystem = ActorSystem("HooplaConnector")
  implicit val executionContext = actorSystem.dispatcher

  val t1 = Unirest
    .post("https://api.hoopla.net/oauth2/token")
    .header("Authorization",keyAndSecretEncoded)
    .header("Content-Type", "application/x-www-form-urlencoded")
      .field("client_id",clientKey)
      .field("client_secret",clientSecret)
      .field("grant_type","client_credential")
    .asJson

  val serverConnection = parse(t1.getBody.toString).extract[HooplaIntegration.ServerAuthConnection]

  println(s"ServerConnection: $serverConnection")

  val exchange = actorSystem.actorOf(HooplaExchangeActor("Codewalla", "tic-tac-toe", serverConnection), "Hoopla-Exchange")

  Thread.sleep(5 * 1000)

  exchange ! UpdateScore("vikas.s@codewalla.com", 11)

  Thread.sleep(5  * 1000)

  exchange ! UpdateScore("nirmalya.s@codewalla.com", 8)

  Thread.sleep(7  * 1000)

  exchange ! UpdateScore("shivali.b@codewalla.com", 15)

  Thread.sleep(7  * 1000)

  actorSystem.terminate

  Unirest.shutdown()

}
