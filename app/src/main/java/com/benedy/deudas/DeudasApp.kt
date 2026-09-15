package com.benedy.deudas

import android.app.Application
import android.util.Log
import com.benedy.deudas.data.auth.AuthRepository
import com.benedy.deudas.data.local.DeudasDatabase
import com.benedy.deudas.data.repository.DebtCrmRepository

class DeudasApp : Application() {
    lateinit var authRepository: AuthRepository
        private set
    lateinit var crmRepository: DebtCrmRepository
        private set

    override fun onCreate() {
        super.onCreate()
        authRepository = AuthRepository(applicationContext)
        val db = DeudasDatabase.getInstance(applicationContext)
        // Force open so migration/schema errors surface at startup with a clear log
        // instead of a mysterious crash on the first CRM screen.
        try {
            db.openHelper.writableDatabase
            Log.i(TAG, "Room DB open OK (schema v${db.openHelper.readableDatabase.version})")
        } catch (e: Exception) {
            Log.e(TAG, "Room DB open FAILED — schema/migration mismatch? Data was NOT wiped.", e)
            // Still assign repo; screens that touch DB will fail until schema is fixed.
            // Never call fallbackToDestructiveMigration.
        }
        crmRepository = DebtCrmRepository(db)
    }

    companion object {
        private const val TAG = "DeudasApp"
    }
}
