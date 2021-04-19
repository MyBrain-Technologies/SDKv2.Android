package mbtsdk.com.mybraintech.sdkv2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher_2)

        //define the activity here
//        val activity = TestActivity::class.java
//        val activity = HomeActivity::class.java
        val activity = MelomindActivity::class.java

        Intent(this, activity)
                .also {
                    startActivity(it)
                }
    }
}