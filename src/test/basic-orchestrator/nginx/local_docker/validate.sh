# Validate stage is useful while releasing the service
cd ../../../../../
mvn clean package
export USER_DATA='{"external_port": 9000, "name": "nginx-service"}'
export DSL_METADATA='{"flavour": "local_docker", "stage":"validate"}'
cd src/test/groovy/nginx || exit
groovy -cp ../../../../target/odin-component-interface-1.0.0-SNAPSHOT-jar-with-dependencies.jar component.groovy
