package com.benedy.deudas

import android.app.Application
import com.benedy.deudas.data.auth.AuthRepository

class DeudasApp : Application() {
    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        authRepository = AuthRepository(applicationContext)
    }
}
