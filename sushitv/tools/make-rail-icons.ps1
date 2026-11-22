# Íconos del menú lateral de SushiTV (96 px, cuadrado redondeado): bandera de Chile, Plutón con su
# corazón, nota musical, claqueta y tele retro. Dibujados aquí (sin logos de marcas).
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
    $g.FillRectangle((Grad "#1E2250" "#0B0D1F"), 0, 0, $S, $S)
    foreach ($s in @(@(12, 14, 2), @(80, 20, 2), @(70, 84, 2), @(18, 78, 1.5), @(86, 56, 1.5))) { $g.FillEllipse((Brush "#FFFFFF"), $s[0], $s[1], $s[2] * 2, $s[2] * 2) }
    $planet = New-Object System.Drawing.Drawing2D.GraphicsPath; $planet.AddEllipse(18, 18, 60, 60)
    $pb = New-Object System.Drawing.Drawing2D.PathGradientBrush $planet
    $pb.CenterPoint = New-Object System.Drawing.PointF 38, 36; $pb.CenterColor = C "#E8C9A0"; $pb.SurroundColors = @((C "#8A5A3C"))
    $g.FillPath($pb, $planet)
    # The heart (Tombaugh Regio), lower left of the disc.
    $h = New-Object System.Drawing.Drawing2D.GraphicsPath
    $h.AddBezier(44, 50, 44, 42, 32, 40, 32, 49); $h.AddBezier(32, 49, 32, 56, 40, 60, 44, 66)
    $h.AddBezier(44, 66, 48, 60, 56, 56, 56, 49); $h.AddBezier(56, 49, 56, 40, 44, 42, 44, 50); $h.CloseFigure()
    $g.FillPath((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(235, 250, 238, 222))), $h)
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
New-Icon "movies" {
    param($g)
    $g.FillRectangle((Grad "#F43F5E" "#9F1239"), 0, 0, $S, $S)
    $g.FillPath((Brush "#FFFFFF"), (Rounded 18 42 60 36 5))
    $g.FillPath((Brush "#1F2937"), (Rounded 18 28 60 13 3))
    $w = Brush "#FFFFFF"
    foreach ($x in @(26, 42, 58)) {
        $g.FillPolygon($w, [System.Drawing.PointF[]]@((New-Object System.Drawing.PointF $x, 28), (New-Object System.Drawing.PointF ($x + 8), 28), (New-Object System.Drawing.PointF ($x + 2), 41), (New-Object System.Drawing.PointF ($x - 6), 41)))
    }
    $g.FillPolygon((Brush "#9F1239"), [System.Drawing.PointF[]]@((New-Object System.Drawing.PointF 42, 50), (New-Object System.Drawing.PointF 42, 70), (New-Object System.Drawing.PointF 58, 60)))
}
New-Icon "series" {
    param($g)
    $g.FillRectangle((Grad "#14B8A6" "#0F766E"), 0, 0, $S, $S)
    $pen = New-Object System.Drawing.Pen (C "#FFFFFF"), 4
    $pen.StartCap = "Round"; $pen.EndCap = "Round"
    $g.DrawLine($pen, 48, 30, 36, 16); $g.DrawLine($pen, 48, 30, 60, 16)
    $g.FillPath((Brush "#FFFFFF"), (Rounded 16 30 64 46 8))
    $g.FillPath((Brush "#134E4A"), (Rounded 22 36 52 34 5))
    $g.FillPolygon((Brush "#FFFFFF"), [System.Drawing.PointF[]]@((New-Object System.Drawing.PointF 42, 44), (New-Object System.Drawing.PointF 42, 62), (New-Object System.Drawing.PointF 57, 53)))
}
