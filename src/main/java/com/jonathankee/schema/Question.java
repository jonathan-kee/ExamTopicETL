package com.jonathankee.schema;

import java.util.List;

public class Question {
    private int number;
    private String exam;
    private String text;

    public Question(int number, String exam, String text) {
        this.number = number;
        this.exam = exam;
        this.text = text;
    }

    public int getNumber() {
        return number;
    }

    public String getExam() {
        return exam;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "Question{" +
                "number=" + number +
                ", exam='" + exam + '\'' +
                ", text='" + text + '\'' +
                '}';
    }

    public static String insertSingle(Question question) {
        // Replace ' with '' so SQL syntax doesn't break
        String safeText1 = question.getExam() != null ? question.getExam().replace("'", "''") : "";
        String safeText2 = question.getText() != null ? question.getText().replace("'", "''") : "";

        String insert = """
                    INSERT INTO scrape.questions
                    (number, exam, text)
                    VALUES
                    (%d,'%s','%s');
                    """.formatted(question.number, safeText1, safeText2);
        return insert;
    }

    public static String insertMultiple(List<Question> questions) {
        String insertBoilerPlate = """
                    INSERT INTO scrape.questions
                    (number, exam, text)
                    VALUES
                    """;

        List<String> data = questions.stream().map(question -> {
            // Replace ' with '' so SQL syntax doesn't break
            String safeText1 = question.getExam() != null ? question.getExam().replace("'", "''") : "";
            String safeText2 = question.getText() != null ? question.getText().replace("'", "''") : "";

            return "(" + question.getNumber() + ","
                    + "'" + safeText1 + "'" + ","
                    + "'" + safeText2 + "'" + ")";
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
