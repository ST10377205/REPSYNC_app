# RepSync identity assets

The RepSync mark combines a dumbbell (repetitions and training) with two circular
arrows (synchronisation). The colour version uses the app's existing coral orange
`#FF7043` and white. It has no small lettering, baked-in shadow, or default Android
robot artwork.

- `repsync-icon.svg`: editable full-square master, with a 72-unit visible canvas.
- `repsync-mark.svg` and `repsync-mark.png`: transparent white mark.
- `repsync-play-store-512.png`: 512 × 512 full-square, opaque, 32-bit PNG for the
  Google Play listing. Let Google Play apply the store mask and shadow.

Android resources include a full-bleed 108 dp adaptive background, a vector
foreground within the central safe region, a separate Android 13+ monochrome
layer, and legacy normal/round PNGs at 48, 72, 96, 144, and 192 pixels. API 26+
adaptive icons and API 33+ themed icons use dedicated resource qualifiers.

The same mark appears in notification icons, login/signup headers, and the
Android 12+ startup screen. Normal application colours and layouts are preserved.

To regenerate the vectors and PNGs on Windows:

```powershell
.\tools\Generate-BrandAssets.ps1
```

`-ExportDirectory` optionally writes shareable previews and the store icon to a
specified directory. The generator replaces only the known launcher resource
names and removes their obsolete stock WebP equivalents to avoid duplicate
Android resources. Keep the SVG masters with the project for future editing.

References:
- https://developer.android.com/develop/ui/views/launch/icon_design_adaptive
- https://developer.android.com/distribute/google-play/resources/icon-design-specifications
