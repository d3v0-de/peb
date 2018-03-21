# about
  This project is meant to offer a platform independant incremental backup tool.
  It's meant to fullfill two main targets:
  - save money - by saving space: each version of a file shall be copied only once - later on possbily by only saving chunks
  - save time - backup should run as fast as possible
  
# features planned
  - be extensible  
  - be platform independant
  - support various targets (like local, SFTP, FTP, Cloud Storages, ...)
  - support various filters to control what is actually backed up
  - incremental backup (no intention to support different aproaches right now)
  - GUI to control all processes + see progress
  - leverage a database for versioncontrol (for now sqlite, mysql or others should also be possible - maybe even useful)
  - allow to delete files(versions) from the backup that have not been seen for a long time
  - possibly add deduplication
  
# status
  This project is in an early stage
  - Currently there's no GUI, not much filtering, only SFTP as backup target - the incremental logic is quite OK
  - configuration via xml-serialization, seems to work
  - only tested on Linux client for now

# next steps:
  - setup build dependencies (to JSCH, jdbc-SQlite, xstream)
  - make sftp more stable
  - rework the current design - which is still somehow prototypish
  - add different targets (first of all local + FTP)
  - add GUI support
  - test on different platforms
