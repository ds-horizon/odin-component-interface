terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "2.22.0"
    }
  }
}

resource "docker_container" "nginx" {
  image   = "nginx:{{ baseConfig.image }}"
  name  = "{{ baseConfig.name }}"

  network_mode = "host"

  ports {
    internal = {{ baseConfig.internal_port }}
    external = {{ flavourConfig.external_port }}
  }
}
