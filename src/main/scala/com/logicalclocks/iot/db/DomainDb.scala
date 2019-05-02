package com.logicalclocks.iot.db

import cats.kernel.Monoid
import com.logicalclocks.iot.lwm2m.IpsoObjectMeasurement

object DomainDb {

  sealed trait Action

  final case class AddMeasurementsToDatabase(measurements: Iterable[IpsoObjectMeasurement]) extends Action
  final object GetMeasurements extends Action
  final case class UpdateDeviceBlockStatus(endpoint: String, block: Boolean) extends Action
  final object StopDb extends Action
  final case class DeleteSingleRecord(id: Int) extends Action
  final case class AddNewPending(list: List[(Int, IpsoObjectMeasurement)]) extends Action

  sealed trait DbOutput
  case class Add(measurements: Iterable[IpsoObjectMeasurement]) extends DbOutput
  case class GetBatch(batchSize: Int) extends DbOutput
  case class DeleteSingle(id: Int) extends DbOutput

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
