# file-backup

[![build](https://github.com/will-molloy/file-backup/workflows/build/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-backup/actions?query=workflow%3Abuild)
[![performance-test](https://github.com/will-molloy/file-backup/workflows/performance-test/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-backup/actions?query=workflow%3Aperformance-test)
[![docker](https://github.com/will-molloy/file-backup/workflows/docker/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-backup/actions?query=workflow%3Adocker)
[![codecov](https://codecov.io/gh/will-molloy/file-backup/branch/main/graph/badge.svg)](https://codecov.io/gh/will-molloy/file-backup)

## Backup = Mirror

- if file only on source, creates file on destination
- if file on source AND destination, updates file on destination
- if file only on destination, deletes file on destination

## Build and test

```bash
./gradlew spotlessApply build performanceTest
```

## Usage

Build and run the docker container (or run the main method directly). 
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

If you need to mount a network drive, run something like:
```bash
docker volume create --driver local --opt type=cifs --opt device="//<SERVER_IP>/<PATH>" --opt o=user='<USER>',password='<PASS>' <VOLUME_NAME>
```

### S3 Backup

Backup to AWS S3 bucket.

```bash
s3.Main <source_path> <destination_bucket> <destination_bucket_prefix>
```

## Project layout

| Module                                   | Description                         |
|------------------------------------------|-------------------------------------|
| [file-backup-core](./file-backup-core)   | Core backup interface and algorithm |
| [file-backup-local](./file-backup-local) | Local backup implementation         |
| [file-backup-s3](./file-backup-s3)       | S3 backup implementation            |