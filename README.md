# file-backup

[![build](https://github.com/will-molloy/file-backup/workflows/build/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-backup/actions?query=workflow%3Abuild)
[![performance-test](https://github.com/will-molloy/file-backup/workflows/performance-test/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-backup/actions?query=workflow%3Aperformance-test)
[![codecov](https://codecov.io/gh/will-molloy/file-backup/branch/main/graph/badge.svg)](https://codecov.io/gh/will-molloy/file-backup)

## Backup = Mirror

- if file only on source, creates file on destination
- if file on source AND destination, updates file on destination
- if file only on destination, deletes file on destination

## Build and test

```
./gradlew spotlessApply build performanceTest
```

## Usage

Run the main method. Args depend on the backup type.

### Local Backup

Backup to locally mounted storage. E.g. another disk or NAS.

```
local.Main <source_path> <destination_path>
```

### S3 Backup

Backup to AWS S3 bucket.

```
s3.Main <source_path> <destination_bucket> <destination_bucket_prefix>
```

## Project layout

| Module                                   | Description                         |
|------------------------------------------|-------------------------------------|
| [file-backup-core](./file-backup-core)   | Core backup interface and algorithm |
| [file-backup-local](./file-backup-local) | Local backup implementation         |
| [file-backup-s3](./file-backup-s3)       | S3 backup implementation            |