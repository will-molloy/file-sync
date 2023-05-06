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

Run the main method. There are 2 types of backup.

### LocalBackup

```
LocalBackup <source_path> <destination_path>
```

### S3Backup

```
S3Backup <source_path> <destination_bucket> <destination_bucket_prefix>
```

## TODO

- Package the application (docker?)
- Schedules
- Restore
