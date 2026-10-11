# Domain registry

`registry.json` is the single source of the hostnames each server routes without a network request.
Every host carries its observed status, the reason it is or is not routed, and its sources.

## Files

| File | Role |
| --- | --- |
| `research/<date>/` | A research snapshot: `domains.json` (every candidate host with status and sources), `classifications.json`, `fixtures-index.json`. |
| `curate.py` | Applies the routing rules to a snapshot and writes `registry.json`. |
| `registry.json` | Curated hosts per service, with `routed`, `reason`, families and subdomain apexes. |
| `server-src/*/…/<Service>Hosts.kt` | Generated from `registry.json`. Do not edit. |

## Routing rules

Applied by `curate.py`, in order:

1. Hosts the code supported before the registry existed are kept (no regressions).
2. Hosts reported as unrelated or repurposed are excluded.
3. `live` hosts are routed.
4. `redirects` hosts are routed unless they redirect to an ad/tracking sink or to another service.
5. Hosts that a routed host of the same service redirected to are routed (observed rotation chain).
6. `unverified` hosts are routed only when their name carries the service brand.
7. `dead` and `parked` hosts are excluded; the fallback identifies them if they come back.

A host claimed by two services is a hard error. Families (bounded regular expressions such as
`dood\.[a-z]{2,6}`) are declared in `curate.py` and must never match another service's hosts; the
`HostRoutingTest` suite enforces this.

## Updating

```shell
# 1. Add a new research snapshot under research/<date>/ (same files as 2026-10-11).
# 2. Curate it into registry.json.
python3 -I tools/domain-registry/curate.py tools/domain-registry/research/<date>
# 3. Regenerate the Kotlin host lists and verify them.
./gradlew generateHostRegistry checkHostRegistry
# 4. Run the routing, fixture and similarity suites.
./gradlew :moongetter-core:jvmTest :moongetter-server-bundle:jvmTest
```

For a one-off host, edit `registry.json` directly (add a host with `"routed": true` and a reason),
then run steps 3 and 4.

## Drift check

`MOONGETTER_DOMAIN_DRIFT=1 ./gradlew :moongetter-server-bundle:jvmTest --tests '*DomainDriftLiveTest*' --rerun`
requests each registry host's root page and writes `moongetter-server-bundle/build/reports/domain-drift/`
(`report.json`, `summary.md`). It lists routed hosts that are now dead or parked, and excluded hosts that came
back. It never fails a build. Results depend on the
network location: bot protection and regional blocking show up as `unverified`.

Hosts reported by `ResolutionEvent.UnknownHostConfirmed` in production are the other input: they were
identified by the fallback and confirmed by a successful extraction.
