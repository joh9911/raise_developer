package com.example.raise_developer

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.oAuthCredential
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class LoginActivity: AppCompatActivity() {

    val provider = OAuthProvider.newBuilder("github.com")

    val auth = Firebase.auth
    //파이어 스토어 함수
    var userID="test qwe123rqw"
    var money = 1000.toString()
    var presentLV=0
    suspend fun checkData(userId: String): DocumentSnapshot? {
        Log.d("체크데이터", "실행")
        return try {
            val data = FireStore.db.collection("user").document(userId)   // 작업할 컬렉션
                .get()   // 문서 가져오기
                .await()
            Log.d("체크데이터","${data.data}")
            data
        } catch (e: Exception) {
            Log.d("checkdata0","실패")
            null
        }
    }

    suspend fun setData(userId: String) {
        val jsonString = ""
        val level = "1"
        val user = hashMapOf(
            "uID" to userId,
            "money" to money,
            "level" to level,
            "jsonString" to jsonString
        )
        FireStore.db.collection("user").document(userId).set(
            user
        ).await()
        FireStore.tutorialCehck = true
        MainActivity.prefs.prefs.edit().clear().apply()
    }

    fun readData(data: DocumentSnapshot, prefs: SharedPreferences, userId: String) {
        var jsonData = ""
        var level = ""
        var dataSet = data.data.toString().split("uID=")[1].split(",")
        var presentId = dataSet[0]
        var jsonDataSet = data.data.toString().split("uID=")[1].split("jsonString=")
        var jsonChanger = jsonDataSet[1].split("}]}")
        if (presentId == userId) {
            if (jsonChanger[0] != "}") {
                jsonData =
                    jsonChanger[0] + "}]}"
            }
            level = dataSet[2].replace("\\s".toRegex(), "").split("=")[1]
            FireStore.presentMoney = dataSet[1].replace("\\s".toRegex(), "").split("=")[1]
        }
        prefs.edit().putString("inventory", jsonData).apply()
        prefs.edit().putString("money", FireStore.presentMoney).apply()
        prefs.edit().putString("level", level).apply()
        val user = hashMapOf(
            "money" to money,
            "level" to level,
            "jsonString" to jsonData
        )
        FireStore.db.collection("user").document(userId).update(user as Map<String, Any>)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)
        initEvent()
    }

    fun initEvent(){
//        로그인버튼
        val loginBtn=findViewById<TextView>(R.id.login_btn)
        loginBtn.setOnClickListener{

            auth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener(
                    OnSuccessListener<AuthResult?>() {
                            authResult -> auth.signInWithCredential(authResult.credential!!)
                        .addOnCompleteListener(this@LoginActivity) {task ->
                            if(task.isSuccessful) {
                                val user = Firebase.auth.currentUser?.email
                                val userId = authResult.additionalUserInfo?.username.toString() // 유저의 아이디
                                Log.d("if Login success", userId)
                                val intent= Intent(this,MainActivity::class.java)
                                intent.putExtra("userEmail",user)
                                intent.putExtra("userId",userId) // 유저 아이디 전달
                                startActivity(intent)
                                finish()
                            }
                            else {
                                Toast.makeText(this,"깃허브 로그인 실패", Toast.LENGTH_LONG).show()}
                        }
                    }
                )
                .addOnFailureListener(
                    OnFailureListener {
                        Toast.makeText(this,"Error", Toast.LENGTH_LONG).show()
                    }
                )
        }
        }


    }

