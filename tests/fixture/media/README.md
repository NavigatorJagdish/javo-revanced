These tiny synthetic test vectors contain a one-second 440 Hz AAC tone and a
black 160 × 96 H.264 video at 24 fps. They contain no downloaded or proprietary
media. They exercise Android MediaExtractor, MediaMuxer and Downloads saving.
The vectors were recovered byte-for-byte from the previously verified first-party
integration test APK and are checked against fixed SHA-256 hashes before copying.

Originally generated using FFmpeg:

```sh
ffmpeg -f lavfi -i sine=frequency=440:duration=1 -c:a aac javo-fixture.m4a
ffmpeg -f lavfi -i color=c=black:s=160x96:r=24:d=1 -i javo-fixture.m4a -c:v libx264 -pix_fmt yuv420p -c:a copy -shortest -movflags +faststart javo-fixture.mp4
ffmpeg -i javo-fixture.mp4 -map 0:v:0 -c:v copy -an -movflags +faststart javo-fixture-video-only.mp4
```

Generation is not required during normal builds. These first-party test assets
are provided under the repository's GPL-3.0-only license.
