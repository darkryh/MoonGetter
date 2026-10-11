#!/usr/bin/env python3
"""Builds registry.json from a domain research snapshot.

Usage: python3 -I tools/domain-registry/curate.py tools/domain-registry/research/<date>

The research snapshot (domains.json) lists every candidate host with an observed status. This script
applies the routing rules below and writes tools/domain-registry/registry.json, which is the single
source the Gradle task `generateHostRegistry` turns into Kotlin host lists.

Routing rules, in order:
  1. Hosts the code already supported before this registry existed are kept (no regressions).
  2. Hosts reported as unrelated or repurposed are excluded.
  3. `live` hosts are routed.
  4. `redirects` hosts are routed unless they redirect to an ad/tracking sink or to another service.
  5. Hosts a routed host of the same service redirected to are routed (observed rotation chain).
  6. `unverified` hosts are routed only when their name carries the service brand (they were usually
     blocked by bot protection during the check, and they come from a maintained resolver list).
  7. `dead` and `parked` hosts are excluded; the fallback identifies them if they come back.
A host claimed by two services is a hard error.
"""
import json
import sys
import urllib.parse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

SINKS = {"survey-smiles.com", "iplogger.org", "moneypornvideo.com", "sidesteamroll.com"}

SERVICES = {
    "Abyss": {
        "module": "server-src/moongetter-abyss", "package": "com.ead.lib.moongetter.abyss", "object": "AbyssHosts",
        "brand": ["abyss"],
        "legacy": ["abyss.to", "argtesa.top"],
        "exclude": {"embedplayabyss.top": "Ad iframe wrapper whose frame URL is built by script; not an Abyss player page."},
        "subdomainsOf": ["abyss.to", "argtesa.top"],
        "families": [],
    },
    "Doodstream": {
        "module": "server-src/moongetter-doodstream", "package": "com.ead.lib.moongetter.doodstream", "object": "DoodstreamHosts",
        "brand": ["dood"],
        "legacy": ["doodstream.com", "d-s.io", "dsvplay.com", "playmogo.com"],
        "subdomainsOf": [],
        "families": [
            {"pattern": r"dood\.[a-z]{2,6}", "reason": "Doodstream rotates the TLD of the dood label (dood.to, dood.wf, dood.yt, ...)."},
            {"pattern": r"doodstream\.[a-z]{2,6}", "reason": "Brand label with rotating TLD."},
        ],
    },
    "Filemoon": {
        "module": "server-src/moongetter-filemoon", "package": "com.ead.lib.moongetter.filemoon", "object": "FilemoonHosts",
        "brand": ["byse", "filemoon"],
        "legacy": ["filemoon.sx", "bysekoze.com"],
        "exclude": {"filemoon.org": "Separate resolver with a different /{route}/{id}/stream extraction path."},
        "subdomainsOf": [],
        "families": [],
    },
    "Lulustream": {
        "module": "server-src/moongetter-lulustream", "package": "com.ead.lib.moongetter.lulustream", "object": "LulustreamHosts",
        "brand": ["lulu"],
        "legacy": [],
        "subdomainsOf": [],
        "families": [
            {"pattern": r"lulu(?:stream|vdo|vdoo|vid|vido)\.[a-z]{2,6}", "reason": "Lulu brand spellings with rotating TLD; replaces the unbounded lulu\\w*\\.\\w+."},
        ],
    },
    "RPMStream": {
        "module": "server-src/moongetter-rpmstream", "package": "com.ead.lib.moongetter.rpmstream", "object": "RpmstreamHosts",
        "brand": ["rpm"],
        "legacy": ["rpmstream.live", "rpmvid.com"],
        "subdomainsOf": ["rpmstream.live", "rpmvid.com"],
        "families": [],
    },
    "Streamtape": {
        "module": "server-src/moongetter-streamtape", "package": "com.ead.lib.moongetter.streamtape", "object": "StreamtapeHosts",
        "brand": ["tape"],
        "legacy": ["streamtape.com", "gettapeads.com"],
        "subdomainsOf": [],
        "families": [
            {"pattern": r"streamtape\.[a-z]{2,6}", "reason": "Brand label with rotating TLD."},
            {"pattern": r"strtape\.[a-z]{2,6}", "reason": "Short brand label with rotating TLD."},
        ],
    },
    "Streamwish": {
        "module": "server-src/moongetter-streamwish", "package": "com.ead.lib.moongetter.streamwish", "object": "StreamwishHosts",
        "brand": ["wish"],
        "legacy": ["swhoi.com"],
        "subdomainsOf": [],
        "families": [
            {"pattern": r"streamwish\.[a-z]{2,6}", "reason": "Brand label with rotating TLD; replaces the unbounded [\\w-]*wish[\\w-]*."},
        ],
    },
    "Vidhide": {
        "module": "server-src/moongetter-vihide", "package": "com.ead.lib.moongetter.vihide", "object": "VihideHosts",
        "brand": ["vidhide", "filelions", "lions"],
        "legacy": ["callistanise.com", "niikaplayerr.com"],
        "subdomainsOf": [],
        "families": [
            {"pattern": r"vidhide(?:pro|vip|pre|plus|hub|fast)?\.[a-z]{2,6}", "reason": "Vidhide brand spellings with rotating TLD."},
            {"pattern": r"filelions\.[a-z]{2,6}", "reason": "FileLions (Vidhide) brand with rotating TLD."},
        ],
    },
    "Mixdrop": {
        "module": "server-src/moongetter-mixdrop", "package": "com.ead.lib.moongetter.mixdrop", "object": "MixdropHosts",
        "brand": ["mixdrop", "mxdrop", "mixdrp", "mixdroop", "m1xdrop", "xdrop"],
        "legacy": [],
        "subdomainsOf": [],
        "families": [
            {"pattern": r"m(?:i{1,4})?xdrop\.[a-z]{2,6}", "reason": "Previously accepted m(i)?xdrop.<tld>; extended to the miixdrop spellings."},
            {"pattern": r"(?:mixdrp|mixdroop|m1xdrop)\.[a-z]{2,6}", "reason": "Observed misspelling labels with rotating TLD."},
            {"pattern": r"mixdrop\d{1,3}\.[a-z]{2,6}", "reason": "Numbered mirrors (mixdrop21.net, mixdrop23.net)."},
        ],
    },
    "Voe": {
        "module": "server-src/moongetter-voe", "package": "com.ead.lib.moongetter.voe", "object": "VoeHosts",
        "brand": ["voe"],
        "legacy": ["voe.sx", "markstyleall.com", "shannonpersonalcost.com", "cindyeyefinal.com", "cindyeyefinal.sx", "teresapoliticallearn.com"],
        "subdomainsOf": [],
        "families": [
            {"pattern": r"voeunblock(?:[1-9]|10)?\.com", "reason": "Numbered unblock mirrors voeunblock.com to voeunblock10.com."},
        ],
    },
}


