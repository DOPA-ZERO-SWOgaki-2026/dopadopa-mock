$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$port = 8000

function Get-ContentType($filePath) {
    $extension = [System.IO.Path]::GetExtension($filePath).ToLowerInvariant()

    switch ($extension) {
        '.html' { return 'text/html; charset=utf-8' }
        '.css' { return 'text/css; charset=utf-8' }
        '.js' { return 'application/javascript; charset=utf-8' }
        '.json' { return 'application/json; charset=utf-8' }
        '.png' { return 'image/png' }
        '.jpg' { return 'image/jpeg' }
        '.jpeg' { return 'image/jpeg' }
        '.svg' { return 'image/svg+xml' }
        '.ico' { return 'image/x-icon' }
        default { return 'application/octet-stream' }
    }
}

function Serve-StaticFile($context) {
    $relativePath = $context.Request.Url.AbsolutePath.TrimStart('/')
    $fullPath = if ([string]::IsNullOrWhiteSpace($relativePath)) {
        [System.IO.Path]::Combine($root, 'index.html')
    } else {
        [System.IO.Path]::Combine($root, $relativePath)
    }

    if (-not [System.IO.File]::Exists($fullPath)) {
        $context.Response.StatusCode = 404
        $body = 'Not Found'
        $buffer = [System.Text.Encoding]::UTF8.GetBytes($body)
        $context.Response.ContentType = 'text/plain; charset=utf-8'
        $context.Response.ContentLength64 = $buffer.Length
        $context.Response.OutputStream.Write($buffer, 0, $buffer.Length)
        $context.Response.OutputStream.Close()
        return
    }

    $contentType = Get-ContentType $fullPath
    $bytes = [System.IO.File]::ReadAllBytes($fullPath)
    $context.Response.StatusCode = 200
    $context.Response.ContentType = $contentType
    $context.Response.ContentLength64 = $bytes.Length
    $context.Response.OutputStream.Write($bytes, 0, $bytes.Length)
    $context.Response.OutputStream.Close()
}

$ip = (Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object { $_.IPAddress -notmatch '^127\.' } |
    Select-Object -First 1).IPAddress

if (-not $ip) {
    $ip = 'localhost'
}

$listener = [System.Net.HttpListener]::new()
$listener.Prefixes.Add("http://*:8000/")
$listener.Prefixes.Add("http://localhost:8000/")

Write-Host "DopaDopa LAN mode"
Write-Host ("Open: http://{0}:{1}" -f $ip, $port)
Write-Host "Use this address from other devices on the same network."
Write-Host "Press Ctrl+C to stop."

$listener.Start()

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        Serve-StaticFile $context
    }
}
finally {
    $listener.Stop()
    $listener.Close()
}
