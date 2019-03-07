package com.logicalclocks.leshan.listeners

import java.util

import akka.actor.ActorRef
import com.logicalclocks.leshan.LeshanActor.AskForMAC
import com.logicalclocks.leshan.LeshanActor.DisconnectDevice
import org.eclipse.leshan.core.observation.Observation
import org.eclipse.leshan.server.registration.Registration
import org.eclipse.leshan.server.registration.RegistrationListener
import org.eclipse.leshan.server.registration.RegistrationUpdate
import org.slf4j.Logger
import org.slf4j.LoggerFactory

case class HopsRegistrationListener(leshanActor: ActorRef) extends RegistrationListener {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  def registered(reg: Registration, previousReg: Registration, previousObsersations: util.Collection[Observation]): Unit = {
    leshanActor ! AskForMAC(reg)
  }

  def updated(update: RegistrationUpdate, updatedReg: Registration, previousReg: Registration): Unit = {
    logger.debug("device is still here: " + updatedReg.getEndpoint)
  }

  def unregistered(reg: Registration, observations: util.Collection[Observation], expired: Boolean, newReg: Registration): Unit = {
    leshanActor ! DisconnectDevice(reg.getId)
  }
}
