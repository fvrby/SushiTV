# Genera los recursos de marca de SushiTV (icono, banner, splash) a partir de la foto de Sushi.
# Uso:  powershell -ExecutionPolicy Bypass -File sushitv\tools\make-brand.ps1
# Requiere solo .NET System.Drawing (incluido en Windows).
param(
    [string]$Photo = (Join-Path $PSScriptRoot "..\..\sushi-icon.png"),
    [string]$Res   = (Join-Path $PSScriptRoot "..\..\app\src\main\res"),
    # Recorte circular de la cara en la foto original (pixeles): centro y radio.
    [int]$Cx = 355, [int]$Cy = 395, [int]$R = 330
)
Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = "Stop"

$Salmon = [System.Drawing.ColorTranslator]::FromHtml("#FF7A59")
$Rice   = [System.Drawing.ColorTranslator]::FromHtml("#F4EFE6")
$Ink    = [System.Drawing.ColorTranslator]::FromHtml("#0E1113")
$Ink2   = [System.Drawing.ColorTranslator]::FromHtml("#1B2227")

$src = [System.Drawing.Image]::FromFile((Resolve-Path $Photo))

function New-Canvas([int]$w, [int]$h) {
    $bmp = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = "AntiAlias"; $g.InterpolationMode = "HighQualityBicubic"
    $g.PixelOffsetMode = "HighQuality"; $g.CompositingQuality = "HighQuality"; $g.TextRenderingHint = "AntiAliasGridFit"
    return @($bmp, $g)
}

# Draws the circular cat crop at (x, y) with diameter d, optional ring.
function Draw-Cat($g, [float]$x, [float]$y, [float]$d, [float]$ring = 0, $ringColor = $Salmon) {
    $tmp, $tg = New-Canvas ([int]$d) ([int]$d)
    $srcRect = New-Object System.Drawing.Rectangle ($Cx - $R), ($Cy - $R), (2 * $R), (2 * $R)
    $tg.DrawImage($src, (New-Object System.Drawing.Rectangle 0, 0, ([int]$d), ([int]$d)), $srcRect, "Pixel")
    $tg.Dispose()
    $brush = New-Object System.Drawing.TextureBrush $tmp
    $brush.TranslateTransform($x, $y)
    if ($ring -gt 0) {
        $g.FillEllipse((New-Object System.Drawing.SolidBrush $ringColor), $x - $ring, $y - $ring, $d + 2 * $ring, $d + 2 * $ring)
    }
    $g.FillEllipse($brush, $x, $y, $d, $d)
    $brush.Dispose(); $tmp.Dispose()
}

function Save($bmp, [string]$path) {
    New-Item -ItemType Directory -Force (Split-Path $path) | Out-Null
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output "wrote $path"
}

# 1) Adaptive icon foreground: 432 px = 108 dp @xxxhdpi. Safe zone is the centre 66 dp (264 px).
$bmp, $g = New-Canvas 432 432
Draw-Cat $g 84 84 264
$g.Dispose(); Save $bmp (Join-Path $Res "drawable-nodpi\sushitv_icon_foreground.png"); $bmp.Dispose()

# 2) Launcher banner: 320x180 dp @xhdpi = 640x360 px.
$bmp, $g = New-Canvas 640 360
$bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush (New-Object System.Drawing.Point 0, 0), (New-Object System.Drawing.Point 640, 360), $Ink2, $Ink
$g.FillRectangle($bg, 0, 0, 640, 360)
$glow = New-Object System.Drawing.Drawing2D.GraphicsPath
$glow.AddEllipse(-60, -40, 400, 440)
$pgb = New-Object System.Drawing.Drawing2D.PathGradientBrush $glow
$pgb.CenterColor = [System.Drawing.Color]::FromArgb(70, $Salmon); $pgb.SurroundColors = @([System.Drawing.Color]::FromArgb(0, $Salmon))
$g.FillPath($pgb, $glow)
Draw-Cat $g 44 60 240 8
$font = New-Object System.Drawing.Font "Segoe UI Black", 74, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$fmt = [System.Drawing.StringFormat]::GenericTypographic
$wSushi = $g.MeasureString("Sushi", $font, 1000, $fmt).Width
$x0 = 316; $yText = 118
$g.DrawString("Sushi", $font, (New-Object System.Drawing.SolidBrush $Rice), $x0, $yText, $fmt)
$g.DrawString("TV", $font, (New-Object System.Drawing.SolidBrush $Salmon), ($x0 + $wSushi + 4), $yText, $fmt)
$small = New-Object System.Drawing.Font "Segoe UI Semibold", 20, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$g.DrawString("TV abierta de Chile", $small, (New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(170, $Rice))), ($x0 + 4), ($yText + 96), $fmt)
$g.Dispose(); Save $bmp (Join-Path $Res "drawable-xhdpi\sushitv_banner.png"); $bmp.Dispose()

# 3) Splash icon: 288 dp canvas, shown masked to a 192 dp circle. 576 px nodpi -> 288 dp at xhdpi.
$bmp, $g = New-Canvas 576 576
Draw-Cat $g 96 96 384
$g.Dispose(); Save $bmp (Join-Path $Res "drawable-nodpi\sushitv_splash.png"); $bmp.Dispose()

# 4) Splash branding image (Android 12+, bottom of the launch screen): 200x80 dp @xhdpi = 400x160 px.
$bmp, $g = New-Canvas 400 160
$font = New-Object System.Drawing.Font "Segoe UI Black", 64, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$wSushi = $g.MeasureString("Sushi", $font, 1000, $fmt).Width
$wTv = $g.MeasureString("TV", $font, 1000, $fmt).Width
$x0 = (400 - ($wSushi + 4 + $wTv)) / 2
$g.DrawString("Sushi", $font, (New-Object System.Drawing.SolidBrush $Rice), $x0, 38, $fmt)
$g.DrawString("TV", $font, (New-Object System.Drawing.SolidBrush $Salmon), ($x0 + $wSushi + 4), 38, $fmt)
$g.Dispose(); Save $bmp (Join-Path $Res "drawable-xhdpi\sushitv_splash_name.png"); $bmp.Dispose()

# 5) In-app mark (replaces OwnTV's logo in the rail, home, About...): Sushi in a circle with a
#    salmon ring, transparent outside. 512 px nodpi, drawn scaled to whatever size the UI asks.
$bmp, $g = New-Canvas 512 512
Draw-Cat $g 20 20 472 18
$g.Dispose(); Save $bmp (Join-Path $Res "drawable-nodpi\sushitv_mark.png"); $bmp.Dispose()

$src.Dispose()
