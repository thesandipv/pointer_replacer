---
id: case-study-android-16-rro
title: "Case Study: Fixing RROs for Android 16"
sidebar_label: "Android 16 RRO Case Study"
sidebar_position: 4
description: Technical case study on Runtime Resource Overlays (RRO) failure in Android 16 (Baklava) and how to resolve it in a standalone AAPT2/Python build pipeline.
---

# Case Study: Runtime Resource Overlay (RRO) Invalidation in Android 16

## Executive Summary

Pointer Replacer modifies Android's system touch feedback pointer (*"Show taps"* under Developer Options) using **Runtime Resource Overlays (RRO)** delivered via Magisk modules. In our production architecture, RRO APKs are generated standalone using a Python toolchain (`compile_rro.py`) driving **AAPT2**, **zipalign**, and **apksigner** directly—without relying on Gradle.

On **Android 16 (Baklava)** and **Android 15 QPR1+**, installed RRO overlays suddenly stopped working: devices continued displaying the stock circular touch dot even after flashing and enabling the Magisk module.

This case study details:
1. The architectural shift inside AOSP's Input and View frameworks that caused existing overlays to be bypassed.
2. The exact breakdown of why standalone AAPT2/Python-built overlays failed.
3. The remediation implemented in our build pipeline to achieve cross-version compatibility from Android 10 through Android 16.

---

## 1. Background & Legacy Architecture

When *"Show taps"* (`Settings.System.SHOW_TOUCHES`) is turned on in Developer Options, Android's input pipeline (`PointerController` in `frameworks/native` / `InputManagerService`) renders a visual sprite at each touch coordinate.

### Legacy Touch Pointer Resolution (Android 10 – Android 14)

Under Android 14 and earlier:
1. The framework resolves the system pointer style from `framework-res` (`targetPackage="android"`):
   ```xml
   <!-- com.android.internal.R.style.Pointer -->
   <style name="Pointer">
       <item name="pointerIconSpotTouch">@drawable/pointer_spot_touch_icon</item>
       <item name="pointerIconSpotHover">@drawable/pointer_spot_hover_icon</item>
       <item name="pointerIconSpotAnchor">@drawable/pointer_spot_anchor_icon</item>
   </style>
   ```
2. The `pointer_spot_touch_icon.xml` resource defines a `<pointer-icon>` mapping to a raster bitmap:
   ```xml
   <pointer-icon xmlns:android="http://schemas.android.com/apk/res/android"
       android:bitmap="@drawable/pointer_spot_touch"
       android:hotSpotX="12dp"
       android:hotSpotY="12dp" />
   ```
3. Raster PNGs are provided across screen densities (`drawable-mdpi`, `drawable-hdpi`, `drawable-xhdpi`, `drawable-xxhdpi`, `drawable-xxxhdpi`).

In our original `compile_rro.py` pipeline, the overlay resource map (`res/xml/overlays.xml`) targeted only these legacy identifiers:

```xml
<overlay xmlns:android="http://schemas.android.com/apk/res/android">
    <item target="drawable/pointer_spot_touch" value="@drawable/pointer_spot_touch" />
    <item target="drawable/pointer_spot_hover" value="@drawable/pointer_spot_hover" />
    <item target="drawable/pointer_spot_anchor" value="@drawable/pointer_spot_anchor" />
    <item target="drawable/pointer_spot_touch_icon" value="@drawable/pointer_spot_touch_icon" />
    <item target="drawable/pointer_spot_hover_icon" value="@drawable/pointer_spot_hover_icon" />
    <item target="drawable/pointer_spot_anchor_icon" value="@drawable/pointer_spot_anchor_icon" />
</overlay>
```

---

## 2. Root Cause Analysis: What Changed in Android 16?

### 2.1 The Vector Cursor Overhaul

Starting in **Android 15 QPR1** and finalized in **Android 16 (Baklava)**, Google redesigned Android's cursor subsystem to support dynamic cursor sizing, color customization, and accessibility scaling (*Settings $\rightarrow$ System $\rightarrow$ Touchpad & mouse*). This feature is governed by the platform feature flag `Flags.FLAG_ENABLE_VECTOR_CURSORS`.

