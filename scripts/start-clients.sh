#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters"
    exit 
fi

NO_CLIENTS=$1
GW_COAP_HOST=localhost
GW_COAP_PORT=5683
CLIENT_JAR=~/MasterThesis/iot/leshan-client-demo.jar
NODE_PREFIX=node-latency-rem-20

echo "Start $NO_CLIENTS clients for GW @ $GW_COAP_HOST:$GW_COAP_PORT"

for (( i=1; i<=$NO_CLIENTS; i++ ))
do
  echo "$NODE_REFIX-$i"
  rand=$(awk -v "seed=$[(RANDOM & 32767) + 32768 * (RANDOM & 32767)]" 'BEGIN { srand(seed); printf("%.5f\n", rand()*0.2)}')
  sleep $rand
  java -jar $CLIENT_JAR -n $NODE_PREFIX-$i -u $GW_COAP_HOST:$GW_COAP_PORT 2>/dev/null &
done

function trap_ctrlc ()
{
    echo "Killing all clients"
    pkill -f "java -jar $CLIENT_JAR"
    sleep 1
    exit 2
}
 
trap "trap_ctrlc" 2

while true
do
  sleep 1
done
