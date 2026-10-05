# R8 rules of the app (1.6.0). The default proguard-android-optimize.txt and the libraries' consumer rules (Room, Glide,
# media3, OkHttp, WorkManager, coroutines) come first; only what is found by name or reflection is listed here.
# Resource shrinking is off on purpose (build.gradle): MIDI and drawables are looked up with getIdentifier.

# Stack traces: ReleaseTree tags use the file name and line number (TimberLogImpl.createStackElementTag), and crash
# reports are retraced with dist/vX.Y.Z/mapping.txt, which tools/release.sh keeps for every release.
-keepattributes SourceFile,LineNumberTable

# DialogActivity instantiates its content fragment by class name (Class.forName + no-argument constructor):
# CustomDialogWv (update dialog) and MediaRecordDeleteFragment (media config). The FragmentManager also re-creates every
# fragment by name after process death.
-keep class * extends androidx.fragment.app.Fragment { public <init>(); }

# WorkManager stores the worker's class name with each scheduled work: a renamed class would break works scheduled by
# an earlier version after an update (UpdateScheduler).
-keep class org.cog.hymnchtv.service.androidupdate.UpdateCheckWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# net.duguying.pinyin reads chars.csv / words.csv next to its class (Class.getResource): package and names must stay.
-keep class net.duguying.pinyin.** { *; }

# The YouTube player page calls these methods by name through addJavascriptInterface.
-keepclassmembers class com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayerBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# ---- Missing-class warnings reviewed in Task A7 Step 6 (each with the reason) go below this line ----
