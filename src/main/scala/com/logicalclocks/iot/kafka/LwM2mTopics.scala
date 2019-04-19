package com.logicalclocks.iot.kafka

sealed trait LwM2mTopics {
  def objectId: Int
  def name: String
}
object LwM2mTopics {
  case object PRESENCE extends LwM2mTopics { val objectId = 3302; val name = "topic-lwm2m-3302-presence" }
  case object TEMPERATURE extends LwM2mTopics { val objectId = 3303; val name = "topic-lwm2m-3303-temperature" }

  val values = Seq(PRESENCE, TEMPERATURE)

  def findNameByObjectId(objectId: Int): Option[String] =
    values
      .find(_.objectId == objectId)
      .map(_.name)
}
