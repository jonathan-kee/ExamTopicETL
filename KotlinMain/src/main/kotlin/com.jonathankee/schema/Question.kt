package com.jonathankee.schema

class Question(val number: Int,
               private val exam: String,
               private val text: String) {
    fun getExam(): String? {
        return exam
    }

    fun getText(): String? {
        return text
    }

    override fun toString(): String {
        return "Question{" +
                "number=" + number +
                ", exam='" + exam + '\'' +
                ", text='" + text + '\'' +
                '}'
    }

    companion object {
        @JvmStatic
        fun insertSingle(question: Question): String {
            // Replace ' with '' so SQL syntax doesn't break
            val safeText1 = if (question.getExam() != null) question.getExam()!!.replace("'", "''") else ""
            val safeText2 = if (question.getText() != null) question.getText()!!.replace("'", "''") else ""

            val insert = """
                    INSERT INTO scrape.questions
                    (number, exam, text)
                    VALUES
                    (%d,'%s','%s');
                    
                    """.trimIndent().formatted(question.number, safeText1, safeText2)
            return insert
        }

        @JvmStatic
        fun insertMultiple(questions: List<Question>): String {
            val insertBoilerPlate = """
                    INSERT INTO scrape.questions
                    (number, exam, text)
                    VALUES
                    
                    """.trimIndent()

            val data = questions.stream().map { question: Question ->
                // Replace ' with '' so SQL syntax doesn't break
                val safeText1 = if (question.getExam() != null) question.getExam()!!.replace("'", "''") else ""
                val safeText2 = if (question.getText() != null) question.getText()!!.replace("'", "''") else ""
                ("(" + question.number + ","
                        + "'" + safeText1 + "'" + ","
                        + "'" + safeText2 + "'" + ")")
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