cd ../../../../../
mvn clean package
export USER_DATA='{"external_port": 9000, "name": "nginx", "namespace": "component-test"}'
export DSL_METADATA='{"flavour": "local_k8s", "stage":"preDeploy"}'
cd src/test/groovy/nginx || exit
groovy -cp ../../../../target/odin-component-interface-1.0.0-SNAPSHOT-jar-with-dependencies.jar -DexportDslData=true component.groovy