In Android 16, this flag is **enabled by default**.

#### AOSP Code: `PointerIcon.java`
Inside `frameworks/base/core/java/android/view/PointerIcon.java`:

```java
public static @NonNull PointerIcon getLoadedSystemIcon(@NonNull Context context, int type,
        boolean useLargeIcons, float pointerScale) {
    ...
    int typeIndex = getSystemIconTypeIndex(type);
    ...
    final int defStyle;
    if (android.view.flags.Flags.enableVectorCursorA11ySettings()) {
        defStyle = com.android.internal.R.style.VectorPointer;
    } else {
        if (useLargeIcons) {
            defStyle = com.android.internal.R.style.LargePointer;
        } else if (android.view.flags.Flags.enableVectorCursors()) {
            defStyle = com.android.internal.R.style.VectorPointer;
        } else {
            defStyle = com.android.internal.R.style.Pointer;
        }
    }

    TypedArray a = context.obtainStyledAttributes(null,
            com.android.internal.R.styleable.Pointer,
            0, defStyle);
    int resourceId = a.getResourceId(typeIndex, -1);
    ...
}
```

Because `enableVectorCursors` evaluates to `true` on Android 16, `PointerIcon` selects **`@style/VectorPointer`** instead of `@style/Pointer`.

#### The New Vector Style: `styles.xml`
In `frameworks/base/core/res/res/values/styles.xml`:

```xml
<!-- Style used on Android 16 -->
<style name="VectorPointer">
    <item name="pointerIconSpotHover">@drawable/pointer_spot_hover_vector_icon</item>
    <item name="pointerIconSpotTouch">@drawable/pointer_spot_touch_vector_icon</item>
    <item name="pointerIconSpotAnchor">@drawable/pointer_spot_anchor_vector_icon</item>
</style>
```

And in `core/res/res/drawable/`:
* `@drawable/pointer_spot_touch_vector_icon` references `@drawable/pointer_spot_touch_vector`.
* `@drawable/pointer_spot_touch_vector.xml` is a 24dp `<vector>` asset defining a modernized concentric ring touch marker.

### 2.2 Why Existing Overlays Failed Silently

When `idmap2` processes our legacy overlay on Android 16:
1. The resource table maps `pointer_spot_touch` and `pointer_spot_touch_icon` without error.
2. At runtime, the input engine queries the touch pointer using `@style/VectorPointer`.
3. The framework requests `pointer_spot_touch_vector_icon` and `pointer_spot_touch_vector`.
4. Because our overlay **never declared mappings for the vector resource IDs**, the system falls back to the default AOSP vector graphic in `framework-res.apk`.
5. The overlaid legacy PNGs remain loaded in memory but are completely ignored by the rendering pipeline.

---

## 3. Secondary Toolchain Pitfalls (No-Gradle Architecture)

Operating a standalone Python + AAPT2 toolchain revealed two additional considerations:

### 3.1 The `android.jar` Symbol Dependency
In `scripts/compile_rro.py`, AAPT2 links against a platform `android.jar`:
```bash
aapt2 link -I <platforms/android-X/android.jar> --manifest AndroidManifest.xml -o unaligned.apk compiled.zip
```
* The symbols `pointer_spot_touch_vector` and `pointer_spot_touch_vector_icon` were only introduced in **API 35 (Android 15)** and **API 36 (Android 16)**.
* If the script links against `android-34.jar`, AAPT2 cannot resolve these resource names in the target package `android`.
* **Requirement**: The build host must provide `android-35.jar` or `android-36.jar`.

### 3.2 Partition Precedence & `OverlayConfig`
In modern Android releases (Android 13+ through 16):
* Android uses `/product/overlay/partition_order.xml` to prioritize overlays across partitions.
* Bundling solely into `system/vendor/overlay/` can cause delays or require manual enabling on devices with strict `OverlayConfig` policies.
* Installing to both `system/product/overlay/` and `system/vendor/overlay/` guarantees maximum compatibility across OEMs and custom ROMs.

---

## 4. The Solution: Dual-Target Resource Architecture

To fix Android 16 while retaining seamless support for Android 10 through 14, we updated `scripts/compile_rro.py` with a dual-target mapping model.

