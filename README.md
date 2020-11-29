# toolbox

Sync exif timestamps, Mac timestamps and timestamps contained in the file name of a picture. Core functionality provided by ExifTool by Phil Harvey: https://exiftool.org

1) Choose one of the following use cases:

    *-e*: Use exif timestamps as reference to adapt file name timestamps (if desired, see "-r"), and Mac timestamps. Uses primary exif timestamps (DateTimeOriginal and CreateDate) by default.

    *-f*: Use timestamps contained in file names as reference to adapt exif timestamps and Mac timestamps.
  
    *-v*: Check if primary exif timestamps are equal to the timestamp contained in the file name.


2) Choose on of the following options:

    *-r*: Rename file according to the reference timestamps. Adds the prefix *yyyyMMdd_hhmmss__* to the original picture file name.

    *-s*: If used together with *-e*, not only primary exif timestamps taken into consideration as a reference.


3) Provide the picture directory.
