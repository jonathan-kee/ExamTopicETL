package com.jonathankee.schema

class Answer(
    val number: Int,
    val questionNumber: Int,
    private val questionExam: String,
    private val text: String,
    val isCorrect: Boolean
) {
    fun getQuestionExam(): String? {
        return questionExam
    }

    fun getText(): String? {
        return text
    }

    override fun toString(): String {
        return "Answer{" +
                "number=" + number +
                ", questionNumber=" + questionNumber +
                ", questionExam='" + questionExam + '\'' +
                ", text='" + text + '\'' +
                ", isCorrect=" + isCorrect +
                '}'
    }

    companion object {
        @JvmStatic
        fun insertMultiple(answers: List<Answer>): String {
            val insertBoilerPlate = """
                    INSERT INTO scrape.answers
                    (number, question_number, question_exam, text, is_correct)
                    VALUES
                    
                    """.trimIndent()


            val data = answers.stream().map { answer: Answer ->
                // Replace ' with '' so SQL syntax doesn't break
                val safeText1 = if (answer.getQuestionExam() != null) answer.getQuestionExam()!!
                    .replace("'", "''") else ""
                val safeText2 = if (answer.getText() != null) answer.getText()!!.replace("'", "''") else ""
                ("(" + answer.number + ","
                        + answer.questionNumber + ","
                        + "'" + safeText1 + "'" + ","
                        + "'" + safeText2 + "'" + ","
                        + answer.isCorrect + ")")
            }.toList()

            val sb = StringBuilder()
            for (i in data.indices) {
                if (i != data.size - 1) {
                    sb.append(data[i] + ",\n")
                } else sb.append(data[i] + ";")
            }

            val fullInsert = insertBoilerPlate + sb.toString()
            return fullInsert
        }
    }
}