# Plan 53: Update App Icon

## Problem

The current app icon is a stylized vector lotus shape on a dark gradient background. The user wants to replace it with the image at `/home/pranav/Downloads/YogaFlow Icon.jpg`.

## Current State

The icon setup uses Android's modern adaptive icon system:

- **API 26+ (adaptive icon)**: `mipmap-anydpi-v26/ic_launcher.xml` references a vector foreground (`ic_launcher_foreground.xml` — white lotus shape) and a gradient background (`ic_launcher_background.xml`).
- **Pre-API 26**: Fallback `.webp` bitmaps in 5 density buckets (`mipmap-mdpi` through `mipmap-xxxhdpi`).

Total files to update: 2 adaptive icon XMLs + 1 foreground drawable + 10 mipmap `.webp` files + potentially the background drawable.

## Fix

### Step 1: Copy the image into the project

Copy the JPG to the drawable directory as a PNG (PNG is preferred for launcher icons):

```bash
cp "/home/pranav/Downloads/YogaFlow Icon.jpg" /home/pranav/Code/YogaFlow/app/src/main/res/drawable/ic_launcher_foreground.png
```

Using the `file` command to check the format — if the source is already a suitable raster, convert to PNG with ImageMagick for better compatibility:

```bash
convert "/home/pranav/Downloads/YogaFlow Icon.jpg" /home/pranav/Code/YogaFlow/app/src/main/res/drawable/ic_launcher_foreground.png
```

### Step 2: Update adaptive icon XMLs

**`mipmap-anydpi-v26/ic_launcher.xml`** and **`ic_launcher_round.xml`** — change the foreground to reference the new image:

```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

Remove the `<monochrome>` layer since a JPG/PNG raster image can't serve as a monochrome drawable.

Optionally update `ic_launcher_background.xml` to a solid color that complements the new icon (e.g., `#FFFFFF` white or a color extracted from the image) instead of the current dark gradient.

### Step 3: Regenerate density-specific mipmap bitmaps

For pre-API 26 fallback, generate `.webp` files at each density. Using ImageMagick:

```bash
# mdpi (48x48)
convert ic_launcher_foreground.png -resize 48x48 ../mipmap-mdpi/ic_launcher.webp
convert ic_launcher_foreground.png -resize 48x48 ../mipmap-mdpi/ic_launcher_round.webp

# hdpi (72x72)
convert ic_launcher_foreground.png -resize 72x72 ../mipmap-hdpi/ic_launcher.webp
convert ic_launcher_foreground.png -resize 72x72 ../mipmap-hdpi/ic_launcher_round.webp

# xhdpi (96x96)
convert ic_launcher_foreground.png -resize 96x96 ../mipmap-xhdpi/ic_launcher.webp
convert ic_launcher_foreground.png -resize 96x96 ../mipmap-xhdpi/ic_launcher_round.webp

# xxhdpi (144x144)
convert ic_launcher_foreground.png -resize 144x144 ../mipmap-xxhdpi/ic_launcher.webp
convert ic_launcher_foreground.png -resize 144x144 ../mipmap-xxhdpi/ic_launcher_round.webp

# xxxhdpi (192x192)
convert ic_launcher_foreground.png -resize 192x192 ../mipmap-xxxhdpi/ic_launcher.webp
convert ic_launcher_foreground.png -resize 192x192 ../mipmap-xxxhdpi/ic_launcher_round.webp
```

Note: WebP encoding with ImageMagick may need `-quality 90` flag. Alternatively, use `png` format for the mipmaps (Android supports PNG in mipmap directories) and convert to WebP via Android Studio or `cwebp` tool.

### Step 4: Clean up old vector foreground

Delete the unused vector drawable:

```bash
rm /home/pranav/Code/YogaFlow/app/src/main/res/drawable/ic_launcher_foreground.xml
```

### Step 5: Verify

Build the project to verify the icon compiles and displays correctly:

```bash
cd /home/pranav/Code/YogaFlow && ./gradlew assembleDebug
```

## Alternative: Use Android Studio Image Asset Studio

If ImageMagick is not available, the recommended approach is to open the project in Android Studio and use the **Image Asset Studio** (right-click `res/` → New → Image Asset). This tool:
1. Accepts a raster image file
2. Generates all density-specific PNG/WebP files automatically
3. Updates the adaptive icon XMLs
4. Handles the icon trimming/safe-zone properly

## Files to modify/create

| File | Action |
|---|---|
| `res/drawable/ic_launcher_foreground.png` | **Create** — copy of the new icon image (converted to PNG) |
| `res/drawable/ic_launcher_foreground.xml` | **Delete** — old vector foreground no longer needed |
| `res/drawable/ic_launcher_background.xml` | **Optionally update** — change to solid color or keep as is |
| `res/mipmap-anydpi-v26/ic_launcher.xml` | **Edit** — change foreground reference to `@drawable/ic_launcher_foreground`; remove monochrome layer |
| `res/mipmap-anydpi-v26/ic_launcher_round.xml` | **Edit** — same changes as above |
| `res/mipmap-*/ic_launcher.webp` (×5) | **Replace** — regenerate at each density |
| `res/mipmap-*/ic_launcher_round.webp` (×5) | **Replace** — regenerate at each density |

## Dependencies

- ImageMagick (`convert` + `cwebp`) or Android Studio Image Asset Studio for generating density-specific bitmaps.
