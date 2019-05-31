package com.logicalclocks.iot.commons

import java.util.Properties
import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._

case class PropertiesReader(props: Properties = new Properties()) {

  def addResource(resourceBasename: String, configName: String): PropertiesReader = {
    val config = if (resourceBasename.equals("")) ConfigFactory.load().getConfig(configName)
                 else ConfigFactory.load(resourceBasename).getConfig(configName)
    val newProps = props
    config.entrySet.asScala.foreach { e =>
      newProps.put(
        e.getKey,
        e.getValue.render().replace("\"", ""))
    }
    PropertiesReader(newProps)
  }
}
