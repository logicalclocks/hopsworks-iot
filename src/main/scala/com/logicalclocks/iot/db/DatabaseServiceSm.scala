package com.logicalclocks.iot.db

import akka.actor.ActorRef
import cats.data.State
import cats.syntax.option._
import com.logicalclocks.iot.db.DomainDb.Action
import com.logicalclocks.iot.db.DomainDb.ActionResult
import com.logicalclocks.iot.db.DomainDb.Add
import com.logicalclocks.iot.db.DomainDb.AddMeasurementsToDatabase
import com.logicalclocks.iot.db.DomainDb.BlockDeviceByEndpoint
import com.logicalclocks.iot.db.DomainDb.BlockEndpoint
import com.logicalclocks.iot.db.DomainDb.DbOutput
import com.logicalclocks.iot.db.DomainDb.DeleteSingle
import com.logicalclocks.iot.db.DomainDb.DeleteSingleRecord
import com.logicalclocks.iot.db.DomainDb.GetBatch
import com.logicalclocks.iot.db.DomainDb.GetBlockedDevices
import com.logicalclocks.iot.db.DomainDb.GetBlockedEndpoints
import com.logicalclocks.iot.db.DomainDb.GetMeasurements
import com.logicalclocks.iot.db.DomainDb.Stop
import com.logicalclocks.iot.db.DomainDb.StopDb
import com.logicalclocks.iot.db.DomainDb.UnblockDeviceByEndpoint
import com.logicalclocks.iot.db.DomainDb.UnblockEndpoint
import com.logicalclocks.iot.db.DomainDb.UpdateDeviceBlockStatus

object DatabaseServiceSm {

  def compose(action: Action, sender: ActorRef): State[DatabaseServiceState, ActionResult] =
    for {
      updateResult <- updateDeviceStatus(action)
      addResult <- addMeasurementsToDatabase(action)
      getResult <- getMeasurementsFromDatabase(action, sender)
      deleteResult <- deleteRecordsFromDatabase(action)
      blockedResult <- changeBlockedDevices(action)
    } yield ActionResult(
      dbOutputs = List(updateResult, addResult, getResult, deleteResult, blockedResult).flatten)

  def updateDeviceStatus(action: Action): State[DatabaseServiceState, Option[DbOutput]] =
    State[DatabaseServiceState, Option[DbOutput]] { s =>
      action match {
        case UpdateDeviceBlockStatus(endpoint, true) =>
          (s, BlockEndpoint(endpoint).some)
        case UpdateDeviceBlockStatus(endpoint, false) =>
          (s, UnblockEndpoint(endpoint).some)
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
          (s, GetBatch(100).some)
        case _ =>
          (s, none[DbOutput])
      }
    }

  def deleteRecordsFromDatabase(action: Action): State[DatabaseServiceState, Option[DbOutput]] =
    State[DatabaseServiceState, Option[DbOutput]] { s =>
      action match {
        case DeleteSingleRecord(id) =>
          (s, DeleteSingle(id).some)
        case _ =>
          (s, none[DbOutput])
      }
    }

  def stopDatabase(action: Action): State[DatabaseServiceState, Option[DbOutput]] =
    State[DatabaseServiceState, Option[DbOutput]] { s =>
      action match {
        case StopDb =>
          (s, Stop.some)
        case _ =>
          (s, none[DbOutput])
      }
    }

  def changeBlockedDevices(action: Action): State[DatabaseServiceState, Option[DbOutput]] =
    State[DatabaseServiceState, Option[DbOutput]] { s =>
      action match {
        case BlockDeviceByEndpoint(endpoint) =>
          (s, BlockEndpoint(endpoint).some)
        case UnblockDeviceByEndpoint(endpoint) =>
          (s, UnblockEndpoint(endpoint).some)
        case GetBlockedDevices =>
          (s, GetBlockedEndpoints.some)
        case _ =>
          (s, none[DbOutput])
      }
    }

  case class DatabaseServiceState()
}
