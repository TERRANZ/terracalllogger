package nu.mine.terranz.terracalllogger.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.CalendarContract.Events
import android.provider.Contacts
import android.telephony.TelephonyManager
import android.util.Log
import java.util.*


class CallsReceiver : BroadcastReceiver() {
    private lateinit var mContext: Context
    private lateinit var mIntent: Intent
    private var prevState: Int = 0
    private var startDate: Long = 0L
    private var number: String = ""
    private var lastState = TelephonyManager.CALL_STATE_IDLE

    private fun onCallStateChanged(state: Int) {
        var callState = "UNKNOWN"
        when (state) {
            TelephonyManager.CALL_STATE_IDLE -> {
                callState = "IDLE"
                startDate = PreferenceManager.getDefaultSharedPreferences(mContext).getLong("sdate", Date().time)
                if (prevState == TelephonyManager.CALL_STATE_RINGING) {
                    saveCallEvent(true, startDate, Date().time - startDate, number)
                } else if (prevState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    saveCallEvent(false, startDate, Date().time - startDate, number)
                } else {
                    saveCallEvent(false, startDate, Date().time - startDate, number)
                }
            }
            TelephonyManager.CALL_STATE_RINGING -> {
                callState = "RINGING"
                prevState = state
                startDate = Date().time
                PreferenceManager.getDefaultSharedPreferences(mContext).edit().putLong("sdate", startDate).apply()
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                callState = "OFFHOOK"
                prevState = state
                startDate = Date().time
                PreferenceManager.getDefaultSharedPreferences(mContext).edit().putLong("sdate", startDate).apply()
            }
        }
        Log.i(">>>Broadcast", "onCallStateChanged $callState")
        lastState = state
    }

    private fun saveCallEvent(incoming: Boolean, startDate: Long, length: Long, number: String) {
        val descr = if (incoming) {
            "Входящий"
        } else {
            "Исходящий"
        }
        val cr = mContext.contentResolver
        val values = ContentValues()

        values.put(Events.DTSTART, startDate)
        values.put(Events.DTEND, startDate + length)
        values.put(Events.TITLE, "Звонок")
        values.put(Events.DESCRIPTION, "$descr вызов, номер: $number " + getContactName(number))
        values.put(Events.CALENDAR_ID, 1)
        values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        val uri = cr.insert(Events.CONTENT_URI, values)
    }

    override fun onReceive(context: Context, intent: Intent) {
        mContext = context
        mIntent = intent
        if (intent.action.equals("android.intent.action.NEW_OUTGOING_CALL")) {
            number = intent.extras.getString("android.intent.extra.PHONE_NUMBER", "")
        } else {
            val stateStr = intent.extras?.getString(TelephonyManager.EXTRA_STATE)
            if (number.isEmpty()) {
                number = intent.extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER, "")
            }
            if (!number.isEmpty()) {
                var state = 0
                if (stateStr == TelephonyManager.EXTRA_STATE_IDLE) {
                    state = TelephonyManager.CALL_STATE_IDLE
                } else if (stateStr == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                    state = TelephonyManager.CALL_STATE_OFFHOOK
                } else if (stateStr == TelephonyManager.EXTRA_STATE_RINGING) {
                    state = TelephonyManager.CALL_STATE_RINGING
                }

                onCallStateChanged(state)
            }
        }
    }

    fun getContactName(phoneNumber: String): String {
        val uri: Uri
        var projection: Array<String>
        var mBaseUri = Contacts.Phones.CONTENT_FILTER_URL
        projection = arrayOf(Contacts.People.NAME)
        try {
            val c = Class
                .forName("android.provider.ContactsContract\$PhoneLookup")
            mBaseUri = c.getField("CONTENT_FILTER_URI").get(mBaseUri) as Uri
            projection = arrayOf("display_name")
        } catch (e: Exception) {
        }

        uri = Uri.withAppendedPath(mBaseUri, Uri.encode(phoneNumber))
        var cursor = mContext.contentResolver.query(uri, projection, null, null, null)

        var contactName = ""

        if (cursor!!.moveToFirst()) {
            contactName = cursor.getString(0)
        }
        cursor.close()
        return contactName
    }

}