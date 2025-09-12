resource "helm_release" "{{ componentMetadata.name }}" {
  name                = "{{ componentMetadata.name }}"
  repository          = "https://dreamsports.jfrog.io/artifactory/d11-helm-charts"
  chart               = "aerospike"
  version             = "1.3.2"
  repository_username = "{{ componentMetadata.credentials.username }}"
  repository_password = "{{ componentMetadata.credentials.password }}"
  namespace           = "{{ componentMetadata.envName }}"
  values              = [file("${path.module}/values.yml")]
}

data "kubernetes_endpoints_v1" "endpoints" {
  metadata {
    name      = "{{ componentMetadata.name }}-headless"
    namespace = "{{ componentMetadata.envName }}"
  }
  depends_on = [resource.helm_release.{{ componentMetadata.name }}]
}
