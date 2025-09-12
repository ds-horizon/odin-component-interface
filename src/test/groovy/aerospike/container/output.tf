output ips {
  value = join(",", flatten([for subset in data.kubernetes_endpoints_v1.endpoints.subset : [for address in subset.address : address.ip]]))
}
