package com.example.amogusapp.realmdb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.realmListOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RealmViewModel : ViewModel() {
    private val realm = BroccoDataBase.realm
    val prompts = realm.query<Prompts>().asFlow().map { results -> results.list.toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private fun createSample() {
        viewModelScope.launch {
            realm.write {
                var prompts1 = Prompts().apply {
                    nameChat = "OCR animatable for modeling with blender"
                }
                var prompts2 = Prompts().apply {
                    nameChat = "New FTP transaction"
                }

                var chat1 = Chat().apply {
                    fullContent = "quia et suscipit\nsuscipit recusandae consequuntur expedita " +
                            "et cum\nreprehenderit molestiae ut ut quas totam\nnostrum rerum " +
                            "est autem sunt rem eveniet architecto"
                }
                var chat2 = Chat().apply {
                    fullContent = "quia et suscipit\nsuscipit recusandae consequuntur expedita " +
                            "et cum\nreprehenderit molestiae ut ut quas totam\nnostrum rerum " +
                            "est autem sunt rem eveniet architecto"
                }
                var chat3 = Chat().apply {
                    fullContent = "quia et suscipit\nsuscipit recusandae consequuntur expedita " +
                            "et cum\nreprehenderit molestiae ut ut quas totam\nnostrum rerum " +
                            "est autem sunt rem eveniet architecto"
                }
                var chat4 = Chat().apply {
                    fullContent = "quia et suscipit\nsuscipit recusandae consequuntur expedita " +
                            "et cum\nreprehenderit molestiae ut ut quas totam\nnostrum rerum " +
                            "est autem sunt rem eveniet architecto"
                }

                prompts1.chatList = realmListOf(chat1, chat3)
                prompts2.chatList = realmListOf(chat2, chat4)

                copyToRealm(prompts1, updatePolicy = UpdatePolicy.ALL)
                copyToRealm(prompts2, updatePolicy = UpdatePolicy.ALL)

                copyToRealm(chat1, updatePolicy = UpdatePolicy.ALL)
                copyToRealm(chat2, updatePolicy = UpdatePolicy.ALL)
                copyToRealm(chat3, updatePolicy = UpdatePolicy.ALL)
                copyToRealm(chat4, updatePolicy = UpdatePolicy.ALL)
            }
        }
    }
}