package com.example.studybuddy.vistas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studybuddy.presentacion.StudyBuddyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private val headerDateFormatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(viewModel: StudyBuddyViewModel) {

    val tasks by viewModel.tasks.collectAsState()

    val tasksByDate = remember(tasks) {
        tasks

            .filter { it.dueDate != null }

            .groupBy { it.dueDate!!.toLocalDateString() }

            .toSortedMap()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (tasksByDate.isEmpty()) {
            item {
                Text(
                    text = "No tienes tareas con fechas asignadas.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {

            tasksByDate.forEach { (dateString, tasksOnDate) ->


                stickyHeader {
                    DateHeader(dateString = dateString)
                }


                items(tasksOnDate) { task ->

                    TaskItem(task = task, onTaskAction = { _, _ ->

                    })
                }
            }
        }
    }
}


@Composable
fun DateHeader(dateString: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = dateString,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}


private fun Date.toLocalDateString(): String {

    val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
    return formatter.format(this)
}