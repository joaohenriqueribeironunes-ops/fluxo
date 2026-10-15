"""Pré-renderiza assets/audio/*.wav — roda antes do build e localmente."""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fluxo import synth
from fluxo.waves import PADS, WAVES

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "assets", "audio")

made = synth.generate_all(OUT, WAVES, PADS)
for p in made:
    print(f"{os.path.getsize(p) / 1024:8.1f} KB  {os.path.basename(p)}")
print(f"\n{len(made)} arquivos em {OUT}")
