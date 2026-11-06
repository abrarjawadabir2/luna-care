# Cloudflare Pages Project for LunaCare Public Landing Page
resource "cloudflare_pages_project" "website" {
  account_id        = var.cloudflare_account_id
  name              = "lunacare-${var.environment}-web"
  production_branch = "main"

  build_config {
    # Zero runtime build command needed: pure static semantic HTML
    build_command   = ""
    destination_dir = "web"
    root_dir        = ""
  }

  source {
    type = "github"
    config {
      owner                         = "abrarjawadabir2"
      repo_name                     = "luna-care"
      production_branch             = "main"
      pr_comments_enabled           = true
      deployments_enabled           = true
      production_deployment_enabled = true
      preview_deployment_setting    = "all"
    }
  }

  deployment_configs {
    production {
      environment_variables = {
        NODE_VERSION = "20"
      }
    }
    preview {
      environment_variables = {
        NODE_VERSION = "20"
      }
    }
  }
}

# Custom Domain Association for Cloudflare Pages
resource "cloudflare_pages_domain" "custom_domain" {
  account_id   = var.cloudflare_account_id
  project_name = cloudflare_pages_project.website.name
  domain       = var.domain
}
