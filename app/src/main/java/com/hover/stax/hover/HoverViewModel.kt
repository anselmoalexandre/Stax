package com.hover.stax.hover

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.hover.stax.contacts.ContactRepo
import com.hover.stax.contacts.StaxContact

class HoverViewModel(val application: Application, val repo: ContactRepo): ViewModel() {

    fun loadContact(contact_id: String): LiveData<StaxContact> {
        return repo.getLiveContact(contact_id)
    }
}