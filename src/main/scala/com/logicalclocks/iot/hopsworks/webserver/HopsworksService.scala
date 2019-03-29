package com.logicalclocks.iot.hopsworks.webserver

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.StandardRoute
import spray.json._
import DefaultJsonProtocol._

trait HopsworksService {
  val route: Route = {
    (get & pathEndOrSingleSlash) {
      complete(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, """{"message":"ok"}""")))
    }
  }

}
