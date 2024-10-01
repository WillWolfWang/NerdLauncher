package com.will.nerdlauncher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NerdLauncherActivity :AppCompatActivity() {

    private lateinit var recycleView: RecyclerView
    private lateinit var progressBar: ProgressBar

    val launcherForPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {result: Boolean ->
        if (result) {

        }
    }
    //      launcherForPermission.launch(Manifest.permission.QUERY_ALL_PACKAGES)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_nerd_launcher)
        recycleView = findViewById<RecyclerView?>(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)

        recycleView.apply {
            recycleView.layoutManager = LinearLayoutManager(this@NerdLauncherActivity)
        }

        setupAdapter()

//        val list = getLauncherActivities(this)
//        Log.e("WillWolf", "list: ${list.size}")
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // 设置适配器
    private fun setupAdapter() {
        // Main / launcher 的 intent 过滤器可能无法与通过 startActivity(Intent) 函数
        // 发送的 main/launcher 隐式 intent 相匹配
        // startActivity(Intent) 函数意味着，启动匹配隐式 intent 的默认 activity
        // 操作系统会悄悄的为目标 intent 添加 Intent.CATEGORY_DEFAULT 类别
        val startupIntent = Intent().apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        lifecycleScope.launch {
            var activities: List<ResolveInfo>? = null
            progressBar.visibility = View.VISIBLE
            launch((Dispatchers.IO)) {
                // 0 参数，表示不打算修改查询结果
                activities = packageManager.queryIntentActivities(startupIntent, 0)
                Log.e("WillWolf", "activity: ${activities?.size}")

                val collator = Collator.getInstance(Locale.CHINA)
                // ResolveInfo 中可以获取 activity 标签和其他一些元数据
                // sortedWith 会返回一个新的 List
                activities = activities?.sortedWith(object : Comparator<ResolveInfo> {

                    override fun compare(o1: ResolveInfo, o2: ResolveInfo): Int {

                        return collator.compare(o1.loadLabel(packageManager).toString(),
                            o2.loadLabel(packageManager).toString())
                    }
                })


            }
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                val activityAdapter = activities?.let { ActivityAdapter(it) }
                recycleView.adapter = activityAdapter
            }
        }
    }

    // 为 recycleView 写一个 ViewHolder
    private class ActivityHolder(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private var tvLabel: TextView
        private var iv: ImageView
        private lateinit var resolveInfo: ResolveInfo

        init {
            itemView.setOnClickListener(this)
            tvLabel = itemView.findViewById(R.id.tv_label)
            iv = itemView.findViewById(R.id.iv_icon)
        }

        fun bindActivity(resolveInfo: ResolveInfo) {
            this.resolveInfo = resolveInfo
            val packageManager = itemView.context.packageManager
            // 获取应用的名称
            val appName = resolveInfo.loadLabel(packageManager)
            tvLabel.text = appName
            val drawable = resolveInfo.loadIcon(packageManager)
            iv.setImageDrawable(drawable)
        }

        //  按钮点击事件
        override fun onClick(v: View) {
            val activityInfo = resolveInfo.activityInfo
            // 传入 Intent.ACTION_MAIN ，避免有些应用的启动行为存在不同
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName(activityInfo.packageName, activityInfo.name)
                // new Task 让应用属于一个独立的 task，并且如果应用已经运行，再次启动
                // 也不会创建新的 task
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val context = v.context
            context.startActivity(intent)
        }
    }
    // recycleView 的 adapter
    private class ActivityAdapter(val activities: List<ResolveInfo>): RecyclerView.Adapter<ActivityHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nerd_launcher, parent, false)
            return ActivityHolder(view)
        }

        override fun getItemCount(): Int {
            return activities.size
        }

        override fun onBindViewHolder(holder: ActivityHolder, position: Int) {
            holder.bindActivity(activities.get(position))
        }

    }

    fun getLauncherActivities(context: Context): List<LauncherActivityInfo> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        return launcherApps.getActivityList(null, android.os.Process.myUserHandle())
    }
}