#!/usr/bin/env python3
"""Inspect an existing P01 stack or save a private gateway rollback snapshot.

Never deploys/restarts containers. No third-party packages are required.
"""
from __future__ import annotations
import argparse
from copy import deepcopy
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

IMAGE = "owasp/modsecurity-crs@sha256:c95914e1855c41791e6745c2a63864a218f99e04948b64d8b31cb7b29b1ba5b3"
SAFE_ENV = {"MODSEC_RULE_ENGINE", "BLOCKING_PARANOIA", "DETECTION_PARANOIA", "ANOMALY_INBOUND", "ANOMALY_OUTBOUND", "MODSEC_AUDIT_ENGINE", "MODSEC_AUDIT_LOG_TYPE", "MODSEC_AUDIT_LOG_FORMAT", "MODSEC_AUDIT_LOG_PARTS", "MODSEC_AUDIT_LOG", "ALLOWED_METHODS"}

def run(argv, cwd):
    result = subprocess.run(argv, cwd=cwd, capture_output=True, text=True, timeout=90)
    if result.returncode:
        # Docker stderr and config may contain interpolated credentials.
        raise ValueError("Command failed: " + argv[0] + " (exit " + str(result.returncode) + "); inspect locally without sharing secrets")
    return result.stdout

def environment(values):
    if isinstance(values, dict):
        return values
    return dict(item.split("=", 1) for item in values or [] if "=" in item)

def private_dir(path, repo):
    path = path.resolve()
    if path == repo or repo in path.parents:
        raise ValueError("Private snapshot must be outside the repository")
    if path.exists():
        raise ValueError("Output directory already exists; choose a fresh run directory")
    path.mkdir(parents=True, mode=0o700)
    os.chmod(path, 0o700)
    return path

