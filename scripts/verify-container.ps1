#Requires -Version 7.0
$ErrorActionPreference = 'Stop'
$containerId = docker compose ps -q gui
if ($LASTEXITCODE -ne 0 -or -not $containerId) { throw 'GUI container is absent' }
$container = (docker inspect $containerId | ConvertFrom-Json)[0]
if ($container.Config.User -ne '10001') { throw 'GUI must run as UID 10001' }
if (-not $container.HostConfig.ReadonlyRootfs) { throw 'Root filesystem must be read-only' }
if ($container.HostConfig.CapDrop -notcontains 'ALL') { throw 'Capabilities must be dropped' }
if (-not ($container.HostConfig.SecurityOpt | Where-Object { $_ -match '^no-new-privileges(?::true)?$' })) {
    throw 'Privilege escalation must be disabled'
}
if ($container.HostConfig.Memory -ne 536870912 -or $container.HostConfig.PidsLimit -ne 256) {
    throw 'Reference memory/PID limits are absent'
}
$temporaryOptions = $container.HostConfig.Tmpfs.'/tmp'
foreach ($requiredOption in @('noexec', 'nosuid', 'size=64m', 'uid=10001', 'gid=10001')) {
    if ($temporaryOptions.Split(',') -notcontains $requiredOption) { throw 'Temporary storage policy mismatch' }
}
if ($container.Mounts | Where-Object Destination -EQ '/var/run/docker.sock') { throw 'Docker socket must not be mounted' }
foreach ($bindings in $container.NetworkSettings.Ports.PSObject.Properties.Value) {
    foreach ($binding in $bindings) {
        if ($binding.HostIp -ne '127.0.0.1') { throw 'GUI ports must bind only to loopback' }
    }
}
$userId = docker compose exec -T gui id -u
if ($LASTEXITCODE -ne 0 -or $userId -ne '10001') { throw 'Runtime UID mismatch' }
docker compose exec -T gui sh -c 'test ! -w /app && test -w /tmp && test -r /licenses/StackEmu-LICENSE'
if ($LASTEXITCODE -ne 0) { throw 'Runtime filesystem/license check failed' }
Write-Output 'PASS: non-root, read-only root, bounded temp/memory/PIDs, dropped capabilities, loopback and licenses'
