output "pages_project_name" {
  description = "Name of the Cloudflare Pages project"
  value       = cloudflare_pages_project.website.name
}

output "pages_subdomain" {
  description = "Default subdomain for the Cloudflare Pages deployment"
  value       = "${cloudflare_pages_project.website.name}.pages.dev"
}

output "tunnel_id" {
  description = "ID of the Cloudflare Tunnel connecting EC2 to Cloudflare"
  value       = cloudflare_tunnel.ec2_backend.id
}

output "tunnel_cname" {
  description = "Target CNAME for routing traffic through Cloudflare Tunnel"
  value       = "${cloudflare_tunnel.ec2_backend.id}.cfargotunnel.com"
}

output "api_hostname" {
  description = "Public API hostname routed to the backend"
  value       = "api.${var.domain}"
}
