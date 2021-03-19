package nu.mine.terranz.terracalllogger.activity

import android.Manifest.permission.*
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.provider.CalendarContract
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import nu.mine.terranz.terracalllogger.R

class MainActivity : AppCompatActivity() {

    private var list = listOf(
        READ_CONTACTS,
        READ_CALENDAR,
        WRITE_CALENDAR,
        PROCESS_OUTGOING_CALLS,
        READ_PHONE_STATE,
        READ_CALL_LOG
    )
    private val calendarsMap = HashMap<Long, String>()
    private val CAL_ID = "cal_id"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_main)

        if (SDK_INT >= M)
            checkAndRequestPermissions()

        try {
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR
            )

            val cr = contentResolver
            val uri = CalendarContract.Calendars.CONTENT_URI
            val cur = cr.query(uri, projection, null, null, null)
            while (cur!!.moveToNext()) {
                val id = cur.getLong(0)
                val name = cur.getString(1)
                calendarsMap[id] = name
            }
            cur.close()
        } catch (e: SecurityException) {
            Log.i(this.javaClass.name, "Get permissions firstly")
        }
    }

    private fun isPermissionsGranted(): Int {
        var counter = 0;
        for (permission in list) {
            counter += checkSelfPermission(this, permission)
        }
        return counter
    }

    private fun deniedPermission(): String {
        for (permission in list) {
            if (checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_DENIED
            ) return permission
        }
        return ""
    }

    private fun requestPermissions() {
        val permission = deniedPermission()
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
        } else {
            ActivityCompat.requestPermissions(this, list.toTypedArray(), 123)
        }
    }

    fun chooseCal(view: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите календарь")

        val sp = getDefaultSharedPreferences(this)

        val selectedCalendar = sp.getLong(CAL_ID, -1L)

        var checkedItem = 0
        if (selectedCalendar != -1L)
            checkedItem = calendarsMap.keys.indexOf(selectedCalendar)

        builder.setSingleChoiceItems(
            calendarsMap.values.toTypedArray(),
            checkedItem
        ) { dialog, which ->
            Log.i(this.javaClass.name, which.toString())
            sp.edit().putLong(CAL_ID, calendarsMap.keys.elementAt(which)).apply()
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel", null)
        val dialog = builder.create()
        dialog.show()
    }


    private fun checkAndRequestPermissions() {
        val readCal = checkSelfPermission(this, READ_CALENDAR)
        val writeCal = checkSelfPermission(this, WRITE_CALENDAR)
        val processCall = checkSelfPermission(this, PROCESS_OUTGOING_CALLS)
        val readPhoneState = checkSelfPermission(this, READ_PHONE_STATE)
        val readCallLog = checkSelfPermission(this, READ_CALL_LOG)
        val readContacts = checkSelfPermission(this, READ_CONTACTS)
        val internet = checkSelfPermission(this, INTERNET)

        if (readCal != PackageManager.PERMISSION_GRANTED
            || writeCal != PackageManager.PERMISSION_GRANTED
            || processCall != PackageManager.PERMISSION_GRANTED
            || readPhoneState != PackageManager.PERMISSION_GRANTED
            || readCallLog != PackageManager.PERMISSION_GRANTED
            || readContacts != PackageManager.PERMISSION_GRANTED
            || internet != PackageManager.PERMISSION_GRANTED
        ) {
            val perms = arrayOf(
                READ_CALENDAR,
                WRITE_CALENDAR,
                PROCESS_OUTGOING_CALLS,
                READ_PHONE_STATE,
                READ_CALL_LOG,
                READ_CONTACTS,
                INTERNET
            )
            ActivityCompat.requestPermissions(this, perms, 1);
        }
    }

}
