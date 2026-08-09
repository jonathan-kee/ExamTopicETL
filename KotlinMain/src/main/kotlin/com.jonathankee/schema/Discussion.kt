package com.jonathankee.schema

class Discussion {
    var number: Int
    var questionNumber: Int
    private var questionExam: String
    private var selectedAnswer: String
    private var text: String
    var upvote: Int

    constructor(
        number: Int,
        questionNumber: Int,
        questionExam: String,
        selectedAnswer: String,
        text: String,
        upvote: Int
    ) {
        this.number = number
        this.questionNumber = questionNumber
        this.questionExam = questionExam
        this.selectedAnswer = selectedAnswer
        this.text = text
        this.upvote = upvote
    }

    constructor() {
        this.number = 0
        this.questionNumber = 0
        this.questionExam = ""
        this.selectedAnswer = ""
        this.text = ""
        this.upvote = 0
    }

    fun setQuestionExam(questionExam: String) {
        this.questionExam = questionExam
    }

    fun setSelectedAnswer(selectedAnswer: String) {
        this.selectedAnswer = selectedAnswer
    }

    fun setText(text: String) {
        this.text = text
    }

    fun getQuestionExam(): String? {
        return questionExam
    }

    fun getSelectedAnswer(): String? {
        return selectedAnswer
    }

    fun getText(): String? {
        return text
    }

    override fun toString(): String {
        return "Dicussion{" +
                "number=" + number +
                ", questionNumber=" + questionNumber +
                ", questionExam='" + questionExam + '\'' +
                ", selectedAnswer='" + selectedAnswer + '\'' +
                ", text='" + text + '\'' +
                ", upvote=" + upvote +
                '}'
    }

    companion object {
        fun insertMultiple(dicussions: List<Discussion>): String {
            val insertBoilerPlate = """
                    INSERT INTO scrape.discussions
                    (number, question_number, question_exam, selected_answer, text, upvote)
                    VALUES
                    
                    """.trimIndent()

            val data = dicussions.stream().map { dicussion: Discussion ->
                // Replace ' with '' so SQL syntax doesn't break
                val safeText1 = if (dicussion.getQuestionExam() != null) dicussion.getQuestionExam()!!
                    .replace("'", "''") else ""
                val safeText2 = if (dicussion.getText() != null) dicussion.getText()!!.replace("'", "''") else ""
                val safeText3 = if (dicussion.getSelectedAnswer() != null) dicussion.getSelectedAnswer()!!
                    .replace("'", "''") else ""
                ("(" + dicussion.number + ","
                        + dicussion.questionNumber + ","
                        + "'" + safeText1 + "'" + ","
                        + "'" + safeText2 + "'" + ","
                        + "'" + safeText3 + "'" + ","
                        + dicussion.upvote + ")")
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