"""Actualiza p0/props.txt con las propiedades de ADB filtradas en memoria."""

import os
from pathlib import Path
import re
import subprocess
import tempfile


DESTINATION = Path(__file__).resolve().parents[1] / "p0" / "props.txt"
SERIAL_PROPERTY = re.compile(
    rb"^\[[^\]\r\n]*serial[^\]\r\n]*\]: \[([^\r\n]*)\](?:\n|$)", re.I | re.M
)


def sanitize(raw):
    data = raw.replace(b"\r\n", b"\n")
    values = set(SERIAL_PROPERTY.findall(data))
    data = SERIAL_PROPERTY.sub(b"", data)
    # Un fabricante puede repetir el identificador en otra propiedad.
    for value in sorted((v for v in values if len(v) >= 6), key=len, reverse=True):
        data = re.sub(re.escape(value), b"<redacted>", data, flags=re.I)
    return data


def main():
    raw = subprocess.check_output(["adb", "shell", "getprop"])
    if not re.search(rb"(?m)^\[ro\.build\.version\.sdk\]: \[\d+\]\r?$", raw):
        raise SystemExit("La captura de getprop está incompleta.")
    data = sanitize(raw)
    tmp = tempfile.NamedTemporaryFile(dir=DESTINATION.parent, delete=False)
    path = Path(tmp.name)
    try:
        with tmp:
            tmp.write(data)
        os.replace(path, DESTINATION)
    finally:
        path.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
