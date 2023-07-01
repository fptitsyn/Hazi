package com.android.application.hazi.workers

import android.content.Context
import android.util.Log
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

class EnergyWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    companion object {
        const val NOTIFICATION_ID = 1
    }

    override suspend fun doWork(): Result {
        val database =
            FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")

        val currentUserId = Firebase.auth.currentUser?.uid

        val userDatabaseReference = database.reference.child("users")
            .orderByChild("id").equalTo(currentUserId).get()
            .await().children.first().ref

        val petRef = userDatabaseReference.child("pet")

        petRef.child("energy").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var energy = snapshot.value.toString().toInt()

                if (energy <= 90) {
                    energy += 10
                } else {
                    energy = 100
                    // Send notification to user
                    val deepLink = NavDeepLinkBuilder(applicationContext)
                        .setGraph(R.navigation.tabs_graph)
                        .setDestination(R.id.pet)
                        .createPendingIntent()

                    val builder = NotificationCompat.Builder(applicationContext, TabsFragment.PET_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(applicationContext.getString(R.string.energy_full_title))
                        .setContentText(applicationContext.getString(R.string.energy_full_content))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(deepLink)
                        .setAutoCancel(true)

                    with(NotificationManagerCompat.from(applicationContext)) {
                        notify(NOTIFICATION_ID, builder.build())
                    }
                }

                petRef.child("energy").setValue(energy)
                MyApplication.energy = energy

                Log.d("EnergyWorker", energy.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(ShopItemFragment.TAG, "Database error occurred: $error")
            }
        })

        return Result.success()
    }
}