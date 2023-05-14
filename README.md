# file-backup

[![build](https://github.com/will-molloy/file-backup/workflows/build/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-backup/actions?query=workflow%3Abuild)
[![integration-test](https://github.com/will-molloy/file-backup/workflows/integration-test/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-backup/actions?query=workflow%3Aintegration-test)
[![performance-test](https://github.com/will-molloy/file-backup/workflows/performance-test/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-backup/actions?query=workflow%3Aperformance-test)
[![docker](https://github.com/will-molloy/file-backup/workflows/docker/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-backup/actions?query=workflow%3Adocker)
[![codecov](https://codecov.io/gh/will-molloy/file-backup/branch/main/graph/badge.svg)](https://codecov.io/gh/will-molloy/file-backup)

## Backup = Mirror

- if file only on source, creates file on destination
- if file on source AND destination, updates file on destination
- if file only on destination, deletes file on destination

## Build and test

```bash
./gradlew spotlessApply build integrationTest performanceTest
```

## Usage

Build and run the docker container.
Args depend on the backup type.

### Local Backup

Backup to locally mounted storage. E.g. another disk or NAS.

1. Build image:
   ```bash
   ./gradlew :file-backup-local:jibDockerBuild
   ```

2. Run:
   ```bash
   docker run --rm -v <source_path>:/source -v <destination_path>:/destination file-backup-local
   ```

### S3 Backup

Backup to AWS S3 bucket (Glacier Deep Archive).

1. Build image:
   ```bash
   ./gradlew :file-backup-s3:jibDockerBuild
   ```

2. Run:
   ```bash
   docker run --rm -v <source_path>:/source -e DESTINATION_BUCKET=<bucket_name> -e DESTINATION_BUCKET_PREFIX=<bucket_prefix> file-backup-s3
   ```

## Project layout

| Module                                   | Description                         |
|------------------------------------------|-------------------------------------|
| [file-backup-core](./file-backup-core)   | Core backup interface and algorithm |
| [file-backup-local](./file-backup-local) | Local backup implementation         |
| [file-backup-s3](./file-backup-s3)       | S3 backup implementation            |