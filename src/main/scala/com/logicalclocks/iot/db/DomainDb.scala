package com.logicalclocks.iot.db

import cats.kernel.Monoid
import com.logicalclocks.iot.lwm2m.Measurement

object DomainDb {

  sealed trait Action

  final case class AddMeasurementsToDatabase(measurements: Iterable[Measurement]) extends Action
  final object GetMeasurements extends Action
  final case class UpdateDeviceBlockStatus(endpoint: String, block: Boolean) extends Action
  final object StopDb extends Action
  final case class DeleteSingleRecord(id: Int) extends Action
  final case class AddNewPending(list: List[(Int, Measurement)]) extends Action
  final case class BlockDeviceByEndpoint(endpoint: String) extends Action
  final case class UnblockDeviceByEndpoint(endpoint: String) extends Action
  final object GetBlockedDevices extends Action

  sealed trait DbOutput
  case class Add(measurements: Iterable[Measurement]) extends DbOutput
  case class GetBatch(batchSize: Int) extends DbOutput
  case class DeleteSingle(id: Int) extends DbOutput
  case class BlockEndpoint(endpoint: String) extends DbOutput
  case class UnblockEndpoint(endpoint: String) extends DbOutput
  final object ClearTables extends DbOutput
  final object GetBlockedEndpoints extends DbOutput
  final object Stop extends DbOutput

  case class ActionResult(dbOutputs: List[DbOutput] = List.empty[DbOutput]) {
    def nonEmpty(): Boolean = dbOutputs.nonEmpty
  }

  object ActionResult {

    implicit val monoid: Monoid[ActionResult] = new Monoid[ActionResult] {
      override def empty: ActionResult = ActionResult()

      override def combine(x: ActionResult, y: ActionResult): ActionResult =
        ActionResult(x.dbOutputs ::: y.dbOutputs)
    }
  }
}
