package com.gonzalocamera.padelcounter.mobile

import android.app.Application
import com.gonzalocamera.padelcounter.mobile.data.MobilePreferences
import com.gonzalocamera.padelcounter.mobile.data.MobileRepository
import com.gonzalocamera.padelcounter.mobile.data.db.PadelDatabase

class MobileApp : Application() {
    val database by lazy { PadelDatabase.getInstance(this) }
    val preferences by lazy { MobilePreferences(this) }
    val repository by lazy { MobileRepository(database.matchDao(), preferences) }
}
