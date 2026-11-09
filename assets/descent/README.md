# Roulette Descent pixel graphics

Original programmatic pixel artwork generated locally with Pillow for this staging delivery. No external image assets, paid generators, or downloaded textures were used. This statement describes the generation source only and does not make a legal ownership determination.

## Palette and rendering
- Unified dusk palette: blue-black masonry and glass, warm amber/orange fire, dense blue-gray smoke, green emergency exit.
- Hard integer-pixel edges only; no antialiasing. Scale in-game with nearest-neighbor sampling.
- Ledge collision surfaces are the continuous bright 4 px top bands. `ledge-cracked.png` preserves the same top silhouette while adding a bold warning crack. `ledge-broken.png` is intentionally transparent and fragmented.
- `building-wall.png` is fully opaque and tileable on 32 px boundaries. `sky.png` contains only sky/haze/stars and no foreground ground plane or large building silhouette.

## Animation and alpha
- `fire-strip.png`: four 32x32 frames laid left-to-right in a 128x32 RGBA strip; transparent background.
- `smoke-a.png`, `smoke-b.png`: 64x64 alternate dense smoke masses. Overlap them near the lower screen; center/base alpha is roughly 235-245 and stepped outer alpha is 150-210 so stacked sprites obscure terrain without soft filtering.
- `coin.png`: transparent 16x16 pickup.
- RGBA utility tiles retain transparency where silhouettes require it. Opaque environment tiles use RGBA for interface consistency; `sky.png` is RGB.

## Preview
`contact-sheet.png` shows each source at 1:1 and a nearest-neighbor enlargement. Checkerboards indicate transparency.
