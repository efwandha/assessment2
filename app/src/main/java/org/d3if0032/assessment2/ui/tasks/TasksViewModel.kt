package org.d3if0032.assessment2.ui.tasks

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.d3if0032.assessment2.data.PreferencesManager
import org.d3if0032.assessment2.data.SortOrder
import org.d3if0032.assessment2.data.Task
import org.d3if0032.assessment2.data.TaskDao


class TasksViewModel @ViewModelInject constructor(
    private val taskDao: TaskDao,
    private val preferencesManager: PreferencesManager,
    @Assisted private val state: SavedStateHandle
    ) : ViewModel() {

        val searchQuery = state.getLiveData("searchQuery","")

        val preferencesFlow = preferencesManager.preferencesFlow

        private val tasksEventChannel = Channel<TasksEvent>()
        val tasksEvent = tasksEventChannel.receiveAsFlow()

        private val tasksFlow = combine(
            searchQuery.asFlow(),
            preferencesFlow
        ){query, filterPreferences ->
            Pair(query, filterPreferences)
        }.flatMapLatest { (query, filterPreferences) ->
            taskDao.getTasks(query, filterPreferences.sortOrder, filterPreferences.hideCompleted)
        }

        val tasks = tasksFlow.asLiveData()

        fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
            preferencesManager.updateSortOrder(sortOrder)
        }

        fun onHideCompletedClick(hideCompleted: Boolean) = viewModelScope.launch {
            preferencesManager.updateHideCompleted((hideCompleted))
        }

        fun onTaskSelected(task: Task) = viewModelScope.launch{
            tasksEventChannel.send(TasksEvent.NavigateToEditTaskScreen(task))
        }

        fun onTaskCheckedChanged(task: Task, isChecked: Boolean) = viewModelScope.launch {
            taskDao.update(task.copy(completed = isChecked))
        }

        fun onTaskSwiped(task: Task) = viewModelScope.launch {
            taskDao.delete(task)
            tasksEventChannel.send(TasksEvent.ShowUndoDeleteTasksMessage(task))
        }

        fun onUndoDeleteClick(task: Task) = viewModelScope.launch {
            taskDao.insert(task)
        }

        fun onAddNewTaskClick() = viewModelScope.launch {
            tasksEventChannel.send(TasksEvent.NavigateToAddTaskScreen)
        }

    sealed class TasksEvent {
        object NavigateToAddTaskScreen : TasksEvent()
        data class  NavigateToEditTaskScreen(val task: Task) : TasksEvent()
        data class ShowUndoDeleteTasksMessage(val task: Task) : TasksEvent()
    }
}
