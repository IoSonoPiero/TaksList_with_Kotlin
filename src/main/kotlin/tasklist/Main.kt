package tasklist

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.datetime.*
import java.io.File

const val SPACE = " "

var taskList: MutableList<Task> = mutableListOf()

fun main() {
    manageJSON("read")
    printMenu()
}

fun printMenu() {
    while (true) {
        println("Input an action (add, print, edit, delete, end):")
        when (readLine()) {
            "add" -> addToTaskList()
            "print" -> printTaskList()
            "edit" -> editDeleteTask("edit")
            "delete" -> editDeleteTask("delete")
            "end" -> {
                println("Tasklist exiting!")
                manageJSON("write")
                return
            }

            else -> println("The input action is invalid")
        }
    }
}

fun manageJSON(action: String) {
    // Build Pattern
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Pass a parameterized object
    val type = Types.newParameterizedType(MutableList::class.java, Task::class.java)

    // Create an Adapter
    val taskListAdapter = moshi.adapter<MutableList<Task>>(type)

    // create a resource File
    val jsonFile = File("tasklist.json")

    when (action) {
        "read" -> {
            // read from file (if exists) and create taskList
            if (jsonFile.exists()) {
                val jsonStr = jsonFile.readText()
                taskList = taskListAdapter.fromJson(jsonStr)!!
            }
        }

        "write" -> {
            // write to JSON file
            val jsonText = taskListAdapter.toJson(taskList).toString()
            jsonFile.writeText(jsonText)
        }
    }
}

fun editDeleteTask(action: String) {
    printTaskList()

    val maxTaskNumber = taskList.size

    // return if no tasks
    if (maxTaskNumber == 0) return

    var taskToEditDelete: Int

    // get the task number to edit
    while (true) {
        println("Input the task number (1-$maxTaskNumber):")
        try {
            taskToEditDelete = readln().toInt()
        } catch (e: NumberFormatException) {
            println("Invalid task number")
            continue
        }

        // edit ore delete the specified task
        if (taskToEditDelete in 1..maxTaskNumber) {
            when (action) {
                "edit" -> {
                    editTask(taskList[taskToEditDelete - 1])
                    println("The task is changed")
                }

                "delete" -> {
                    taskList.removeAt(taskToEditDelete - 1)
                    println("The task is deleted")
                }
            }
            break
        } else {
            println("Invalid task number")
        }
    }
}

fun editTask(aTask: Task) {
    while (true) {
        println("Input a field to edit (priority, date, time, task):")
        when (readLine()) {
            "priority" -> {
                aTask.priority = managePriority()
                break
            }

            "date" -> {
                aTask.date = manageDate()
                break
            }

            "time" -> {
                aTask.time = manageTime()
                break
            }

            "task" -> {
                aTask.subTask = manageTask(aTask, overwrite = true)
                break
            }

            else -> println("Invalid field")
        }
    }
}

fun printTaskList() {

    // prevent printing empty tasks
    if (taskList.size == 0) {
        println("No tasks have been input")
        return
    }

    printTableHeader()
    printRowSeparator()

    // fill completely only the first task
    var isFirstRowInTask = true

    // declare values that will fill the table
    var counter: String
    var theDate: String
    var theTime: String
    var priority: String
    var dueTag: String

    // iterate the tasks
    for (outerIndex in 0 until taskList.size) {

        // print the subtasks
        for (innerIndex in 0 until (taskList[outerIndex].subTask.size)) {

            if (isFirstRowInTask) {
                // get all values
                counter = formatTextInCell(4, 1, text = (outerIndex + 1).toString())
                theDate = formatTextInCell(12, 1, taskList[outerIndex].date)
                theTime = formatTextInCell(7, 1, taskList[outerIndex].time)
                priority = " ${taskList[outerIndex].priority.color} "
                dueTag = " ${getTaskOverdue(taskList[outerIndex].date)} "

                isFirstRowInTask = false
            } else {
                // fill with dummy data
                counter = formatTextInCell(4, 1, SPACE.repeat(1))
                theDate = formatTextInCell(12, 1, SPACE.repeat(10))
                theTime = formatTextInCell(7, 1, SPACE.repeat(5))
                priority = formatTextInCell(3, 1, SPACE.repeat(1))
                dueTag = formatTextInCell(3, 1, SPACE.repeat(1))
            }
            val fullTaskText = taskList[outerIndex].subTask[innerIndex]

            val chunkedText = fullTaskText.chunked(44)

            if (chunkedText.size == 1) {
                val textPlaceHolder = formatTextInCell(44, 0, fullTaskText)
                println("|$counter|$theDate|$theTime|$priority|$dueTag|$textPlaceHolder|")
            } else {
                for (index in chunkedText.indices) {
                    val textPlaceHolder = formatTextInCell(44, 0, chunkedText[index])
                    if (index == 0) {
                        println("|$counter|$theDate|$theTime|$priority|$dueTag|$textPlaceHolder|")
                    } else {
                        println("|    |            |       |   |   |$textPlaceHolder|")
                    }
                }
            }
        }
        // separate each task by an empty line
        printRowSeparator()
        isFirstRowInTask = true
    }
}

