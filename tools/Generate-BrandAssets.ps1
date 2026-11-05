param(
    [string] $ProjectRoot = (Split-Path $PSScriptRoot -Parent),
    [string] $ExportDirectory
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$ProjectRoot = [IO.Path]::GetFullPath($ProjectRoot)
$resourceRoot = Join-Path $ProjectRoot 'app\src\main\res'
$brandRoot = Join-Path $ProjectRoot 'app\brand'
New-Item -ItemType Directory -Force $brandRoot | Out-Null
if ($ExportDirectory) { New-Item -ItemType Directory -Force $ExportDirectory | Out-Null }
$orange = [Drawing.ColorTranslator]::FromHtml('#FF7043')
$white = [Drawing.Color]::White
$invariant = [Globalization.CultureInfo]::InvariantCulture
function Number([double] $Value) { $Value.ToString('0.###', $invariant) }
function Write-Utf8([string] $Path, [string] $Text) {
    New-Item -ItemType Directory -Force ([IO.Path]::GetDirectoryName($Path)) | Out-Null
    [IO.File]::WriteAllText($Path, $Text, [Text.UTF8Encoding]::new($false))
}
function Rounded-Rectangle([single] $X, [single] $Y, [single] $Width, [single] $Height, [single] $Radius) {
    $path = [Drawing.Drawing2D.GraphicsPath]::new()
    $d = 2 * $Radius
    $path.AddArc($X,$Y,$d,$d,180,90)
    $path.AddArc(($X+$Width-$d),$Y,$d,$d,270,90)
    $path.AddArc(($X+$Width-$d),($Y+$Height-$d),$d,$d,0,90)
    $path.AddArc($X,($Y+$Height-$d),$d,$d,90,90)
    $path.CloseFigure()
    return ,$path
}
function Arrow-Points([double] $Angle) {
    $radians=$Angle*[Math]::PI/180
    $px=54+26*[Math]::Cos($radians); $py=54+26*[Math]::Sin($radians)
    $ux=-[Math]::Sin($radians); $uy=[Math]::Cos($radians)
    return ,([Drawing.PointF[]]@(
        [Drawing.PointF]::new(($px+7*$ux),($py+7*$uy)),
        [Drawing.PointF]::new(($px-5*$ux-6*$uy),($py-5*$uy+6*$ux)),
        [Drawing.PointF]::new(($px-5*$ux+6*$uy),($py-5*$uy-6*$ux))
    ))
}
$arrows=@((Arrow-Points 340),(Arrow-Points 160))
$trianglePaths=@($arrows | ForEach-Object {
    'M'+(Number $_[0].X)+','+(Number $_[0].Y)+' L'+(Number $_[1].X)+','+(Number $_[1].Y)+' L'+(Number $_[2].X)+','+(Number $_[2].Y)+' Z'
})
$barPath='M37,45 H39 Q41,45 41,47 V61 Q41,63 39,63 H37 Q35,63 35,61 V47 Q35,45 37,45 Z M41,48 H45 V60 H41 Z M45,52 H63 V56 H45 Z M63,48 H67 V60 H63 Z M69,45 H71 Q73,45 73,47 V61 Q73,63 71,63 H69 Q67,63 67,61 V47 Q67,45 69,45 Z'
$arcPath='M29.568,45.107 A26,26 0,0 1,76.517,41 M78.432,62.893 A26,26 0,0 1,31.483,67'
$svgMark=@"
  <path d="$arcPath" fill="none" stroke="#FFFFFF" stroke-width="5.6" stroke-linecap="round"/>
  <path d="$($trianglePaths -join ' ')" fill="#FFFFFF"/>
  <path d="$barPath" fill="#FFFFFF" transform="rotate(-35 54 54)"/>
"@
$svg="<svg xmlns=`"http://www.w3.org/2000/svg`" width=`"512`" height=`"512`" viewBox=`"18 18 72 72`">`n  <rect x=`"18`" y=`"18`" width=`"72`" height=`"72`" fill=`"#FF7043`"/>`n$svgMark</svg>`n"
$transparentSvg="<svg xmlns=`"http://www.w3.org/2000/svg`" width=`"512`" height=`"512`" viewBox=`"18 18 72 72`">`n$svgMark</svg>`n"
Write-Utf8 (Join-Path $brandRoot 'repsync-icon.svg') $svg
Write-Utf8 (Join-Path $brandRoot 'repsync-mark.svg') $transparentSvg

function Vector-Mark([string] $Width, [string] $Viewport, [string] $GroupOpen, [string] $GroupClose) {
@"
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="$Width" android:height="$Width" android:viewportWidth="$Viewport" android:viewportHeight="$Viewport">
    $GroupOpen
    <path android:pathData="$arcPath" android:fillColor="@android:color/transparent" android:strokeColor="#FFFFFFFF" android:strokeWidth="5.6" android:strokeLineCap="round" />
    <path android:pathData="$($trianglePaths -join ' ')" android:fillColor="#FFFFFFFF" />
    <group android:rotation="-35" android:pivotX="54" android:pivotY="54">
        <path android:pathData="$barPath" android:fillColor="#FFFFFFFF" />
    </group>
    $GroupClose
</vector>
"@
}
Write-Utf8 (Join-Path $resourceRoot 'drawable\ic_launcher_foreground.xml') (Vector-Mark '108dp' '108' '' '')
Write-Utf8 (Join-Path $resourceRoot 'drawable\ic_launcher_monochrome.xml') (Vector-Mark '108dp' '108' '' '')
Write-Utf8 (Join-Path $resourceRoot 'drawable\ic_repsync_mark.xml') (Vector-Mark '24dp' '72' '<group android:translateX="-18" android:translateY="-18">' '</group>')
Write-Utf8 (Join-Path $resourceRoot 'drawable\ic_notification.xml') (Vector-Mark '24dp' '72' '<group android:translateX="-18" android:translateY="-18">' '</group>')
Write-Utf8 (Join-Path $resourceRoot 'drawable\ic_launcher_background.xml') @'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FF7043" android:pathData="M0,0 H108 V108 H0 Z" />
</vector>
'@
foreach($version in @('v26','v33')) {
    $mono = if($version -eq 'v33') { '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />' } else { '' }
    $xml=@"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
$mono
</adaptive-icon>
"@
    foreach($name in @('ic_launcher','ic_launcher_round')) { Write-Utf8 (Join-Path $resourceRoot "mipmap-anydpi-$version\$name.xml") $xml }
}

function Render-Icon([int] $Size, [string] $Shape, [string] $File) {
    # Render large, then downsample for clean legacy icons and consistent small sizes.
    $canvasSize=1024
    $bitmap=[Drawing.Bitmap]::new($canvasSize,$canvasSize,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics=[Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode=[Drawing.Drawing2D.SmoothingMode]::AntiAlias
    if($Shape -eq 'square') { $graphics.Clear($orange) } else { $graphics.Clear([Drawing.Color]::Transparent) }
    $graphics.ScaleTransform(($canvasSize/72.0),($canvasSize/72.0))
    $background=[Drawing.SolidBrush]::new($orange)
    $foreground=[Drawing.SolidBrush]::new($white)
    if($Shape -eq 'circle') { $graphics.FillEllipse($background,[single]0,[single]0,[single]72,[single]72) }
    elseif($Shape -eq 'rounded') { $p=Rounded-Rectangle 0 0 72 72 16; $graphics.FillPath($background,$p); $p.Dispose() }
    elseif($Shape -eq 'square') { $graphics.FillRectangle($background,[single]0,[single]0,[single]72,[single]72) }
    $graphics.TranslateTransform(-18,-18)
    $pen=[Drawing.Pen]::new($white,[single]5.6)
    $pen.StartCap=[Drawing.Drawing2D.LineCap]::Round; $pen.EndCap=[Drawing.Drawing2D.LineCap]::Round
    $graphics.DrawArc($pen,[single]28,[single]28,[single]52,[single]52,[single]200,[single]130)
    $graphics.DrawArc($pen,[single]28,[single]28,[single]52,[single]52,[single]20,[single]130)
    foreach($points in $arrows) { $graphics.FillPolygon($foreground,$points) }
    $state=$graphics.Save()
    $graphics.TranslateTransform(54,54); $graphics.RotateTransform(-35); $graphics.TranslateTransform(-54,-54)
    $barbell=[Drawing.Drawing2D.GraphicsPath]::new([Drawing.Drawing2D.FillMode]::Winding)
    foreach($x in @(35,67)) { $p=Rounded-Rectangle $x 45 6 18 2; $barbell.AddPath($p,$false); $p.Dispose() }
    foreach($x in @(41,63)) { $barbell.AddRectangle([Drawing.RectangleF]::new([single]$x,48,4,12)) }
    $barbell.AddRectangle([Drawing.RectangleF]::new(45,52,18,4))
    $graphics.FillPath($foreground,$barbell); $barbell.Dispose()
    $graphics.Restore($state)
    $graphics.Dispose(); $pen.Dispose(); $background.Dispose(); $foreground.Dispose()
    if($Size -eq $canvasSize) { $bitmap.Save($File,[Drawing.Imaging.ImageFormat]::Png) }
    else {
        $result=[Drawing.Bitmap]::new($Size,$Size,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g=[Drawing.Graphics]::FromImage($result)
        if($Shape -eq 'square') {
            $g.Clear($orange)
            $g.CompositingMode=[Drawing.Drawing2D.CompositingMode]::SourceOver
        } else { $g.CompositingMode=[Drawing.Drawing2D.CompositingMode]::SourceCopy }
        $g.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $attributes=[Drawing.Imaging.ImageAttributes]::new()
        $attributes.SetWrapMode([Drawing.Drawing2D.WrapMode]::TileFlipXY)
        $g.DrawImage($bitmap,[Drawing.Rectangle]::new(0,0,$Size,$Size),0,0,$canvasSize,$canvasSize,[Drawing.GraphicsUnit]::Pixel,$attributes)
        $result.Save($File,[Drawing.Imaging.ImageFormat]::Png)
        $attributes.Dispose(); $g.Dispose(); $result.Dispose()
    }
    $bitmap.Dispose()
}
foreach($bucket in @(@('mdpi',48),@('hdpi',72),@('xhdpi',96),@('xxhdpi',144),@('xxxhdpi',192))) {
    $dir=Join-Path $resourceRoot ('mipmap-'+$bucket[0]); New-Item -ItemType Directory -Force $dir | Out-Null
    Render-Icon $bucket[1] 'rounded' (Join-Path $dir 'ic_launcher.png')
    Render-Icon $bucket[1] 'circle' (Join-Path $dir 'ic_launcher_round.png')
    # Replace only the known stock assets; Android cannot have PNG and WebP with the same resource name.
    foreach($name in @('ic_launcher.webp','ic_launcher_round.webp')) {
        $target=[IO.Path]::GetFullPath((Join-Path $dir $name))
        if(-not $target.StartsWith(([IO.Path]::GetFullPath($resourceRoot)+'\'),[StringComparison]::OrdinalIgnoreCase)){throw 'Icon path escaped resource directory'}
        if(Test-Path -LiteralPath $target){Remove-Item -LiteralPath $target}
    }
}
Render-Icon 512 'square' (Join-Path $brandRoot 'repsync-play-store-512.png')
Render-Icon 1024 'transparent' (Join-Path $brandRoot 'repsync-mark.png')
if($ExportDirectory) {
    Render-Icon 512 'rounded' (Join-Path $ExportDirectory 'RepSync-icon-preview.png')
    Render-Icon 512 'circle' (Join-Path $ExportDirectory 'RepSync-icon-round.png')
    Copy-Item -LiteralPath (Join-Path $brandRoot 'repsync-play-store-512.png') -Destination (Join-Path $ExportDirectory 'RepSync-play-store-512.png') -Force
    Copy-Item -LiteralPath (Join-Path $brandRoot 'repsync-mark.svg') -Destination (Join-Path $ExportDirectory 'RepSync-mark.svg') -Force
}
'RepSync vector, adaptive, monochrome, density PNGs and store assets generated.'
