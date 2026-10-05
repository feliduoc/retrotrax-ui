# Emite en JSON la cancion que suena, cada 2 segundos
# script creado con la ayuda de IA (claude)
Add-Type -AssemblyName System.Runtime.WindowsRuntime
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Await($WinRtTask, $ResultType) {
    $asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
        $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and
        $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
    $netTask = $asTaskGeneric.MakeGenericMethod($ResultType).Invoke($null, @($WinRtTask))
    $netTask.Wait(-1) | Out-Null
    $netTask.Result
}

 $null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]

while ($true) {
    try {
        $manager = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
        $session = $manager.GetSessions() | Where-Object {
            $_.GetPlaybackInfo().PlaybackStatus -eq [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus]::Playing
        } | Select-Object -First 1
        if (-not $session) { $session = $manager.GetSessions() | Select-Object -First 1 }

        if ($session) {
            $props = Await ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
            [PSCustomObject]@{
                title  = $props.Title
                artist = $props.Artist
                album  = $props.AlbumTitle
                app    = $session.SourceAppUserModelId
            } | ConvertTo-Json -Compress
        }
    } catch { }
    Start-Sleep -Seconds 2
}