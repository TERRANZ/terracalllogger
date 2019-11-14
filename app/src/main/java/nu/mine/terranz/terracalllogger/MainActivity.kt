package nu.mine.terranz.terracalllogger

import android.Manifest.permission.*
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val permissionsRequestCode = 123
    private lateinit var managePermissions: ManagePermissions
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_main)

        val list = listOf(
            READ_CONTACTS,
            READ_CALENDAR,
            WRITE_CALENDAR,
            PROCESS_OUTGOING_CALLS,
            READ_PHONE_STATE,
            READ_CALL_LOG,
            READ_CONTACTS
        )

        // Initialize a new instance of ManagePermissions class
        managePermissions = ManagePermissions(this, list, permissionsRequestCode)

        // Button to check permissions states

        if (SDK_INT >= M)
            managePermissions.checkPermissions()

    }
}
