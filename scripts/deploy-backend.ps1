#!/usr/bin/env pwsh
<#
.SYNOPSIS
Deploy backend to Render, Railway, or Fly.io.
#>

param(
    [ValidateSet("render", "railway", "fly")]
    [string]$Platform = "render"
)

$ErrorActionPreference = "Stop"

switch ($Platform) {
    "render" {
        Write-Host "Deploying to Render..."
        Write-Host "1. Push this repo to GitHub"
        Write-Host "2. Go to https://dashboard.render.com/connect/github"
        Write-Host "3. Create new Web Service, select this repo"
        Write-Host "4. Runtime: Docker"
        Write-Host "5. Dockerfile: backend/Dockerfile"
        Write-Host "6. Set environment variables from render.yaml"
        Write-Host "7. Deploy"
        Write-Host ""
        Write-Host "After deployment, copy the backend URL and run:"
        Write-Host "  npx wrangler secret put BACKEND_URL"
    }
    "railway" {
        Write-Host "Deploying to Railway..."
        Write-Host "1. Install Railway CLI: npm i -g @railway/cli"
        Write-Host "2. Run: railway login"
        Write-Host "3. Run: railway init"
        Write-Host "4. Run: railway up"
    }
    "fly" {
        Write-Host "Deploying to Fly.io..."
        Write-Host "1. Install Fly CLI: curl -L https://fly.io/install.ps1 | powershell"
        Write-Host "2. Run: fly auth login"
        Write-Host "3. Run: fly launch --config fly.toml"
    }
}
