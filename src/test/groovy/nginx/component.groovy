package nginx

import com.dream11.Odin

Odin.component {
    dslVersion "2.1.0"

    flavour {
        name "local_docker"
        // optional, if not provided Odin's default runner will be used
        runnerImage "dream11/nginx-runner"

        preDeploy {
            run "echo 'baseConfigWithDefaults : ${getBaseConfigWithDefaults()}'"
            run "echo Running hypothetical pre-deploy script"
        }

        deploy {
            if (hasLastState()) {
                run "echo '${getLastState()}' > terraform.tfstate"
            }

            run "echo flavourConfigWithDefaults : '${getFlavourConfigWithDefaults()}'"
            run "terraform init"
            run "terraform plan"
            run "terraform apply -auto-approve"
            out "cat terraform.tfstate"

            discovery {
                if (hasLastState()) {
                    run "echo '${getLastState()}' > terraform.tfstate"
                }
                run "terraform output ip_address | xargs"
            }
        }

        postDeploy {
            run "echo Running hypothetical post-deploy script"
        }

        healthcheck {
            linearRetryPolicy {
                intervalSeconds 2
                count 3
            }
            script {
                filePath "./script_based_hc.sh"
            }
            tcp {
                port "${data("\$.flavourConfig.external_port")}"
            }
        }

        undeploy {
            run "echo '${getLastState()}' > terraform.tfstate"

            run "terraform init"
            run "terraform plan"
            run "terraform destroy -auto-approve"
        }

        operate {
            name "redeploy"
            run "echo operationConfigWithDefaults : '${getOperationConfigWithDefaults()}'"
            run "ls"
        }
    }

    flavour {
        name "local_k8s"
        // optional, if not provided Odin's default runner will be used
        runnerImage "dream11/nginx-runner"

        deploy {
            // create deployment
            run 'curl --no-progress-meter -k -X POST -H \'Content-Type: application/yaml\' --data "$(cat deployment.yaml)" http://127.0.0.1:7979/apis/apps/v1/namespaces/' + data("\$.namespace") + '/deployments | jq \'.metadata.name\' | xargs > state.info'
            // create service
            run 'curl --no-progress-meter -k -X POST -H \'Content-Type: application/yaml\' --data "$(cat service.yaml)" http://127.0.0.1:7979/api/v1/namespaces/' + data("\$.namespace") + '/services > service.json'
            run 'cat service.json | jq \'.metadata.name\' | xargs >> state.info'
            run 'cat service.json | jq \'.spec.clusterIP\' | xargs >> state.info'
            out "cat state.info"

            discovery {
                run "echo '${getLastState()}' > state.info"
                run "sed -n '3p' state.info"
            }
        }

        healthcheck {
            linearRetryPolicy {
                intervalSeconds 2
                count 3
            }
            http {
                port "${data("\$.flavourConfig.external_port")}"
            }
        }

        undeploy {
            run "echo '${getLastState()}' > state.info"
            run 'curl --no-progress-meter -k -X DELETE http://127.0.0.1:7979/apis/apps/v1/namespaces/' + data("\$.namespace") + '/deployments/"$(sed -n \'1p\' state.info)"'
            run 'curl --no-progress-meter -k -X DELETE http://127.0.0.1:7979/api/v1/namespaces/' + data("\$.namespace") + '/services/"$(sed -n \'2p\' state.info)"'
        }
    }

    flavour {
        name "local_tester"

        preDeploy {
            run "./sample.sh success"
        }

        deploy {

            run "./sample.sh success"
            download {
                provider "S3"
                uri "s3://playgroundv2-generated-templates/banner/banner/application/"
                relativeDestination "templates"
            }
            download {
                provider "S3"
                uri "s3://d11-aerospike-stag/features.conf"
            }
            discovery {
                run "echo 192.168.100.1,192.168.100.2,192.168.100.3"
            }
            out "echo sample_output"
        }

        postDeploy {
            run "./sample.sh success"
        }

        healthcheck {
            linearRetryPolicy {
                intervalSeconds 2
                count 3
            }
            script {
                filePath "./script_based_hc.sh success"
            }
        }

        undeploy {
            run "./sample.sh success"
        }

        caught {
            run "./caught.sh success"
        }

        finalised {
            run "./finalised.sh"
        }

    }
}
