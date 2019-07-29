# hops-iot-gateway
This is the IoT Gateway part of the project that integrates IoT and Big Data. It will be the only secure end-to-end open source system supporting ingesting IoT data into Big Data platforms. The framework will also provide generic support for automated classification of IoT data to support anomaly detection, and denial-of-service attacks, providing mitigating measures, such as automated exclusion of misbehaving devices and dropping of traffic from sources of DoS attacks.

## other components
Hopsworks dev branch: [hopsworks](https://github.com/kai-chi/hopsworks/tree/iot-thesis)<br/>
Hops-util dev branch: [hops-util](https://github.com/kai-chi/hops-util/tree/iot-thesis)

## architecture
<img src="https://raw.githubusercontent.com/logicalclocks/hopsworks-iot/master/screens/SystemArchitecture.jpg" width="500">

## long story short
Any IoT device talking LWM2M can connect to an IoT Gateway using Raw Public Keys. The data collected by the gateway is passed to Kafka broker and processed with Spark Streaming jobs. The data is visualised in Grafana and stored in HopsFS (Hadoop)

## installation
soon...
