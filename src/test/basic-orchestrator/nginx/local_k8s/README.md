Prerequisite
--

1. `jq` CLI utility is installed
2. K8s cluster is running locally. You can deploy it locally using Minikube by running `minikube start --driver=docker`
3. Namespace by name `component-test` is created
4. Kube API proxy is exposed on port `7979`. On minikube, you can do that by
   running `minikube kubectl proxy --port=7979`
