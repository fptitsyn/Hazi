package com.android.application.hazi.workers

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.application.hazi.R
import com.android.application.hazi.fragments.TabsFragment
import com.android.application.hazi.models.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class TasksNotificationWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context,
    workerParams
) {

    companion object {
        val TAG: String = TasksNotificationWorker::class.java.simpleName
        const val NOTIFICATION_ID = 3
    }

    override suspend fun doWork(): Result {
        val database =
            FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")

        val currentUserId = Firebase.auth.currentUser?.uid

        val userDatabaseReference = database.reference.child("users")
            .orderByChild("id").equalTo(currentUserId).get()
            .await().children.first().ref

        val tasksRef = userDatabaseReference.child("tasks")
        val tasksQuery = tasksRef.orderByChild("date")

        val taskList = mutableListOf<Task>()

        tasksQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (taskSnapshot in snapshot.children) {
                    val task = taskSnapshot.getValue<Task>()

                    if (task != null) {
                        if (!(task.date.isNullOrBlank())) {
                            taskList.add(task)
                        }
                    }
                }

                Log.d(TabsFragment.TAG, taskList.toString())
                if (taskList.isNotEmpty()) {
                    val deepLink = NavDeepLinkBuilder(applicationContext)
                        .setGraph(R.navigation.tabs_graph)
                        .setDestination(R.id.tasks)
                        .createPendingIntent()

                    var bigText = ""

                    for (task in taskList) {
                        bigText += "${task.name}: ${task.date} \n"
                    }

                    Log.d(TabsFragment.TAG, bigText)

                    val builder =
                        NotificationCompat.Builder(applicationContext, TabsFragment.TASKS_CHANNEL_ID)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle(applicationContext.getString(R.string.tasks_notification_title))
                            .setContentText(applicationContext.getString(R.string.tasks_notification_content))
                            .setStyle(
                                NotificationCompat.BigTextStyle()
                                    .bigText(bigText)
                            )
                            .setContentIntent(deepLink)
                            .setAutoCancel(true)

                    with(NotificationManagerCompat.from(applicationContext)) {
                        notify(NOTIFICATION_ID, builder.build())
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, "Database error occurred: $error")
            }
        })

        return Result.success()
    }
}