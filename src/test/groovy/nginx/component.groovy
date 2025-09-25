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
                attributes([
                        region: "us-east-1"
                ])
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
