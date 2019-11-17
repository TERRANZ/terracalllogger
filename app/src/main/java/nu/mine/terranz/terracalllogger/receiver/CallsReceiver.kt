package nu.mine.terranz.terracalllogger.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.provider.CalendarContract.Events.*
import android.provider.Contacts
import android.telephony.TelephonyManager.*
import android.util.Log
import nu.mine.terranz.terracalllogger.R
import java.util.*

private const val SDATE = "sdate"
private const val PREVSTATE = "prevstate"
private const val STEPS = "steps"
private const val CAL_ID = "cal_id"
private const val NEW_OUTGOING_CALL = "android.intent.action.NEW_OUTGOING_CALL"
private const val PHONE_NUMBER = "android.intent.extra.PHONE_NUMBER"

class CallsReceiver : BroadcastReceiver() {
    private lateinit var mContext: Context
    private lateinit var mIntent: Intent
    private var number: String = ""

    private fun onCallStateChanged(state: Int) {
        val sp = getDefaultSharedPreferences(mContext)
        if (state == CALL_STATE_IDLE) {
            val startDate = sp.getLong(SDATE, Date().time)
            when (sp.getInt(PREVSTATE, 0)) {
                CALL_STATE_RINGING -> {
                    saveCallEvent(
                        missed = sp.getInt(STEPS, 2) == 2,
                        incoming = true,
                        startDate = startDate,
                        length = Date().time - startDate,
                        number = number
                    )
                }
                CALL_STATE_OFFHOOK -> {
                    saveCallEvent(
                        missed = false,
                        incoming = sp.getInt(STEPS, 2) != 2,
                        startDate = startDate,
                        length = Date().time - startDate,
                        number = number
                    )
                }
                else -> saveCallEvent(
                    missed = true,
                    incoming = true,
                    startDate = startDate,
                    length = Date().time - startDate,
                    number = number
                )
            }
        } else {
            sp.edit().putLong(SDATE, Date().time).apply()
            sp.edit().putInt(PREVSTATE, state).apply()
            sp.edit().putInt(STEPS, sp.getInt(STEPS, 1) + 1).apply()
        }
    }

    private fun saveCallEvent(
        missed: Boolean,
        incoming: Boolean,
        startDate: Long,
        length: Long,
        number: String
    ) {
        val sp = getDefaultSharedPreferences(mContext)

        var descr = if (incoming) {
            mContext.getString(R.string.in_call)
        } else {
            mContext.getString(R.string.out_call)
        }

        descr = if (missed) {
            mContext.getString(R.string.lost_call) + " " + descr
        } else {
            mContext.getString(R.string.accepted_call) + " " + descr
        }

        val cr = mContext.contentResolver
        val values = ContentValues()
        val calendarId = sp.getLong(CAL_ID, 1L)
        val len = length / 1000
        val descFull =
            descr + "вызов, от: " + number + " (" + getContactName(number) + ") длительностью: " + len + " секунд"

        values.put(DTSTART, startDate)
        values.put(DTEND, startDate + length)
        values.put(TITLE, descr + " " + mContext.getString(R.string.call))
        values.put(DESCRIPTION, descFull)
        values.put(CALENDAR_ID, calendarId)
        values.put(EVENT_TIMEZONE, TimeZone.getDefault().id)
        cr.insert(CONTENT_URI, values)

        sp.edit().remove(SDATE).remove(PREVSTATE).remove(STEPS).apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        mContext = context
        mIntent = intent
        if (intent.action == NEW_OUTGOING_CALL) {
            number = intent.extras?.getString(PHONE_NUMBER, "").toString()
        } else {
            val stateStr = intent.extras?.getString(EXTRA_STATE)
            if (number.isEmpty()) {
                number = intent.extras?.getString(EXTRA_INCOMING_NUMBER, "").toString()
            }
            if (!number.isEmpty()) {
                var state = 0
                when (stateStr) {
                    EXTRA_STATE_IDLE -> state = CALL_STATE_IDLE
                    EXTRA_STATE_OFFHOOK -> state = CALL_STATE_OFFHOOK
                    EXTRA_STATE_RINGING -> state = CALL_STATE_RINGING
                }

                Log.i(this.javaClass.name, "Call state changed to: $stateStr")
                onCallStateChanged(state)
            }
        }
    }

    private fun getContactName(phoneNumber: String): String {
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
        val cursor = mContext.contentResolver.query(uri, projection, null, null, null)

        var contactName = ""

        if (cursor!!.moveToFirst()) {
            contactName = cursor.getString(0)
        }
        cursor.close()
        return contactName
    }

}