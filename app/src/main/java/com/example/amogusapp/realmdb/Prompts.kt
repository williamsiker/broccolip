package com.example.amogusapp.realmdb

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

//Prompts 1 -> Many Chat
class Prompts : RealmObject {
    @PrimaryKey var _id: ObjectId = ObjectId()
    var nameChat : String? = null
    var chatList : RealmList<Chat> = realmListOf()
}