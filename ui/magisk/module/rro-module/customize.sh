SKIPUNZIP=0

ui_print "****************************************"
ui_print "*        Allusive RRO Overlay          *"
ui_print "****************************************"
ui_print "- Target: Android System Framework (android)"
ui_print "- Installing RRO overlay to /system/vendor/overlay"

# Set permissions
set_perm_recursive "$MODPATH/system" 0 0 0755 0644

ui_print "- Overlay deployed successfully."
ui_print " "
ui_print "TIP: Enable Developer Options -> 'Show taps' to see your new touch pointer!"
ui_print "****************************************"