### 4.1 Updated `res/xml/overlays.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<overlay xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Legacy Android (<= 14) Drawables & Pointer Icons -->
    <item target="drawable/pointer_spot_touch" value="@drawable/pointer_spot_touch" />
    <item target="drawable/pointer_spot_hover" value="@drawable/pointer_spot_hover" />
    <item target="drawable/pointer_spot_anchor" value="@drawable/pointer_spot_anchor" />
    <item target="drawable/pointer_spot_touch_icon" value="@drawable/pointer_spot_touch_icon" />
    <item target="drawable/pointer_spot_hover_icon" value="@drawable/pointer_spot_hover_icon" />
    <item target="drawable/pointer_spot_anchor_icon" value="@drawable/pointer_spot_anchor_icon" />

    <!-- Android 15 QPR1+ & Android 16 Vector Pointer Drawables & Icons -->
    <item target="drawable/pointer_spot_touch_vector" value="@drawable/pointer_spot_touch" />
    <item target="drawable/pointer_spot_hover_vector" value="@drawable/pointer_spot_hover" />
    <item target="drawable/pointer_spot_anchor_vector" value="@drawable/pointer_spot_anchor" />
    <item target="drawable/pointer_spot_touch_vector_icon" value="@drawable/pointer_spot_touch_vector_icon" />
    <item target="drawable/pointer_spot_hover_vector_icon" value="@drawable/pointer_spot_hover_vector_icon" />
    <item target="drawable/pointer_spot_anchor_vector_icon" value="@drawable/pointer_spot_anchor_vector_icon" />
</overlay>
```

### 4.2 Generating Pointer Icon Descriptors

In `compile_rro.py`, we emit descriptor XMLs that wrap our custom bitmaps inside `<pointer-icon>` definitions for both legacy and vector icon endpoints:

```python
xml_descriptors = {
    # Legacy descriptors
    "pointer_spot_touch_icon.xml": "@drawable/pointer_spot_touch",
    "pointer_spot_hover_icon.xml": "@drawable/pointer_spot_hover",
    "pointer_spot_anchor_icon.xml": "@drawable/pointer_spot_anchor",
    # Modern Android 16 vector descriptors
    "pointer_spot_touch_vector_icon.xml": "@drawable/pointer_spot_touch",
    "pointer_spot_hover_vector_icon.xml": "@drawable/pointer_spot_hover",
    "pointer_spot_anchor_vector_icon.xml": "@drawable/pointer_spot_anchor",
}
for xml_name, bitmap_ref in xml_descriptors.items():
    (drawable_dir / xml_name).write_text(f"""<?xml version="1.0" encoding="utf-8"?>
<pointer-icon xmlns:android="http://schemas.android.com/apk/res/android"
    android:bitmap="{bitmap_ref}"
    android:hotSpotX="12dp"
    android:hotSpotY="12dp" />
""")
```

### 4.3 Manifest & Platform Resolution

* **Target SDK**: Bumped `targetSdkVersion` to `35`.
* **Platform Resolver**: Updated `find_android_tools()` to parse platform version numbers numerically (`android-37.0` > `android-36` > `android-34`), ensuring `aapt2 link` always uses the latest SDK symbols.
* **Magisk Packaging**: Updated module packaging to deploy to both `system/product/overlay/` and `system/vendor/overlay/`.

---

## 5. Verification & Diagnosis Commands

To verify that an RRO is active on Android 16 via ADB:

```bash
# 1. Verify that the overlay is enabled
adb shell cmd overlay list --user current | grep allusive

# 2. Inspect idmap resource translation table
adb shell idmap2 dump --idmap-path /data/resource-cache/android-com.afterroot.allusive_rro-allusive_rro.apk@idmap

# 3. Enable touch indicator
adb shell settings put system show_touches 1
```

---

## Conclusion

Android 16 did not break or deprecate Runtime Resource Overlays; rather, the input subsystem transitioned its internal reference style from `@style/Pointer` to `@style/VectorPointer`. 

By adding the new vector resource targets alongside existing bitmap targets in our AAPT2 build script, Pointer Replacer continues to function seamlessly on Android 16 without requiring architectural changes or Gradle dependencies.
