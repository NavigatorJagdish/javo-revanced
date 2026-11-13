"""Verified first-party media vectors; CI needs no external FFmpeg installation."""
from pathlib import Path
import hashlib

VECTORS = {
    'javo-fixture.m4a': '6c7e7e094509c1bbe23a1587562aba938d2978efcfc4168e43a835cf620f131f',
    'javo-fixture.mp4': '1529ca6c5fd5fabdfad51ef53725bf40a1e2e041bd03e4721bbf20a414c0c63f',
    'javo-fixture-video-only.mp4': 'ef2ac25df9457a4330f9fda4e4321f1e59ab3b3a8800e1cba4f6c726c855280a',
}

def copy_media(source: Path, destination: Path):
    validated = {}
    for name, digest in VECTORS.items():
        data = (source / name).read_bytes()
        if len(data) > 65536 or hashlib.sha256(data).hexdigest() != digest:
            raise ValueError(f'Invalid first-party media vector: {name}')
        validated[name] = data
    destination.mkdir(parents=True, exist_ok=True)
    for name, data in validated.items():
        (destination / name).write_bytes(data)
