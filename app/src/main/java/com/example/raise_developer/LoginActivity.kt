package com.example.raise_developer

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.apollographql.apollo3.api.ApolloResponse
import com.example.graphqlsample.queries.GithubCommitQuery
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.oAuthCredential
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class LoginActivity: AppCompatActivity() {

    val provider = OAuthProvider.newBuilder("github.com")

    val auth = Firebase.auth
    //파이어 스토어 함수
    var presentLV=0
    var jsonData = ""
    var level = ""
    var presentMoney = ""
    var tutorialCehck = false

    companion object {
        lateinit var prefs: PreferenceInventory
    }

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
        val user = hashMapOf(
            "uID" to userId,
            "money" to "",
            "level" to "1",
            "jsonString" to ""
        )
        FireStore.db.collection("user").document(userId).set(
            user
        ).await()
        FireStore.tutorialCehck = true
        prefs.prefs.edit().clear().apply()
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
            "money" to presentMoney,
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
        val progressBar = findViewById<ProgressBar>(R.id.login_page_progrss_bar)
        val continueBtn = findViewById<TextView>(R.id.continue_btn)
        progressBar.max = 100
        loginBtn.setOnClickListener{

            auth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener(
                    OnSuccessListener<AuthResult?>() {
                            authResult -> auth.signInWithCredential(authResult.credential!!)
                        .addOnCompleteListener(this@LoginActivity) {task ->
                            if(task.isSuccessful) {
                                loginBtn.visibility = View.INVISIBLE
                                progressBar.visibility = View.VISIBLE
                                val userEmail = Firebase.auth.currentUser?.email
                                val userId = authResult.additionalUserInfo?.username.toString() // 유저의 아이디
                                CoroutineScope(Dispatchers.Main).launch {
                                    //파이어스토어 정보 받아오기
                                    val checkData = checkData(userId)
                                    if (checkData?.data == null) {
                                        setData(userId)
                                    } else {
                                        readData(checkData, prefs.prefs, userId)

                                    }

                                    Log.d("if Login success", userId)
                                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                    intent.putExtra("userEmail", userEmail)
                                    intent.putExtra("userId", userId) // 유저 아이디 전달
                                    intent.putExtra("tutorialCheck",tutorialCehck)
                                    startActivity(intent)
                                    finish()
                                }
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

