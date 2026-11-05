param([string]$ProjectRoot=(Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$destination=Join-Path $ProjectRoot 'play-store\graphics'
New-Item -ItemType Directory -Force $destination | Out-Null
Copy-Item -LiteralPath (Join-Path $ProjectRoot 'app\brand\repsync-play-store-512.png') -Destination (Join-Path $destination 'app-icon-512.png') -Force
$bitmap=[Drawing.Bitmap]::new(1024,500,[Drawing.Imaging.PixelFormat]::Format24bppRgb)
$g=[Drawing.Graphics]::FromImage($bitmap)
$g.SmoothingMode=[Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint=[Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$bounds=[Drawing.Rectangle]::new(0,0,1024,500)
$background=[Drawing.Drawing2D.LinearGradientBrush]::new($bounds,[Drawing.ColorTranslator]::FromHtml('#FF7043'),[Drawing.ColorTranslator]::FromHtml('#FFD1AF'),28)
$g.FillRectangle($background,$bounds)
$ink=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#35263B'))
$soft=[Drawing.Pen]::new([Drawing.Color]::FromArgb(65,255,255,255),18)
$g.DrawEllipse($soft,660,-165,520,520)
$g.DrawEllipse($soft,720,-105,400,400)
$g.DrawEllipse($soft,-190,350,420,420)
$name=[Drawing.Font]::new('Segoe UI',52,[Drawing.FontStyle]::Bold,[Drawing.GraphicsUnit]::Pixel)
$headline=[Drawing.Font]::new('Segoe UI',61,[Drawing.FontStyle]::Bold,[Drawing.GraphicsUnit]::Pixel)
$body=[Drawing.Font]::new('Segoe UI',25,[Drawing.FontStyle]::Regular,[Drawing.GraphicsUnit]::Pixel)
$g.DrawString('RepSync',$name,$ink,105,84)
$g.DrawString('Build your rhythm.',$headline,$ink,101,184)
$g.DrawString('Plan your workouts. Track your progress.',$body,$ink,107,285)
$g.DrawString('Keep going, online or offline.',$body,$ink,107,326)
$bitmap.Save((Join-Path $destination 'feature-graphic-1024x500.png'),[Drawing.Imaging.ImageFormat]::Png)
$g.Dispose();$bitmap.Dispose();$background.Dispose();$ink.Dispose();$soft.Dispose();$name.Dispose();$headline.Dispose();$body.Dispose()
$svg=@'
<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500" viewBox="0 0 1024 500">
<defs><linearGradient id="bg" x2="1" y2="0.5"><stop stop-color="#ff7043"/><stop offset="1" stop-color="#ffd1af"/></linearGradient></defs>
<rect width="1024" height="500" fill="url(#bg)"/>
<g fill="none" stroke="#fff" stroke-opacity=".25" stroke-width="18"><circle cx="920" cy="95" r="260"/><circle cx="920" cy="95" r="200"/><circle cx="20" cy="560" r="210"/></g>
<g fill="#35263b" font-family="Segoe UI, sans-serif"><text x="105" y="143" font-size="52" font-weight="700">RepSync</text><text x="105" y="251" font-size="61" font-weight="700">Build your rhythm.</text><text x="107" y="315" font-size="25">Plan your workouts. Track your progress.</text><text x="107" y="356" font-size="25">Keep going, online or offline.</text></g></svg>
'@
[IO.File]::WriteAllText((Join-Path $destination 'feature-graphic-source.svg'),$svg,[Text.UTF8Encoding]::new($false))

$raw=Join-Path $ProjectRoot 'play-store\screenshots\raw'
$screenDestination=Join-Path $ProjectRoot 'play-store\screenshots\phone'
New-Item -ItemType Directory -Force $screenDestination | Out-Null
# Deliver the original phone captures without resizing, frames or promotional text.
Get-ChildItem -LiteralPath $raw -Filter *.png -ErrorAction SilentlyContinue | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $screenDestination $_.Name) -Force
}
Write-Output 'Store graphics generated; original phone screenshots copied without overlays.'
