provider "aws" {
  region  = "us-east-1"
}

provider "helm" {
  kubernetes {
    config_path = "{{ componentMetadata.kubeConfigPath }}"
  }
}

provider "kubernetes" {
  config_path = "{{ componentMetadata.kubeConfigPath }}"
}

terraform {
  required_providers {
    helm = {
      source  = "hashicorp/helm"
      version = "2.5.1"
    }
  }
}
