# Genera los avatares de SushiTV desde las fotos de la carpeta avatares/ (local, no versionada).
# Cada foto se recorta en un cuadrado centrado en la cara y se guarda a 256 px como
# app/src/main/res/drawable-nodpi/sushitv_avatar_NN.png. La forma redondeada la pone la app.
# Uso:  powershell -ExecutionPolicy Bypass -File sushitv\tools\make-avatars.ps1
param(
    [string]$Photos = (Join-Path $PSScriptRoot "..\..\avatares"),
    [string]$Res    = (Join-Path $PSScriptRoot "..\..\app\src\main\res\drawable-nodpi"),
    [int]$Size = 256
)
Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = "Stop"

# Por foto (en orden alfabético): centro X, centro Y y lado del cuadrado, en píxeles de la foto original.
$crops = @(
    @(511, 420, 620),   # 1  gato naranjo echado
    @(370, 373, 740),   # 2  naranjo de cerca
    @(516, 400, 700),   # 3  naranjo peludo
    @(725, 820, 1380),  # 4  calico (Sushi) de frente
    @(760, 820, 1200),  # 5  calico con snack
    @(528, 451, 800),   # 6  calico sacando la lengua
    @(400, 330, 600),   # 7  blanco y naranjo
    @(561, 480, 900),   # 8  naranjo de cerca
    @(250, 400, 560),   # 9  con chaleco
    @(733, 800, 1300),  # 10 naranjo serio
    @(448, 377, 700)    # 11 blanco y naranjo, ojos grandes
)

$files = Get-ChildItem -Path $Photos -Filter *.png | Sort-Object Name
if ($files.Count -ne $crops.Count) { throw "Hay $($files.Count) fotos y $($crops.Count) recortes definidos." }
New-Item -ItemType Directory -Force $Res | Out-Null
for ($i = 0; $i -lt $files.Count; $i++) {
    $img = [System.Drawing.Image]::FromFile($files[$i].FullName)
    $cx, $cy, $side = $crops[$i]
    $side = [math]::Min($side, [math]::Min($img.Width, $img.Height))
    $x = [math]::Max(0, [math]::Min($cx - $side / 2, $img.Width - $side))
    $y = [math]::Max(0, [math]::Min($cy - $side / 2, $img.Height - $side))
    $bmp = New-Object System.Drawing.Bitmap $Size, $Size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = "HighQualityBicubic"; $g.PixelOffsetMode = "HighQuality"; $g.SmoothingMode = "AntiAlias"
    $g.DrawImage($img, (New-Object System.Drawing.Rectangle 0, 0, $Size, $Size), (New-Object System.Drawing.Rectangle ([int]$x), ([int]$y), ([int]$side), ([int]$side)), "Pixel")
    $g.Dispose(); $img.Dispose()
    $out = Join-Path $Res ("sushitv_avatar_{0:D2}.png" -f ($i + 1))
    $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
    Write-Output "wrote $out"
}
