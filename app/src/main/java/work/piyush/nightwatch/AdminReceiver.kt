package work.piyush.nightwatch

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

/** Grants only the "lock screen now" policy, used by the sleep guard. */
class AdminReceiver : DeviceAdminReceiver() {
    companion object {
        fun component(ctx: Context) = ComponentName(ctx, AdminReceiver::class.java)

        fun active(ctx: Context): Boolean =
            ctx.getSystemService(DevicePolicyManager::class.java).isAdminActive(component(ctx))

        fun remove(ctx: Context) {
            ctx.getSystemService(DevicePolicyManager::class.java).removeActiveAdmin(component(ctx))
        }
    }
}
