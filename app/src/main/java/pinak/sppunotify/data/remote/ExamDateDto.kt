package pinak.sppunotify.data.remote

data class ExamDateDto(
    val courseName: String,
    val status: String,
    val startDate: String,
    val endDateWithoutLateFee: String,
    val endDateWithLateFee: String
)
