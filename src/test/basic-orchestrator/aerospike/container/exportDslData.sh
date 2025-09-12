cd ../../../../../
mvn clean package
export USER_DATA='{}'
export COMPONENT_METADATA='{"componentMetadata": { "kubeConfigPath": "~/.kube", "name": "aerospike", "envName": "test-aerospike", "credentials": {"username": "user", "password": "pass"} }}'
export DSL_METADATA='{"flavour": "container", "stage":"preDeploy"}'
cd src/test/groovy/aerospike || exit
groovy -cp ../../../../target/odin-component-interface-1.0.0-SNAPSHOT-jar-with-dependencies.jar -DexportDslData=true component.groovy
