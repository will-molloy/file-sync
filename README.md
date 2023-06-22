# file-sync

[![build](https://github.com/will-molloy/file-sync/workflows/build/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-sync/actions?query=workflow%3Abuild)
[![integration-test](https://github.com/will-molloy/file-sync/workflows/integration-test/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-sync/actions?query=workflow%3Aintegration-test)
[![performance-test](https://github.com/will-molloy/file-sync/workflows/performance-test/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-sync/actions?query=workflow%3Aperformance-test)
[![release](https://github.com/will-molloy/file-sync/workflows/release/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-sync/actions?query=workflow%3Arelease)
[![codecov](https://codecov.io/gh/will-molloy/file-sync/branch/main/graph/badge.svg)](https://codecov.io/gh/will-molloy/file-sync)

## Rules

- if file only on source, creates file on destination
- if file on source AND destination, updates file on destination
- if file only on destination, deletes file on destination

**⚠ This is not backup software ⚠**

## Build and test

Requires JDK 19.

```bash
./gradlew spotlessApply build integrationTest performanceTest
```

## Usage

Pull and run the docker container.
Args depend on the backup type.

### Local Backup

Backup to locally mounted storage. E.g. another disk or NAS.

```bash
docker pull ghcr.io/will-molloy/file-sync-local:latest
docker run --rm -v <source_path>:/source:ro -v <destination_path>:/destination ghcr.io/will-molloy/file-sync-local
```

### S3 Backup

Backup to AWS S3 bucket (Glacier Deep Archive).

```bash
docker pull ghcr.io/will-molloy/file-sync-s3:latest
docker run --rm -v <source_path>:/source:ro -e DESTINATION_BUCKET=<bucket_name> -e DESTINATION_BUCKET_PREFIX=<bucket_prefix> ghcr.io/will-molloy/file-sync-s3
```

## Schedules

Write a script wrapping the `docker` commands and use e.g. Windows Task Scheduler.

## Notifications

### Discord

Enable discord notifications via webhook:

```bash
-e DISCORD_WEBHOOK=<full_webhook_url>
```

## Project layout

| Module                                   | Description                         |
|------------------------------------------|-------------------------------------|
| [file-sync-core](./file-sync-core)   | Core backup interface and algorithm |
| [file-sync-local](./file-sync-local) | Local backup implementation         |
| [file-sync-s3](./file-sync-s3)       | S3 backup implementation            |