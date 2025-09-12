package aerospike

import com.dream11.Odin

Odin.component {
    dslVersion "1.0.0"

    flavour {
        name "container"
        deploy {
            if (hasLastState()) {
                run "echo '${getLastState()}' > terraform.tfstate"
            }

            run "terraform init"
            run "terraform apply -auto-approve"
            out "cat terraform.tfstate"

            discovery {
                run "echo '${getLastState()}' > terraform.tfstate"
                run "terraform output ips"
            }
        }

        healthcheck {
            linearRetryPolicy {
                count 5
                intervalSeconds 6
            }

            tcp {
                port "3000"
            }
        }

        undeploy {
            run "terraform init"
            run "terraform destroy -auto-approve"
        }
    }
}
