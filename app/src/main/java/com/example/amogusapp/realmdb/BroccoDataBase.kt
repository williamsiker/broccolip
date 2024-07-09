package com.example.amogusapp.realmdb

import android.app.Application
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

class BroccoDataBase  : Application() {
    companion object {
        lateinit var realm : Realm
    }
    override fun onCreate() {
        super.onCreate()
        realm = Realm.open(
            configuration = RealmConfiguration.create(
                schema = setOf(
                    Prompts::class,
                    Chat::class
                )
            )
        )
    }
}