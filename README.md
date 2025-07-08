# file-sync

> [!CAUTION]
> This project is deprecated. For backup and synchronization needs, consider using [rustic-rs](https://github.com/rustic-rs/rustic) for backups or [rclone](https://rclone.org/) for synchronization instead, which offer better performance, deduplication, compression, and proper backup features.

[![build](https://github.com/will-molloy/file-sync/workflows/build/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-sync/actions?query=workflow%3Abuild)
[![codecov](https://codecov.io/gh/will-molloy/file-sync/branch/main/graph/badge.svg)](https://codecov.io/gh/will-molloy/file-sync)

## Sync = Mirror

- if file only on source, creates file on destination
- if file on source AND destination, updates file on destination
- if file only on destination, deletes file on destination

> [!WARNING]
> This is not backup software

## Build and test

Requires JDK 19.

```bash
./gradlew spotlessApply build integrationTest performanceTest
```

## Usage

Pull and run the docker container.
Args depend on the sync type.

### `LocalSync`

Sync to locally mounted storage. E.g. another disk or NAS.

```bash
docker pull ghcr.io/will-molloy/file-sync-local:latest
docker run --rm -v <source_path>:/source:ro -v <destination_path>:/destination ghcr.io/will-molloy/file-sync-local
```

### `S3Sync`

Sync to AWS S3 bucket (Glacier Deep Archive).

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

| Module                               | Description                       |
|--------------------------------------|-----------------------------------|
| [file-sync-core](./file-sync-core)   | Core sync interface and algorithm |
| [file-sync-local](./file-sync-local) | Local sync implementation         |
| [file-sync-s3](./file-sync-s3)       | S3 sync implementation            |