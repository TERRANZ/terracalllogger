package nu.mine.terranz.terracalllogger

import android.app.Application
import android.content.Context
import nu.mine.terranz.terracalllogger.R.string.error_caught
import org.acra.ACRA
import org.acra.ReportingInteractionMode.SILENT
import org.acra.annotation.ReportsCrashes
import org.acra.sender.HttpSender.Method.POST

@ReportsCrashes(
    formUri = "http://terranz.mine.nu/jbrss/errors/do.error.report/calllogger",
    httpMethod = POST,
    mode = SILENT,
    resToastText = error_caught
)
class CallLoggerApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        ACRA.init(this)
    }
}