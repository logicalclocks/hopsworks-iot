package com.logicalclocks.iot.db

import akka.actor.ActorRef
import cats.data.State
import cats.syntax.option._
import com.logicalclocks.iot.db.DomainDb.Action
import com.logicalclocks.iot.db.DomainDb.ActionResult
import com.logicalclocks.iot.db.DomainDb.Add
import com.logicalclocks.iot.db.DomainDb.AddMeasurementsToDatabase
import com.logicalclocks.iot.db.DomainDb.AddNewPending
import com.logicalclocks.iot.db.DomainDb.DbOutput
import com.logicalclocks.iot.db.DomainDb.DeleteSingle
import com.logicalclocks.iot.db.DomainDb.DeleteSingleRecord
import com.logicalclocks.iot.db.DomainDb.GetBatch
import com.logicalclocks.iot.db.DomainDb.GetMeasurements
import com.logicalclocks.iot.db.DomainDb.UpdateDeviceBlockStatus

object DatabaseServiceSm {

  def compose(action: Action, sender: ActorRef): State[DatabaseServiceState, ActionResult] =
    for {
      updateResult <- updateDeviceStatus(action)
      addResult <- addMeasurementsToDatabase(action)
      getResult <- getMeasurementsFromDatabase(action, sender)
      deleteResult <- deleteRecordsFromDatabase(action)
    } yield ActionResult(
      dbOutputs = List(updateResult, addResult, getResult, deleteResult).flatten)

  def updateDeviceStatus(action: Action): State[DatabaseServiceState, Option[DbOutput]] =
    State[DatabaseServiceState, Option[DbOutput]] { s =>
      action match {
        case UpdateDeviceBlockStatus(endpoint, true) =>
          (s.copy(blockedDevicesEndpoints = s.blockedDevicesEndpoints + endpoint), none[DbOutput])
        case UpdateDeviceBlockStatus(endpoint, false) =>
          (s.copy(blockedDevicesEndpoints = s.blockedDevicesEndpoints - endpoint), none[DbOutput])
        case _ =>
          (s, none[DbOutput])
      }
    }

  def addMeasurementsToDatabase(action: Action): State[DatabaseServiceState, Option[DbOutput]] =
    State[DatabaseServiceState, Option[DbOutput]] { s =>
      action match {
        case AddMeasurementsToDatabase(measurements) =>
          (s, Add(measurements).some)
        case _ =>
          (s, none[DbOutput])
      }
    }

  def getMeasurementsFromDatabase(action: Action, sender: ActorRef): State[DatabaseServiceState, Option[DbOutput]] =
    State[DatabaseServiceState, Option[DbOutput]] { s =>
      action match {
        case GetMeasurements =>
          (s, GetBatch(s.pendingForACK.size + 100).some)
        case _ =>
          (s, none[DbOutput])
      }
    }

  def deleteRecordsFromDatabase(action: Action): State[DatabaseServiceState, Option[DbOutput]] =
    State[DatabaseServiceState, Option[DbOutput]] { s =>
      action match {
        case DeleteSingleRecord(id) =>
          (s.copy(pendingForACK = s.pendingForACK - id), DeleteSingle(id).some)
        case _ =>
          (s, none[DbOutput])
      }
    }

  def addNewPending(action: Action): State[DatabaseServiceState, Option[DbOutput]] =
    State[DatabaseServiceState, Option[DbOutput]] { s =>
      action match {
        case AddNewPending(list) =>
          (s.copy(pendingForACK = s.pendingForACK ++ list.map(_._1)), none[DbOutput])
        case _ =>
          (s, none[DbOutput])
      }
    }

  case class DatabaseServiceState(
    blockedDevicesEndpoints: Set[String] = Set.empty[String],
    pendingForACK: Set[Int] = Set.empty[Int])
}
