package com.example.raise_developer

import android.app.Application
import android.content.Context
import android.util.Log

// 홈버튼 눌러서 onStop 상태가 된 건지, 다른 액티비티가 실행되어 onStop 상태가 된 건지 구별하기 위한 공용 클래스
class OnStopChecker : Application() {
    var count = 0

    init{
        instance = this
    }

    companion object {
        var instance: OnStopChecker? = null

    }
// count가 0이면 home 버튼 누른거임
    fun applicationStatusChecker(): Boolean{
        Log.d("applStatusChecker","${count}")
        return count == 0
    }

    fun activityStarted(){
        count  += 1
        Log.d("activityStarted","${count}")
    }

    fun activityStopped(){
        count -= 1
        Log.d("activityStopped","${count}")

    }
}