def save(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    os.chmod(path, 0o600)

def validate_config(config):
    services = config["services"]
    gateway = services["nginx"]
    env = environment(gateway.get("environment"))
    errors = []
    if gateway.get("image") != IMAGE:
        errors.append("WAF image digest differs from reviewed digest")
    required = {"BLOCKING_PARANOIA": "1", "DETECTION_PARANOIA": "1", "ANOMALY_INBOUND": "5", "ANOMALY_OUTBOUND": "4", "MODSEC_AUDIT_LOG_PARTS": "AZ", "MODSEC_AUDIT_LOG_FORMAT": "JSON", "MODSEC_AUDIT_LOG_TYPE": "Serial", "MODSEC_AUDIT_ENGINE": "RelevantOnly", "MODSEC_AUDIT_LOG": "/var/log/modsecurity/audit/waf-audit.jsonl", "ALLOWED_METHODS": "GET HEAD POST OPTIONS PUT PATCH DELETE"}
    for key, expected in required.items():
        if str(env.get(key)) != expected:
            errors.append(key + " does not match reviewed policy")
    if env.get("MODSEC_RULE_ENGINE") not in ("DetectionOnly", "On"):
        errors.append("WAF mode must be DetectionOnly or On")
    for service in ("postgres", "backend", "frontend"):
        if services[service].get("ports"):
            errors.append(service + " must not publish host ports")
    ports = gateway.get("ports", [])
    if len(ports) != 1 or str(ports[0].get("target")) != "8443":
        errors.append("Only gateway HTTPS target 8443 may be published")
    mounts = {v["target"]: v for v in gateway.get("volumes", [])}
    for target in ("/etc/nginx/templates/nginx.conf.template", "/etc/nginx/ssl"):
        volume = mounts.get(target, {})
        if volume.get("type") != "bind" or not volume.get("read_only"):
            errors.append(target + " must be a read-only bind mount")
        elif not Path(volume["source"]).exists():
            errors.append(target + " source is missing")
    for target in ("/var/log/nginx", "/var/log/modsecurity/audit"):
        if mounts.get(target, {}).get("type") != "volume":
            errors.append(target + " requires a named volume")
    return errors

def snapshot_config(config, current, output):
    """Pin old image/env and copy bind FILES so later checkout cannot change rollback.

    TLS directories remain at the original path. Deploy uses a separate TLS copy.
    All existing named volume names are reused as external volumes.
    """
    gateway = deepcopy(config["services"]["nginx"])
    gateway["image"] = current["Image"]
    gateway["environment"] = environment(current["Config"].get("Env"))
    gateway.pop("build", None)
    gateway.pop("depends_on", None)
    gateway.pop("profiles", None)
    gateway.pop("pull_policy", None)
    mounts = {m["Destination"]: m for m in current["Mounts"]}
    for index, volume in enumerate(gateway.get("volumes", [])):
        actual = mounts.get(volume["target"])
        if actual is None:
            raise ValueError("Configured gateway mount differs from running gateway; snapshot before checkout")
        if volume["type"] == "bind":
            src = Path(actual["Source"])
            if src.is_file() and src.suffix in (".conf", ".template"):
                dst = output / ("bind-" + str(index) + "-" + src.name)
                shutil.copy2(src, dst)
                os.chmod(dst, 0o600)
                # Read-only config must remain readable by the container's user.
                os.chmod(dst, 0o644)
                volume["source"] = str(dst)
            else:
                volume["source"] = str(src)
        elif volume["type"] == "volume":
            name = actual.get("Name")
            if not name:
                raise ValueError("Named-volume identity missing")
            volume["source"] = "rollback_" + str(index)
        else:
            raise ValueError("Unsupported gateway mount; review rollback manually")
    rollback = {"services": {"nginx": gateway}, "networks": {}, "volumes": {}}
    running_networks = current["NetworkSettings"]["Networks"]
    for index, (key, net) in enumerate(running_networks.items()):
        logical = "rollback_net_" + str(index)
        rollback["networks"][logical] = {"external": True, "name": key}
    gateway["networks"] = {key: {} for key in rollback["networks"]}
    for volume in gateway.get("volumes", []):
        if volume["type"] == "volume":
            actual = mounts[volume["target"]]
            rollback["volumes"][volume["source"]] = {"external": True, "name": actual["Name"]}
    return rollback

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["snapshot", "check"])
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--project", required=True, help="Existing Compose project; never guess a new name")
    parser.add_argument("--compose-file", action="append", required=True, help="Repeat in the live stack's order, including overrides")
    parser.add_argument("--sha", help="Full reviewed source SHA, required for check")
    parser.add_argument("--output", type=Path, required=True, help="Fresh private directory outside Git")
    args = parser.parse_args()
    repo = args.repo.resolve()
    compose = ["docker", "compose", "--project-directory", str(repo), "-p", args.project]
    files = []
    for name in args.compose_file:
        file = Path(name)
        if not file.is_absolute():
            file = repo / file
        file = file.resolve(strict=True)
        files.append(str(file))
        compose += ["-f", str(file)]
    config = json.loads(run(compose + ["config", "--format", "json"], repo))
    containers = {}
    for service in ("nginx", "postgres", "backend", "frontend"):
        identifier = run(compose + ["ps", "-q", service], repo).strip()
        if not identifier or "\n" in identifier:
            raise ValueError("Expected one running container for " + service)
        current = json.loads(run(["docker", "inspect", identifier], repo))[0]
        labels = current["Config"].get("Labels") or {}
        if labels.get("com.docker.compose.project") != args.project or labels.get("com.docker.compose.service") != service:
            raise ValueError("Container/project identity mismatch for " + service)
        if not current["State"].get("Running"):
            raise ValueError(service + " is not running")
        containers[service] = current
    gateway = containers["nginx"]
    labels = gateway["Config"]["Labels"]
    actual_files = [str(Path(x).resolve()) for x in labels.get("com.docker.compose.project.config_files", "").split(",") if x]
    if actual_files != files:
        raise ValueError("Compose file list/order differs from the live gateway; include its overrides")
    sha = run(["git", "rev-parse", "HEAD"], repo).strip()
    if args.action == "check":
        if not args.sha or len(args.sha) != 40 or sha != args.sha:
            raise ValueError("HEAD is not the exact reviewed SHA")
        if run(["git", "status", "--porcelain", "--untracked-files=normal"], repo).strip():
            raise ValueError("Tracked/untracked source changes present; review before deploy")
        errors = validate_config(config)
        if errors:
            raise ValueError("; ".join(errors))
    output = private_dir(args.output, repo)
    if args.action == "snapshot":
        save(output / "rollback.compose.json", snapshot_config(config, gateway, output))
    manifest = {"observed_at": datetime.now(timezone.utc).isoformat(), "action": args.action,
        "source_sha": sha, "project": args.project, "compose_files": files,
        "planned_gateway_image": config["services"]["nginx"].get("image"),
        "planned_waf": {k: v for k, v in environment(config["services"]["nginx"].get("environment")).items() if k in SAFE_ENV},
        "running_waf": {k: v for k, v in environment(gateway["Config"].get("Env")).items() if k in SAFE_ENV},
        "containers": {service: {"id": c["Id"], "image_id": c["Image"], "started_at": c["State"].get("StartedAt"),
            "health": c["State"].get("Health", {}).get("Status", "not_configured")}
            for service, c in containers.items()},
        "note": "Manifest is metadata only. Snapshot may contain secrets: keep private. No deploy, syntax test or HTTP request was executed."}
    save(output / "manifest.json", manifest)
    print(json.dumps({"status": "PASS", "action": args.action, "project": args.project, "output": str(output)}, ensure_ascii=False))

if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError, subprocess.SubprocessError) as error:
        print("FAIL: " + str(error), file=sys.stderr)
        sys.exit(1)
