package com.example.amogusapp.realmdb

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class Chat : RealmObject {
    @PrimaryKey var _id: ObjectId = ObjectId()
    var fullContent : String = "null"
}