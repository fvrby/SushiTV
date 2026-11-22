# Íconos del menú lateral de SushiTV (96 px, cuadrado redondeado): bandera de Chile, Pluto TV, nota
# musical y globo terráqueo. El de Pluto es su ícono oficial (marcas/pluto-tv.png, de
# pluto.tv/favicon/large.png); los demás se dibujan aquí.
# Uso:  powershell -ExecutionPolicy Bypass -File sushitv\tools\make-rail-icons.ps1
param([string]$Res = (Join-Path $PSScriptRoot "..\..\app\src\main\res\drawable-nodpi"))
Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = "Stop"
$S = 96; $R = 22
function C([string]$hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }
function Brush([string]$hex) { New-Object System.Drawing.SolidBrush (C $hex) }
function Grad([string]$a, [string]$b) {
    $p0 = New-Object System.Drawing.Point 0, 0; $p1 = New-Object System.Drawing.Point $S, $S
    return New-Object System.Drawing.Drawing2D.LinearGradientBrush $p0, $p1, (C $a), (C $b)
}
function Rounded([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $p.AddArc($x, $y, $r * 2, $r * 2, 180, 90); $p.AddArc($x + $w - $r * 2, $y, $r * 2, $r * 2, 270, 90)
    $p.AddArc($x + $w - $r * 2, $y + $h - $r * 2, $r * 2, $r * 2, 0, 90); $p.AddArc($x, $y + $h - $r * 2, $r * 2, $r * 2, 90, 90)
    $p.CloseFigure(); return $p
}
function Star([float]$cx, [float]$cy, [float]$ro, [float]$ri) {
    $pts = for ($i = 0; $i -lt 10; $i++) {
        $a = -[math]::PI / 2 + $i * [math]::PI / 5; $r = if ($i % 2 -eq 0) { $ro } else { $ri }
        New-Object System.Drawing.PointF ($cx + $r * [math]::Cos($a)), ($cy + $r * [math]::Sin($a))
    }
    return [System.Drawing.PointF[]]$pts
}
function New-Icon([string]$name, [scriptblock]$draw) {
    $bmp = New-Object System.Drawing.Bitmap $S, $S, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = "AntiAlias"; $g.PixelOffsetMode = "HighQuality"
    $clip = Rounded 0 0 $S $S $R
    $g.SetClip($clip)
    & $draw $g
    $g.ResetClip()
    $g.DrawPath((New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(40, 255, 255, 255)), 2), (Rounded 1 1 ($S - 2) ($S - 2) ($R - 1)))
    $g.Dispose()
    $out = Join-Path $Res "sushitv_rail_$name.png"; $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
    Write-Output "wrote $out"
}

New-Icon "chile" {
    param($g)
    $g.FillRectangle((Brush "#FFFFFF"), 0, 0, $S, $S / 2)
    $g.FillRectangle((Brush "#D52B1E"), 0, $S / 2, $S, $S / 2)
    $g.FillRectangle((Brush "#0039A6"), 0, 0, $S / 2, $S / 2)
    $g.FillPolygon((Brush "#FFFFFF"), (Star ($S / 4) ($S / 4) 15 6))
}
New-Icon "pluto" {
    param($g)
    $g.FillRectangle((Brush "#000000"), 0, 0, $S, $S)
    $logo = [System.Drawing.Image]::FromFile((Join-Path $PSScriptRoot "marcas\pluto-tv.png"))
    $g.InterpolationMode = "HighQualityBicubic"
    $g.DrawImage($logo, 4, 4, $S - 8, $S - 8)
    $logo.Dispose()
}
New-Icon "music" {
    param($g)
    $g.FillRectangle((Grad "#8B5CF6" "#DB2777"), 0, 0, $S, $S)
    $w = Brush "#FFFFFF"
    $g.FillEllipse($w, 22, 58, 22, 17); $g.FillEllipse($w, 54, 52, 22, 17)
    $g.FillRectangle($w, 38, 26, 6, 40); $g.FillRectangle($w, 70, 20, 6, 40)
    $beam = New-Object System.Drawing.Drawing2D.GraphicsPath
    $beam.AddPolygon([System.Drawing.PointF[]]@((New-Object System.Drawing.PointF 38, 26), (New-Object System.Drawing.PointF 76, 18), (New-Object System.Drawing.PointF 76, 30), (New-Object System.Drawing.PointF 38, 38)))
    $g.FillPath($w, $beam)
}
New-Icon "world" {
    param($g)
    $g.FillRectangle((Grad "#22D3EE" "#1D4ED8"), 0, 0, $S, $S)
    $disc = New-Object System.Drawing.Drawing2D.GraphicsPath; $disc.AddEllipse(18, 18, 60, 60)
    $g.FillPath((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(60, 255, 255, 255))), $disc)
    $pen = New-Object System.Drawing.Pen (C "#FFFFFF"), 4
    $g.SetClip($disc, "Intersect")
    $g.DrawEllipse($pen, 34, 18, 28, 60)                 # meridians
    $g.DrawLine($pen, 48, 16, 48, 80)
    $g.DrawLine($pen, 16, 48, 80, 48)                    # equator
    $g.DrawLine($pen, 20, 33, 76, 33); $g.DrawLine($pen, 20, 63, 76, 63)
    $g.ResetClip(); $g.SetClip((Rounded 0 0 $S $S $R))
    $g.DrawEllipse($pen, 18, 18, 60, 60)
}