fun formatTextInCell(cellSize: Int, initialSpaces: Int, text: String?): String {
    val fillerSpaces = cellSize - initialSpaces - (text?.length ?: 0)
    return "${SPACE.repeat(initialSpaces)}$text${SPACE.repeat(fillerSpaces)}"
}

fun printRowSeparator() {
    println("+----+------------+-------+---+---+--------------------------------------------+")
}

fun printTableHeader() {
    println(
        "+----+------------+-------+---+---+--------------------------------------------+\n" +
                "| N  |    Date    | Time  | P | D |                   Task                     |"
    )
}

fun getTaskOverdue(theDate: String?): String {
    // get the date from Task
    val taskDate: LocalDate? = theDate?.toLocalDate()

    // calculate current data
    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date

    // when the task is scheduled compared to today
    val numberOfDays: Int = taskDate?.let { currentDate.daysUntil(it) } ?: 0
    return when {
        numberOfDays < 0 -> DueTag.O.color
        numberOfDays > 0 -> DueTag.I.color
        else -> DueTag.T.color
    }
}

fun addToTaskList() {

    // initialize a dummy Task
    val priority = Priority.N
    val date = "1-1-1"
    val time = "1:1"
    val subTask: MutableList<String> = mutableListOf()
    val aTask = Task(priority, date, time, subTask)

    // populate task with real data
    aTask.priority = managePriority()
    aTask.date = manageDate()
    aTask.time = manageTime()
    aTask.subTask = manageTask(aTask)

    // add the task to taskList if subTask are > 0
    if (aTask.subTask.size > 0) {
        taskList.add(aTask)
    } else {
        println("The task is blank")
    }
}

fun managePriority(): Priority {
    while (true) {
        println("Input the task priority (C, H, N, L):")

        val priority = readLine()

        if (priority != null) {
            when (priority.uppercase()) {
                "C" -> return Priority.C
                "H" -> return Priority.H
                "N" -> return Priority.N
                "L" -> return Priority.L
            }
        }
    }
}

fun manageDate(): String {

    var theDate: LocalDate?

    // Date: <year (4 digits)>-<number of month>-<number of day in the month>
    while (true) {
        println("Input the date (yyyy-mm-dd):")
        try {
            val (year, month, day) = readln().split("-")
            theDate = LocalDate(year.toInt(), month.toInt(), day.toInt())
        } catch (e: RuntimeException) {
            println("The input date is invalid")
            continue
        }
        break
    }
    return theDate.toString()
}

fun manageTime(): String {

    var theTime: LocalDateTime?

    // Time: <hour (0 - 23)>:<minutes (0 - 59)>
    while (true) {
        println("Input the time (hh:mm):")
        try {
            val (hour, minute) = readln().split(":")
            theTime = LocalDateTime(
                year = 2022,
                monthNumber = 1,
                dayOfMonth = 1,
                hour = hour.toInt(),
                minute = minute.toInt()
            )
        } catch (e: RuntimeException) {
            println("The input time is invalid")
            continue
        }
        break
    }
    return theTime.toString().split("T").last()
}

fun manageTask(aTask: Task, overwrite: Boolean = false): MutableList<String> {
    // if I'm editing the task, clear all subtask
    if (overwrite) aTask.subTask.clear()

    println("Input a new task (enter a blank line to end):")
    // populate the task
    while (true) {
        val input = readLine()?.trim()

        if (input.isNullOrEmpty() || input.isBlank()) break
        aTask.subTask.add(input)
    }
    return aTask.subTask
}