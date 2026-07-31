package com.jonathankee.schema;

import java.util.List;

public class Answer {
    private int number;
    private int questionNumber;
    private String questionExam;
    private String text;
    private boolean isCorrect;

    public Answer(int number, int questionNumber, String questionExam, String text, boolean isCorrect) {
        this.number = number;
        this.questionNumber = questionNumber;
        this.questionExam = questionExam;
        this.text = text;
        this.isCorrect = isCorrect;
    }

    public int getNumber() {
        return number;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public String getQuestionExam() {
        return questionExam;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    @Override
    public String toString() {
        return "Answer{" +
                "number=" + number +
                ", questionNumber=" + questionNumber +
                ", questionExam='" + questionExam + '\'' +
                ", text='" + text + '\'' +
                ", isCorrect=" + isCorrect +
                '}';
    }

    public static String insertMultiple(List<Answer> answers) {
        String insertBoilerPlate = """
                    INSERT INTO answers
                    (number, question_number, question_exam, text, is_correct)
                    VALUES
                    """;


        List<String> data = answers.stream().map(answer -> {
            // Replace ' with '' so SQL syntax doesn't break
            String safeText1 = answer.getQuestionExam() != null ? answer.getQuestionExam().replace("'", "''") : "";
            String safeText2 = answer.getText() != null ? answer.getText().replace("'", "''") : "";

            return "(" + answer.getNumber() + ","
                    + answer.getQuestionNumber() + ","
                    + "'" + safeText1 + "'" + ","
                    + "'" + safeText2 + "'" + ","
                    + answer.isCorrect() + ")";
        }).toList();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            if (i != data.size() - 1) {
                sb.append(data.get(i) + ",\n");
            } else
                sb.append(data.get(i) + ";");
        }

        String fullInsert = insertBoilerPlate + sb.toString();
        return fullInsert;
    }
}
