package com.android.application.hazi.workers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.application.hazi.R
import com.android.application.hazi.fragments.ShopItemFragment
import com.android.application.hazi.fragments.TabsFragment
import com.android.application.hazi.utils.MyApplication
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class HungerWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    companion object {
        const val NOTIFICATION_ID = 2
    }

    override suspend fun doWork(): Result {
        val database =
            FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")

        val currentUserId = Firebase.auth.currentUser?.uid

        val userDatabaseReference = database.reference.child("users")
            .orderByChild("id").equalTo(currentUserId).get()
            .await().children.first().ref

        val petRef = userDatabaseReference.child("pet")

        petRef.child("hunger").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var hunger = snapshot.value.toString().toInt()

                if (hunger <= 90) {
                    hunger += 10

                    petRef.child("hunger").setValue(hunger)
                    MyApplication.hunger = hunger
                } else {
                    hunger = 100

                    val deepLink = NavDeepLinkBuilder(applicationContext)
                        .setGraph(R.navigation.tabs_graph)
                        .setDestination(R.id.pet)
                        .createPendingIntent()

                    val builder = NotificationCompat.Builder(applicationContext, TabsFragment.PET_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(applicationContext.getString(R.string.hunger_full_title))
                        .setContentText(applicationContext.getString(R.string.hunger_full_content))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(deepLink)
                        .setAutoCancel(true)

                    with(NotificationManagerCompat.from(applicationContext)) {
                        if (ActivityCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        notify(EnergyWorker.NOTIFICATION_ID, builder.build())
                    }
                }

                Log.d("HungerWorker", hunger.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(ShopItemFragment.TAG, "Database error occurred: $error")
            }
        })

        return Result.success()
    }
}