package com.benedy.deudas

import android.app.Application
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
        crmRepository = DebtCrmRepository(db)
    }
}
