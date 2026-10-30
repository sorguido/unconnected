package it.sorguido.unconnected

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager

object ScreeningAccess {
    fun roleAvailable(context: Context): Boolean = runCatching {
        context.getSystemService(RoleManager::class.java)?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
    }.getOrDefault(false)
    fun hasRole(context: Context): Boolean = runCatching {
        context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
    }.getOrDefault(false)
    fun hasContacts(context: Context): Boolean = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    fun ready(context: Context): Boolean = hasRole(context) && hasContacts(context)
}
