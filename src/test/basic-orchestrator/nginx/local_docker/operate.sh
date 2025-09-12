cd ../../../../../
mvn clean package
export USER_DATA='{"count": 1, "external_port": 9000, "name": "nginx-service"}'
export DSL_METADATA='{"flavour": "local_docker", "stage":"operate", "config" : {"operationName" : "redeploy"} }'
cd src/test/groovy/nginx || exit
groovy -cp ../../../../target/odin-component-interface-*-jar-with-dependencies.jar component.groovy
