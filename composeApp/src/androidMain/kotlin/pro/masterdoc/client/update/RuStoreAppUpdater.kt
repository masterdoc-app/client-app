package pro.masterdoc.client.update

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import ru.rustore.sdk.appupdate.manager.factory.RuStoreAppUpdateManagerFactory
import ru.rustore.sdk.appupdate.model.AppUpdateInfo
import ru.rustore.sdk.appupdate.model.AppUpdateOptions
import ru.rustore.sdk.appupdate.model.AppUpdateType
import ru.rustore.sdk.appupdate.model.InstallState
import ru.rustore.sdk.appupdate.model.InstallStatus
import ru.rustore.sdk.appupdate.model.UpdateAvailability

class RuStoreAppUpdater(private val activity: ComponentActivity) {
    private val manager =
        try {
            RuStoreAppUpdateManagerFactory.create(activity)
        } catch (t: Throwable) {
            Log.i(TAG, "RuStore updates unavailable", t)
            null
        }
    private var checking = false
    private var silentInFlight = false
    private var silentListener: ((InstallState) -> Unit)? = null

    fun checkAndStart() {
        if (checking || manager == null) return
        if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return

        checking = true
        val installed = try {
            activity.packageManager.getPackageInfo(activity.packageName, 0).longVersionCode.toInt()
        } catch (t: Throwable) {
            Log.w(TAG, "Version code read failed", t)
            checking = false
            return
        }

        manager.getAppUpdateInfo()
            .addOnSuccessListener { info ->
                try {
                    if (info.updateAvailability != UpdateAvailability.UPDATE_AVAILABLE) return@addOnSuccessListener
                    val flow = selectUpdateFlow(installed, info.availableVersionCode.toInt()) ?: return@addOnSuccessListener
                    when (flow) {
                        AppUpdateFlow.Immediate -> startImmediate(info)
                        AppUpdateFlow.Silent -> startSilent(info)
                    }
                } finally {
                    checking = false
                }
            }
            .addOnFailureListener { t ->
                Log.i(TAG, "getAppUpdateInfo unavailable", t)
                checking = false
            }
    }

    private fun startImmediate(info: AppUpdateInfo) {
        val options = AppUpdateOptions.Builder().appUpdateType(AppUpdateType.IMMEDIATE).build()
        manager?.startUpdateFlow(info, options)
            ?.addOnSuccessListener { resultCode ->
                if (resultCode == Activity.RESULT_CANCELED || resultCode == ACTIVITY_NOT_FOUND) {
                    activity.finish()
                }
            }
            ?.addOnFailureListener {
                activity.finish()
            }
    }

    private fun startSilent(info: AppUpdateInfo) {
        if (silentInFlight) return
        silentInFlight = true

        if (silentListener == null) {
            silentListener = { state ->
                when (state.installStatus) {
                    InstallStatus.DOWNLOADED -> {
                        val options = AppUpdateOptions.Builder().appUpdateType(AppUpdateType.SILENT).build()
                        manager?.completeUpdate(options)
                        resetSilentFlow()
                    }
                    InstallStatus.FAILED -> {
                        Log.i(TAG, "Silent update download failed")
                        resetSilentFlow()
                    }
                    else -> Unit
                }
            }
            manager?.registerListener(silentListener!!)
        }

        val options = AppUpdateOptions.Builder().appUpdateType(AppUpdateType.SILENT).build()
        manager?.startUpdateFlow(info, options)
            ?.addOnFailureListener { t ->
                Log.i(TAG, "Silent update failed", t)
                resetSilentFlow()
            }
    }

    private fun resetSilentFlow() {
        silentInFlight = false
        silentListener?.let { listener ->
            manager?.unregisterListener(listener)
            silentListener = null
        }
    }

    companion object {
        private const val TAG = "RuStoreAppUpdater"
        private const val ACTIVITY_NOT_FOUND = 2
    }
}
