package com.example.raise_developer

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import com.example.graphqlsample.queries.GithubCommitQuery
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.oAuthCredential
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class LoginActivity: AppCompatActivity() {

    val provider = OAuthProvider.newBuilder("github.com")

    val auth = Firebase.auth
    //파이어 스토어 함수
    val db = FirebaseFirestore.getInstance()
    var jsonData = ""
    var level = "1"
    var presentMoney = ""
    var tutorialCheck = false

    //    깃허브 정보
    var githubContributionData: List<GithubCommitQuery.Week>? = null // 깃허브 정보를 받으려는 변수
    val token = BuildConfig.GITHUB_TOKEN // 나의 깃허브 토큰
    val apolloClient = ApolloClient.builder()
        .addHttpInterceptor(AuthorizationInterceptor("${token}"))
        .serverUrl("https://api.github.com/graphql")
        .build()

    inner class AuthorizationInterceptor(val token: String) : HttpInterceptor { // 인증을 도와주는 클래스
        override suspend fun intercept(
            request: HttpRequest,
            chain: HttpInterceptorChain
        ): HttpResponse {
            return chain.proceed(
                request.newBuilder().addHeader("Authorization", "Bearer $token").build()
            )
        }
    }

    companion object {
        lateinit var prefs: PreferenceInventory
    }

    var myService : MyService? = null
    var isConService = false
    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            Log.d("서비스","실행됨")
            val b = p1 as MyService.LocalBinder
            isConService = true
            myService = b.getService()
            myService?.githubInfoActivityToService(githubContributionData)
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isConService = false
        }
    }
    fun serviceBind() {
        val bindService = Intent(this, MyService::class.java)
        bindService(bindService, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun serviceUnBind() {
        if (isConService) {
            unbindService(serviceConnection)
            isConService = false
        }
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
        db.collection("user").document(userId).set(
            user
        ).await()
        tutorialCheck = true
        prefs.prefs.edit().clear().apply()
    }

    fun readData(data: DocumentSnapshot, prefs: SharedPreferences, userId: String) {
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
            presentMoney = dataSet[1].replace("\\s".toRegex(), "").split("=")[1]
        }
        prefs.edit().putString("inventory", jsonData).apply()
        prefs.edit().putString("money", presentMoney).apply()
        prefs.edit().putString("level", level).apply()
    }

    suspend fun updateData(userId: String){
        val user = hashMapOf(
            "money" to presentMoney,
            "level" to level,
            "jsonString" to jsonData
        )
        FireStore.db.collection("user").document(userId).update(user as Map<String, Any>).await()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)
        prefs = PreferenceInventory(this)
        initEvent()
    }

    fun initEvent(){
//        로그인버튼
        val loginBtn=findViewById<TextView>(R.id.login_btn)
        val progressBar = findViewById<ProgressBar>(R.id.login_page_progrss_bar)
        val githubIcon = findViewById<ImageView>(R.id.login_page_github_icon)
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
                                githubIcon.visibility = View.INVISIBLE
                                progressBar.visibility = View.VISIBLE

                                val userEmail = Firebase.auth.currentUser?.email
                                val userId = authResult.additionalUserInfo?.username.toString() // 유저의 아이디

                                CoroutineScope(Dispatchers.Main).launch {
                                    // 깃허브 정보 받아오기
                                    val githubResponse: Deferred<ApolloResponse<GithubCommitQuery.Data>> =
                                        async {
                                            apolloClient.query(GithubCommitQuery(userId))
                                                .execute()
                                        }
                                    githubContributionData =
                                        githubResponse.await().data?.user?.contributionsCollection?.contributionCalendar?.weeks
                                    Log.d("깃허브 ㄱ끝","2")
                                    progressBar.progress = 30

                                    serviceBind()

                                    val checkData = checkData(userId)
                                    progressBar.progress = 60

                                    if (checkData?.data == null) {
                                        setData(userId)
                                        progressBar.progress = 70
                                    } else {
                                        readData(checkData, prefs.prefs, userId)
                                        progressBar.progress = 90
                                        updateData(userId)
                                    }

                                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                    intent.putExtra("userEmail", userEmail)
                                    intent.putExtra("userId", userId) // 유저 아이디 전달
                                    intent.putExtra("tutorialCheck",tutorialCheck)
                                    intent.putExtra("level",level)
                                    intent.putExtra("presentMoney",presentMoney)

                                    progressBar.progress = 100
                                    progressBar.visibility = View.INVISIBLE

                                    continueBtn.visibility = View.VISIBLE
                                    githubIcon.visibility = View.VISIBLE

                                    continueBtn.setOnClickListener {
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                            }
                            else {
                                Toast.makeText(this,"깃허브 로그인 실패", Toast.LENGTH_LONG).show()}
                        }
                    }
                )
                .addOnFailureListener{ e ->
                    val i = findViewById<TextView>(R.id.mainBot)
                    i.setText("해당 앱의")
                    Toast.makeText(this,"${e}", Toast.LENGTH_LONG).show()
                    loginBtn.setText("오류")
                }
        }
        }

    override fun onDestroy() {
        super.onDestroy()
        serviceUnBind()
        Log.d("onDestroy","g")
    }

}


