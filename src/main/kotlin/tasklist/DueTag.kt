package tasklist

enum class DueTag(val color: String) {
    I("\u001B[102m \u001B[0m"),
    T("\u001B[103m \u001B[0m"),
    O("\u001B[101m \u001B[0m")
}