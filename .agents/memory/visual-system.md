---
name: Visual system rules
description: Programmatic asset generation rules — no raster assets, no external SVGs. Canvas + ImageVector.Builder only.
---

## Rule
All logos and icons are generated programmatically. No PNG, no SVG files, no external image assets.

## NexusPlusLogo (ui/components/NexusPlusLogo.kt)
- Compose Canvas composable (not ImageVector)
- Draws: outer hexagon (primaryColor) + sine-wave arc (accentColor) + two signal nodes
- Hexagon = Nexus structure; wave = Nexus Wave Technologies signature
- Caller sets size via `modifier`, colors via params — fully theme-aware
- Used in WelcomeScreen replacing the old `Icons.Filled.AutoAwesome`

## NseVectorIcon (ui/components/NseVectorIcon.kt)
- ImageVector.Builder — 5 waveform bars (mountain profile) + sine arc above
- Used in FeatureCatalog as the NSE feature icon (or wherever an ImageVector icon is needed)
- Lazy-initialized singleton; standard Material3 tinting applies

## NexusShapes (ui/theme/Shape.kt)
- M3 shape scale: extraSmall(4dp) → small(8dp) → medium(16dp) → large(24dp) → extraLarge(32dp)
- Convenience tokens: ShapeButton, ShapeCard, ShapeChip, ShapeHero for one-off usages
- Wired into MaterialTheme via Theme.kt `shapes = NexusShapes`

## Sacred greeting
`R.string.jai_shri_krishna` = "Jai Shri Krishna". Rendered in HomeScreen via `JaiShriKrishnaGreeting()` composable:
- Surface(primaryContainer) + Row + Text(titleSmall, SemiBold)
- semantics(mergeDescendants=true) { contentDescription = text }
- Also in NexusTtsScreen as `JaiShriKrishnaHeader()`.
