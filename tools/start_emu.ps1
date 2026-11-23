$emu = "C:\Users\12452\AppData\Local\Android\Sdk\emulator\emulator.exe"
$logDir = "C:\dsh\wuzhi\tools\preview\emu30-host"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$argList = @("-avd","wuzhi30","-no-window","-no-audio","-no-boot-anim","-no-snapshot","-gpu","host","-accel","on","-no-metrics","-netdelay","none","-netspeed","full")
$p = Start-Process -FilePath $emu -ArgumentList $argList -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput "$logDir\emulator.out.log" -RedirectStandardError "$logDir\emulator.err.log"
$p.Id | Set-Content "$logDir\pid.txt"
Write-Output ("started pid " + $p.Id)
