package com.afterroot.allusive.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.afterroot.allusive.R
import com.afterroot.allusive.adapter.BottomNavigationAdapter
import com.afterroot.allusive.fragment.InstallPointerFragment
import com.afterroot.allusive.fragment.MainFragment
import com.afterroot.allusive.fragment.RepoHolderFragment
import com.afterroot.allusive.fragment.SettingsFragment
import com.afterroot.allusive.utils.DatabaseFields
import com.afterroot.allusive.utils.PermissionChecker
import com.afterroot.allusive.utils.User
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_dashboard.*

class DashboardActivity : AppCompatActivity() {

    lateinit var toolbar: ActionBar
    val TAG = "DashboardActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Light)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        toolbar = supportActionBar!!
        navigationold.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    override fun onStart() {
        super.onStart()

        //Firebase Analytics
        val bundle = Bundle()
        with(bundle) {
            putString("Device_Name", Build.DEVICE)
            putString("Manufacturer", Build.MANUFACTURER)
            putString("AndroidVersion", Build.VERSION.RELEASE)
        }
        FirebaseAnalytics.getInstance(this).logEvent("DeviceInfo", bundle)

        //Initialize Interstitial Ad
        MobileAds.initialize(this, getString(R.string.interstitial_ad_1_id))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions()
        } else {
            Log.d(TAG, "onStart: Loading fragments..")
            loadFragments()

            if (Settings.System.getInt(contentResolver, "show_touches") == 0) {
                Snackbar.make(view_pager, "Show touches disabled. Would you like to enable", Snackbar.LENGTH_INDEFINITE).setAction("ENABLE") {
                    Settings.System.putInt(contentResolver,
                            "show_touches", 1)
                }.show()
            }
        }

        //Add user in db if not available
        val db = FirebaseFirestore.getInstance()
        FirebaseAuth.getInstance().currentUser.let {
            if (it != null) {
                db.collection(DatabaseFields.USERS)
                        .document(it.uid).set(User(it.displayName!!, it.email!!, it.uid))
                Toast.makeText(this, it.displayName, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val manifestPermissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_SETTINGS)

    private fun checkPermissions() {
        Log.d(TAG, "checkPermissions: Checking Permissions..")
        val permissionChecker = PermissionChecker(this)
        if (permissionChecker.lacksPermissions(manifestPermissions)) {
            Log.d(TAG, "checkPermissions: Requesting Permissions..")
            ActivityCompat.requestPermissions(this, manifestPermissions, REQUEST_CODE)
        } else {
            Log.d(TAG, "checkPermissions: Permissions Granted..")
            loadFragments()

            //TODO
            /*when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    when {
                        Settings.System.canWrite(this) ->
                            when {
                                Settings.System.getInt(contentResolver, "show_touches") == 0 ->
                                    indefiniteSnackbar(view_pager, "Show touches disabled. Would you like to enable", "ENABLE", {
                                        Settings.System.putInt(contentResolver,
                                                "show_touches", 1)
                                    }).show()
                            }
                        else -> {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            intent.data = Uri.parse("package:$packageName")
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent)
                        }
                    }
            }*/
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE -> {
                val isPermissionGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (!isPermissionGranted) {
                    Log.d(TAG, "onRequestPermissionsResult: Permissions not Granted..")
                    Snackbar.make(this.container, "Please Grant Permissions", Snackbar.LENGTH_INDEFINITE).setAction("GRANT") {
                        checkPermissions()
                    }
                } else {
                    loadFragments()
                }
            }
        }
    }

    private var viewpagerAdapter: BottomNavigationAdapter? = null
    private fun loadFragments() {
        view_pager.setPagingEnabled(false)
        viewpagerAdapter = BottomNavigationAdapter(supportFragmentManager)
        val mainFragment = MainFragment.newInstance()
        val installPointerFragment = InstallPointerFragment.newInstance()
        val settingsFragment = SettingsFragment()
        val pointersRepoFragment = RepoHolderFragment()


        viewpagerAdapter!!.run {
            addFragment(mainFragment, "Allusive")
            addFragment(pointersRepoFragment, "Browse Pointers")
            addFragment(settingsFragment, "Settings")
        }

        view_pager.adapter = viewpagerAdapter

    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        val title = getString(R.string.app_name)
        when (item.itemId) {
            R.id.navigation_home -> {
                view_pager.currentItem = 0
                toolbar.title = viewpagerAdapter!!.getPageTitle(0).toString()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                view_pager.currentItem = 1
                toolbar.title = viewpagerAdapter!!.getPageTitle(1).toString()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_settings -> {
                view_pager.currentItem = 2
                toolbar.title = viewpagerAdapter!!.getPageTitle(2).toString()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    companion object {
        val REQUEST_CODE = 256

        fun showInstallPointerFragment(activity: FragmentActivity) {
            activity.supportFragmentManager.beginTransaction().replace(R.id.fragment_container, InstallPointerFragment()).commit()
        }
    }
}
