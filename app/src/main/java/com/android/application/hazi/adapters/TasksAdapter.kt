package com.android.application.hazi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.android.application.hazi.models.Task
import com.android.application.hazi.R
import com.android.application.hazi.databinding.TaskItemBinding

interface TaskActionListener {
    fun onTaskClick(task: Task)

    fun onTaskCompleted(task: Task, checkBox: CheckBox)
}

class TasksAdapter(
    private val tasks: MutableList<Task>,
    private val taskActionListener: TaskActionListener
) :
    RecyclerView.Adapter<TasksAdapter.TaskViewHolder>(), View.OnClickListener {

    override fun onClick(v: View) {
        val task = v.tag as Task

        when (v.id) {
            R.id.taskCompletedCheckBox -> {
                taskActionListener.onTaskCompleted(task, v as CheckBox)
            }
            else -> {
                taskActionListener.onTaskClick(task)
            }
        }
    }

    class TaskViewHolder (
        val binding: TaskItemBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = TaskItemBinding.inflate(inflater, parent, false)

        binding.root.setOnClickListener(this)
        binding.taskCompletedCheckBox.setOnClickListener(this)

        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        with(holder.binding) {
            holder.itemView.tag = task
            taskCompletedCheckBox.tag = task

            taskNameTextView.text = task.name
            taskDescriptionTextView.text = task.description
            taskDifficultyTextView.text = when (task.difficulty) {
                1 -> "Easy"
                2 -> "Medium"
                3 -> "Hard"
                else -> "Easy"
            }
            taskCompletedCheckBox.isChecked = false
        }
    }

    override fun getItemCount(): Int {
        return tasks.size
    }
}