"""Generate density assets from the prepared icon artwork. Requires Pillow."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
source = Image.open(ROOT / 'assets/branding/launcher-artwork.png').convert('RGB')
source.resize((512,512), Image.Resampling.LANCZOS).save(ROOT / 'app/src/main/res/drawable-nodpi/launcher_artwork.png')
source.resize((256,256), Image.Resampling.LANCZOS).save(ROOT / 'docs/icon.png')
