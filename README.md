# about
  This project is meant to offer a platform independant incremental backup tool.
  It's meant to fullfill two main targets:
  1. save money - by saving space - only backup changes
  2. save time - backup should run as fast as possible - recognize changes fast, only transfer changes  
  
# features 
  mostly planned
  - be extensible  
  - be platform independant
  - support various targets (like local, SFTP, FTP, Cloud Storages, ...)
  - support various filters to control what is actually backed up
  - incremental backup (no intention to support different aproaches right now)
  - GUI to control all processes + see progress
  - leverage a database for versioncontrol (for now sqlite, mysql or others should also be possible - maybe even useful)
  - allow to delete files(versions) from the backup that have not been seen for a configured time
  - possibly backup only changed parts of files - not sure if that's worth the trouble though  
  - possibly add deduplication
  
# status
  This project is in an early stage
  - Currently there's no GUI, not much filtering, only SFTP as backup target - the incremental logic is quite OK
  - configuration via xml-deserialization, seems to work (though there's no built in config creator yet)
  - only tested on Linux client for now

# next steps:
  - rework the current design - which is still somehow prototypish
  - add different targets (first of all local + FTP)
  - add GUI support
  - test on different platforms
