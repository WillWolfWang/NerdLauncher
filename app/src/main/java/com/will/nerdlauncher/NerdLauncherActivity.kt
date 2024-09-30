package com.will.nerdlauncher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NerdLauncherActivity :AppCompatActivity() {

    private lateinit var recycleView: RecyclerView

    val launcherForPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {result: Boolean ->

        if (result) {

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_nerd_launcher)


        recycleView = findViewById<RecyclerView?>(R.id.recyclerView)

        recycleView.apply {
            recycleView.layoutManager = LinearLayoutManager(this@NerdLauncherActivity)
        }

//      launcherForPermission.launch(Manifest.permission.QUERY_ALL_PACKAGES)
        setupAdapter()

        val list = getLauncherActivities(this)
        Log.e("WillWolf", "list: ${list.size}")

        isStart = true

//        val myThread = MyThread()
//        myThread.start()

        scope.launch {
            while (isStart) {
                Log.e("WillWolf", "scope start-->" + Thread.currentThread().name)
                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isStart = false
    }

    @Volatile
    var isStart = false


    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)


    inner class MyThread : Thread() {
        override fun run() {
            super.run()
            while (isStart) {
                Log.e("WillWolf", "thread start-->")
                sleep(1000)
            }
        }
    }

    // 设置适配器
    private fun setupAdapter() {
        val startupIntent = Intent(Intent.ACTION_MAIN).apply {
//            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        // 0 参数，表示不打算修改查询结果
        val activities: List<ResolveInfo> = packageManager.queryIntentActivities(startupIntent, 0)
        Log.e("WillWolf", "activity: ${activities.size}")
    }

    fun getLauncherActivities(context: Context): List<LauncherActivityInfo> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        return launcherApps.getActivityList(null, android.os.Process.myUserHandle())
    }
}