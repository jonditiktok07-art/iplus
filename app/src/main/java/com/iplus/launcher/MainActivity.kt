package com.iplus.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {

    data class AppItem(val label: String, val pkg: String, val icon: Drawable, val intent: Intent)

    private val apps = mutableListOf<AppItem>()
    private lateinit var adapter: AppAdapter

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0F172A.toInt())
            setPadding(dp(32), dp(24), dp(32), dp(24))
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        bar.addView(TextView(this).apply {
            text = "i-plus"
            textSize = 30f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        bar.addView(barButton("تعيين كلانشر افتراضي") {
            try { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
            catch (e: Exception) { toast("افتح إعدادات الجهاز > التطبيقات الافتراضية > الشاشة الرئيسية") }
        })
        bar.addView(barButton("تعطيل اللانشر القديم") { showOldLaunchers() })
        root.addView(bar)

        adapter = AppAdapter()
        val grid = GridView(this).apply {
            numColumns = 6
            verticalSpacing = dp(16)
            horizontalSpacing = dp(16)
            selector = ColorDrawable(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            ).apply { topMargin = dp(24) }
            adapter = this@MainActivity.adapter
            setOnItemClickListener { _, _, pos, _ -> launch(apps[pos]) }
            setOnItemLongClickListener { _, _, pos, _ -> appInfo(apps[pos].pkg); true }
        }
        root.addView(grid)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    override fun onBackPressed() { /* نبقى في الشاشة الرئيسية */ }

    private fun loadApps() {
        val pm = packageManager
        val seen = HashSet<String>()
        val list = mutableListOf<AppItem>()
        for (cat in listOf(Intent.CATEGORY_LAUNCHER, "android.intent.category.LEANBACK_LAUNCHER")) {
            val q = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(q, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == packageName || !seen.add(pkg)) continue
                val i = Intent(Intent.ACTION_MAIN).addCategory(cat)
                    .setClassName(pkg, ri.activityInfo.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                list.add(AppItem(ri.loadLabel(pm).toString(), pkg, ri.loadIcon(pm), i))
            }
        }
        apps.clear()
        apps.addAll(list.sortedBy { it.label.lowercase() })
        adapter.notifyDataSetChanged()
    }

    private fun launch(a: AppItem) {
        try { startActivity(a.intent) } catch (e: Exception) { toast("تعذر فتح التطبيق") }
    }

    private fun appInfo(pkg: String) {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
        } catch (e: Exception) { toast("تعذر فتح معلومات التطبيق") }
    }

    // ---- تعطيل اللانشر القديم ----
    private fun showOldLaunchers() {
        val pm = packageManager
        val homes = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0
        ).map { it.activityInfo.packageName }.distinct().filter { it != packageName }

        if (homes.isEmpty()) { toast("ما لقيت لانشر ثاني على الجهاز"); return }

        val labels = homes.map {
            try { pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString() + "\n" + it } catch (e: Exception) { it }
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("اختر اللانشر اللي تبي تعطله")
            .setItems(labels) { _, i -> confirmDisable(homes[i]) }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun confirmDisable(pkg: String) {
        val disableCmd = "adb shell pm disable-user --user 0 $pkg"
        val enableCmd = "adb shell pm enable $pkg"
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("cmd", disableCmd))

        AlertDialog.Builder(this)
            .setTitle("تعطيل: $pkg")
            .setMessage(
                "تنبيه: عيّن هذا اللانشر كافتراضي أول، وبعدين عطّل القديم.\n\n" +
                "التطبيق ما يقدر يعطل تطبيقات النظام بنفسه. تقدر تعطله بأحد طريقين:\n\n" +
                "1) من إعدادات التطبيق (زر أدناه) وتضغط Disable إذا كان متاح.\n\n" +
                "2) من الكمبيوتر عبر ADB (الأمر منسوخ):\n$disableCmd\n\n" +
                "للتراجع:\n$enableCmd"
            )
            .setPositiveButton("فتح إعدادات التطبيق") { _, _ -> appInfo(pkg) }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    // ---- واجهة ----
    private fun barButton(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isFocusable = true
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(12) }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    inner class AppAdapter : BaseAdapter() {
        override fun getCount() = apps.size
        override fun getItem(p: Int) = apps[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, convert: View?, parent: ViewGroup?): View {
            val a = apps[p]
            val cell = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(12), dp(8), dp(12))
                layoutParams = AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(130)
                )
                // GridView ياخذ الفوكس بنفسه، فالخلية تعتمد على حالة الاختيار
                background = StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_selected), rounded(0xFF2563EB.toInt()))
                    addState(intArrayOf(android.R.attr.state_pressed), rounded(0xFF1D4ED8.toInt()))
                    addState(intArrayOf(), rounded(0x00000000))
                }
            }
            cell.addView(ImageView(this@MainActivity).apply {
                setImageDrawable(a.icon)
                layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
            })
            cell.addView(TextView(this@MainActivity).apply {
                text = a.label
                setTextColor(Color.WHITE)
                textSize = 14f
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            return cell
        }

        private fun rounded(color: Int) = GradientDrawable().apply {
            setColor(color); cornerRadius = dp(12).toFloat()
        }
    }
}
