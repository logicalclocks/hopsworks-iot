package com.logicalclocks.leshan

class LeshanServer(
                    coapsHost: String,
                    coapsPort: Int,
                    keyStorePath: Option[String],
                    keyStoreType: Option[String],
                    keyStorePass: Option[String],
                    keyStoreAlias: Option[String],
                    keyStoreAliasPass: Option[String],
                  ) {
  def start(): Unit = {
    println("Start leshan server " + this.toString)
  }

}

object LeshanServer
