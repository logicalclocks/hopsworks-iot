package com.logicalclocks

import com.logicalclocks.example.HopsKafkaConsumer

object RunConsumerExample extends App {
  new Thread(new HopsKafkaConsumer).start()
}
