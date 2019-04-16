# hops-iot-gateway
This is the IoT Gateway part of the project that integrates IoT and Big Data. It will be the only secure end-to-end open source system supporting ingesting IoT data into Big Data platforms. The framework will also provide generic support for automated classification of IoT data to support anomaly detection, and denial-of-service attacks, providing mitigating measures, such as automated exclusion of misbehaving devices and dropping of traffic from sources of DoS attacks.

## other components
Hopsworks development branch: [iot-thesis](https://github.com/kai-chi/hopsworks/tree/iot-thesis)

## architecture
![text](/screens/SystemArchitecture.jpg)

## long story short
Any IoT device talking LWM2M can connect to an IoT Gateway using Raw Public Keys. The data collected by the gateway is be passed to Kafka broker and processed with Spark Streaming jobs. The data is visualised in Grafana and stored in Hops-FS (Hadoop)

## timeline

- [x] Milestone 1 - IoT  
- [x] Milestone 2 - Kafka Producer   
- [ ] Milestone 3 - Hopsworks API, Gateway API   
- [ ] Milestone 4 - DatabaseService   
- [ ] Milestone 4.5 - Front-end  
- [ ] Milestone 5 - Streaming jobs  
- [ ] Milestone 6 - Testing & benchmarking  
- [ ] Milestone 7 - Visualisation & storage  
- [ ] Milestone 8 - Defending the thesis  


## contributions
this project is currently a master thesis project. no contributions are accepted but in case of any suggestions you can open an issue.