def host_of(value):
    if not value:
        return None
    if "//" not in value:
        return value.lower()
    return (urllib.parse.urlparse(value).hostname or "").lower() or None


def main(research_dir):
    research = Path(research_dir).resolve()
    domains = json.loads((research / "domains.json").read_text())
    classifications = json.loads((research / "classifications.json").read_text())
    repurposed = set(classifications.get("repurposedOrUnrelated", []))
    by_service = {entry["service"]: entry for entry in domains}
    all_hosts = {m["domain"]: entry["service"] for entry in domains for m in entry["mirrors"]}

    services = []
    owner = {}
    for name, spec in SERVICES.items():
        entry = by_service[name]
        mirrors = {m["domain"].lower(): m for m in entry["mirrors"]}
        excluded_reason = spec.get("exclude", {})
        decisions = {}

        def decide(host, routed, reason, mirror=None):
            if host in decisions and decisions[host]["routed"]:
                return
            decisions[host] = {
                "host": host,
                "status": (mirror or {}).get("status", "legacy"),
                "routed": routed,
                "reason": reason,
                "redirectsTo": host_of((mirror or {}).get("redirectsTo")),
                "sources": (mirror or {}).get("sources", []),
            }

        for host, mirror in sorted(mirrors.items()):
            status = mirror["status"]
            target = host_of(mirror.get("redirectsTo"))
            brand = any(token in host for token in spec["brand"])
            if host in spec["legacy"]:
                decide(host, True, "supported before the registry existed", mirror)
            elif host in excluded_reason:
                decide(host, False, excluded_reason[host], mirror)
            elif host in repurposed:
                decide(host, False, "reported as unrelated or repurposed", mirror)
            elif status == "live":
                decide(host, True, "live", mirror)
            elif status == "redirects" and target in SINKS:
                decide(host, False, f"redirects to ad/tracking sink {target}", mirror)
            elif status == "redirects" and target and target in all_hosts and all_hosts[target] != name:
                decide(host, False, f"redirects to another service's host {target}", mirror)
            elif status == "redirects":
                decide(host, True, "redirects within the service" if target else "redirects", mirror)
            elif status == "unverified" and brand:
                decide(host, True, "unverified (bot protection or timeout) but brand-named and resolver-listed", mirror)
            else:
                decide(host, False, status, mirror)

        for host in spec["legacy"]:
            decide(host, True, "supported before the registry existed")

        # Rule 5: rotation targets observed from routed hosts of this service.
        for host, decision in list(decisions.items()):
            target = decision["redirectsTo"]
            if not (decision["routed"] and target and target not in SINKS and target != host):
                continue
            if all_hosts.get(target, name) != name:
                continue
            existing = decisions.get(target)
            if existing is None or (not existing["routed"] and existing["status"] == "unverified"):
                decisions.pop(target, None)
                decide(target, True, f"observed redirect target of {host}", mirrors.get(target))

        for host, decision in decisions.items():
            if decision["routed"]:
                if host in owner and owner[host] != name:
                    raise SystemExit(f"{host} is routed by both {owner[host]} and {name}")
                owner[host] = name

        services.append({
            "service": name,
            "module": spec["module"],
            "package": spec["package"],
            "object": spec["object"],
            "subdomainsOf": spec["subdomainsOf"],
            "families": spec["families"],
            "hosts": sorted(decisions.values(), key=lambda d: (not d["routed"], d["host"])),
        })

    registry = {
        "schemaVersion": 1,
        "researchedAt": "2026-10-11",
        "research": str(research.relative_to(ROOT)),
        "rules": __doc__.split("Routing rules, in order:")[1].strip(),
        "sinks": sorted(SINKS),
        "services": services,
    }
    out = ROOT / "tools/domain-registry/registry.json"
    out.write_text(json.dumps(registry, indent=2) + "\n")
    for service in services:
        routed = sum(1 for h in service["hosts"] if h["routed"])
        print(f"{service['service']:<11} routed={routed:<4} excluded={len(service['hosts']) - routed}")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else str(ROOT / "tools/domain-registry/research/2026-10-11"))
