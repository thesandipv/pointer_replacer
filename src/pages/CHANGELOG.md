# Changelog

## v1.9.x

### v1.9.4 (05/05/2021)

- Initial Support for Making *Magisk* Modules.

#### How to create and apply the magisk module?

```
Click apply and choose magisk.
Wait for tasks to complete.
Note to the path of the saved magisk module.
Open magisk manager and apply module from generated zip.
```

### v1.9.3 (21/02/2021)

- CI Build Changes

### v1.9.2 (14/12/2020)

- Fix Crashes

### v1.9.1 (12/12/2021)

- N/A

### v1.9.0 (11/12/2020)

- Fast Scroll in Pointers Repository
- More Detail in Pointer Info Screen
- Pointers Download directory is now private. Which fixes download error on Android 10.
- Import Pointer feature removed. Instead, Upload pointer on repo and use.
- Some Design Changes.
- `Repo:` New Filter that filters out Pointers uploaded by only you.
- `Repo:` New animations while Sorting & Filtering.
- `Repo:` You can now edit the Name and Description of the Pointer uploaded by you. (Long Press on Pointer)

---

## v1.8.x

### v1.8.9 (12/07/2020)

- Pointer upload model changed.
- Under the hood changes

### v1.8.8 (03/03/2020)

- not released

### v1.8.7 (10/12/2019)

- Sort by Date and Downloads
- Fast scroll in Pointer Repo
- List Animations bug fix
- `New:` donation option

### v1.8.6 (19/11/2019)

- N/A

### v1.8.5 (09/10/2019)

- `New:` Dark Mode (Set via App Settings > App Theme) (Default is System Theme)
- Minor Fixes

### v1.8.4 (25/09/2019)

- `Fix` crash on custom launchers

### v1.8.3 (23/09/2019)

- `Fix` issue #3 https://github.com/sandipv22/pointer_replacer/issues/3
- Internal Code Changes
- pointer image will be center cropped

### v1.8.2 (07/09/2019)

- Pointer Picker is now a detailed list. [1st time you should click 'Import Old Pointers']  
More detail in Pointer Repo List
- Transparent grid behind pointers for better Handling of Transparent Pointers
- Description is now optional while uploading new pointer

### v1.8.1 (09/08/2019)

- `Fixed` Crashes

### v1.8.0 (27/07/2019)

Major Update after release. It has been almost 2 years.

##### Changes include

- `New:` Brand new Pointer Repository - Download and Install pointer by others or you can now share your pointer with the world.
- `New:` Can change mouse pointer also. (Report me if not working)
- `New:` Complete rewrite of User Interface.
- `New:` Toggle 'Show Touches' from Settings (Extension Needed)
- Many under-the-hood changes

---

## v1.7.x

### v1.7.0 (16/09/2017)

- Released on Play Store.
- `Improvement:` Minor Improvements.

---

## v1.6.x

### v1.6.4 (27/07/2016)

- `New:` Pointer transparency settings (Disabled by default. Enable from Settings)
- `New:` Now you can set max pointer padding from Settings. Max limit is 999.
- `New:` Pokemon Pointers (Go to Install/Manage Pointers > Click on Pokemon Pointers to download Pointer Pack).
- `Improvement:` Improved app experience when too many pointers are installed.
- Other many changes and improvements.

### v1.6.3 (31/05/2016)

- `New:` Improvements for Marshmallow.
- `New:` 4 New Pointers.
- `New:` Added long press to delete pointer.
- `Improvement:` Other minor improvements and bug fixes.

### v1.6.2 (12/03/2016)

- `New:` You can see Pointer Preview before applying pointer.
- `New:` You can check for update from Settings
- `New:` Now you can set max pointer size from Settings. Max limit is 999.
- `New:` DayNight theme from AppCompat Support Library. (You can enable it from Settings)

### v1.6.1 (06/03/2016)

- `Fixed:` Pointer is not changing on Jellybean and KitKat

### v1.6.0 (06/03/2016)

- `New` Card UI
- `New` Color Picker from Material-Dialogs Library
- You can change Card Background Color from settings
- Max size for pointer is 300\*300

---

## v1.5.x

### v1.5.7 (25/12/2015)

- `Added` Reset Color option.
- Minimum Pointer Size is based on device dpi. If your device is hdpi then minimum pointer size will 49\*49 and if your device is xhdpi then minimum pointer size will 66\*66.
- `Added` Padding Slider. After minimum pointer size use padding to decrease pointer scale.
- `Fixed` Current pointer text looking half when pointer is set to minimum size.
- Some UI Changes.

### v1.5.6 (11/12/2015)

- `New` Christmas Pointers.
- `New` 3 Heart Pointers.
- Some Bug Fixes.

### v1.5.5B (25/11/2015)

- New Icon
- `Added` Live Grid Preview & Semi-Live Grid Preview.
- `Added` Reset Size Function.
- `Added` Reboot/Soft-reboot Prompt after confirming pointer.
- `Added` 1 new pointer posted by blue_hmay@naver.com
- Layout Improvement.?

### v1.5.4 (29/10/2015)

- Now with Android Change Log!
- Minor Layout improvement.
- `Added` Pointer resizing function.
- `Added` 55 new pointers posted by @Hermankzr.
- Now color picker remember picked color.

### v1.5.3 (26/10/2015)

- Now pointer size is based on device dpi. [experimental]
- Added G+ Community Link

### v1.5.2 (25/10/2015)

- `Fixed:` Reduced pointer size.
- `Added:` Transparent Customize Activity Background.
- `Added:` Now Customize activity can load all available pointers from `[path-to-sdcard]/Pointers/`

### v1.5.1 b12 (21/10/2015)

- Changed Package Name
- `Added` Load from SDCard option to load pointer image from `[path-to-sdcard]/Pointers/pointer.png`
- `Added` `WRITE_EXTERNAL_STORAGE` back for loading pointer from sdcard.
- `Added` 4 new different pointers. [Rectangle, GPS, Arrow, Default]
- Reduced Size of pointer.

---

## v1.4.x

### v1.4 b11 (02/10/2015)

- Replaced RGB color picker with Color Picker Dialog for better color selection.
- Removed `WRITE_EXTERNAL_STORAGE` permission.

---

## v1.3.x

### v1.3 b10 (01/10/2015)

- `Added` RGB color picker that changes color of Pointer. Cheers!!

### v1.3 b8

- `Removed` support for ICS. Sorry.

---

## v1.2.x

### v1.2 b5

- Enabled Action Bar.
- Now Action Bar is Purple.

### v1.2

- `Added` Purple Concentric Circle Type Pointer [Removed Old Pointer]
- Now Action Bar is Hidden in Main Activity.

---

## v1.1

- `Added` UI of App
- Simple Material Design
- Purple Status Bar & Navigation Bar [Only Lollipop and above]

---

## v1.0

- Initial Release
