# file-backup

[![build](https://github.com/will-molloy/file-backup/workflows/build/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-backup/actions?query=workflow%3Abuild)
[![integration-test](https://github.com/will-molloy/file-backup/workflows/integration-test/badge.svg?branch=main&event=push)](https://github.com/will-molloy/file-backup/actions?query=workflow%3Aintegration-test)
[![codecov](https://codecov.io/gh/will-molloy/file-backup/branch/main/graph/badge.svg)](https://codecov.io/gh/will-molloy/file-backup)

### Build and test

```
./gradlew spotlessApply build integrationTest
```

### Plan:

Backup = mirror
- if file/directory on src AND not on dest, copy to dest
- if file/directory not on src AND on dest, delete from dest
- if file/directory on src AND dest, update dest
  - use last modified time?

job
  - abstract
  - src
  - dest
  - how to scan src/dest
    - assume src will always be windows? i.e. default implementation here
  - how to copy src -> dest
  - how to delete from dest

NAS job
  - concrete
  - how to scan NAS (dest)
  - how to copy/delete etc.

AWS-S3 job
  - concrete
  - login to AWS/assume role
  - how to scan S3 bucket
  - how to copy/delete etc.

profile
  - src
  - dest
  - job type

job runner
  - reads the profiles
  - instantiates and runs the jobs

scheduler
  - some way to run on a schedule? 
    - Windows scheduler?
  - Otherwise run via bash script adhoc?
    - Similar to auto-handbrake-encoding

Other
- Dockerise?
- Logging, record time etc.
