# Helpdesk Security Lab — Architecture

## Current Lab Architecture

```mermaid
flowchart TB
    HOST["Windows Host / VirtualBox"]
    VM["Ubuntu VM"]
    DOCKER["Docker Engine"]

    HOST --> VM
    VM --> DOCKER

    subgraph BRIDGE["Default Docker Bridge"]
        WEB["helpdesk-web: 8080"]
        HTTPS["helpdesk-https: 8443"]
        TEST["helpdesk-https-harden-test: 18443"]
    end

    subgraph INTERNAL["helpdesk-internal - Prepared"]
        EMPTY["0 connected containers"]
    end

    DOCKER --> BRIDGE
    DOCKER --> INTERNAL
```

## Current Deployment

- Ubuntu VirtualBox VM runs Docker Engine.
- Three Nginx containers are running.
- Published ports are bound to 127.0.0.1.
- helpdesk-https uses the default Docker bridge.
- All three running containers use the default Docker bridge.
- helpdesk-internal is a user-defined bridge network.
- helpdesk-internal has no connected containers.
- Backend and PostgreSQL are not yet deployed.

## Security Notes

- HTTPS and selected security headers were tested.
- Hardening was verified on a separate test container.
- Fixed credential fallback values were found in Compose.
- UFW is inactive.
- SSH listens on all IPv4/IPv6 interfaces.
- IA-HD07 remains partially verified.

## Evidence

See evidence/EV-HD-28-summary.md.
