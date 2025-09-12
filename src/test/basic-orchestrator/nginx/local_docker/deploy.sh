cd ../../../../../
mvn clean package
export USER_DATA='{"external_port": 9000, "name": "nginx-service"}'
export DSL_METADATA='{"flavour": "local_docker", "stage":"deploy", "stateConfig": {"provider":"S3",
"uri":"s3://odin-components-state-stag/odin-component-interface-nginx-test.tfstate"}}'
cd src/test/groovy/nginx || exit
groovy -cp ../../../../target/odin-component-interface-*-SNAPSHOT-jar-with-dependencies.jar component.groovy
