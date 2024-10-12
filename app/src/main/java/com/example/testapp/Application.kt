package com.example.testapp

import android.app.Application
import android.content.Context

class ApplicationContext : Application() {
    companion object {
        private lateinit var instance: ApplicationContext
        fun applicationContext(): Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}