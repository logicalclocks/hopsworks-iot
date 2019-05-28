package com.logicalclocks.iot.hopsworks.webserver

import akka.http.scaladsl.model.headers.{ `Access-Control-Allow-Origin`, `Access-Control-Allow-Credentials`, `Access-Control-Allow-Headers` }
import akka.http.scaladsl.server.Directives.respondWithHeaders
import akka.http.scaladsl.server.{ Directive0, Route }

trait CorsSupport {

  private def addAccessControlHeaders: Directive0 = {
    respondWithHeaders(
      `Access-Control-Allow-Origin`.*,
      `Access-Control-Allow-Credentials`(true),
      `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With"))
  }

  def corsHandler(r: Route) = addAccessControlHeaders { r }
}
