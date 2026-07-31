package com.jonathankee.schema;

import java.util.List;

public class Discussion {
    private int number;
    private int questionNumber;
    private String questionExam;
    private String selectedAnswer;
    private String text;
    private int upvote;

    public Discussion(int number, int questionNumber, String questionExam, String selectedAnswer, String text, int upvote) {
        this.number = number;
        this.questionNumber = questionNumber;
        this.questionExam = questionExam;
        this.selectedAnswer = selectedAnswer;
        this.text = text;
        this.upvote = upvote;
    }

    public Discussion() {
        this.number = 0;
        this.questionNumber = 0;
        this.questionExam = "";
        this.selectedAnswer = "";
        this.text = "";
        this.upvote = 0;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public void setQuestionExam(String questionExam) {
        this.questionExam = questionExam;
    }

    public void setSelectedAnswer(String selectedAnswer) {
        this.selectedAnswer = selectedAnswer;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setUpvote(int upvote) {
        this.upvote = upvote;
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

    public String getSelectedAnswer() {
        return selectedAnswer;
    }

    public String getText() {
        return text;
    }

    public int getUpvote() {
        return upvote;
    }

    @Override
    public String toString() {
        return "Dicussion{" +
                "number=" + number +
                ", questionNumber=" + questionNumber +
                ", questionExam='" + questionExam + '\'' +
                ", selectedAnswer='" + selectedAnswer + '\'' +
                ", text='" + text + '\'' +
                ", upvote=" + upvote +
                '}';
    }

    public static String insertMultiple(List<Discussion> dicussions) {
        String insertBoilerPlate = """
                    INSERT INTO discussions
                    (number, question_number, question_exam, selected_answer, text, upvote)
                    VALUES
                    """;

        List<String> data = dicussions.stream().map(dicussion -> {
            // Replace ' with '' so SQL syntax doesn't break
            String safeText1 = dicussion.getQuestionExam() != null ? dicussion.getQuestionExam().replace("'", "''") : "";
            String safeText2 = dicussion.getText() != null ? dicussion.getText().replace("'", "''") : "";
            String safeText3 = dicussion.getSelectedAnswer() != null ? dicussion.getSelectedAnswer().replace("'", "''") : "";

            return "(" + dicussion.getNumber() + ","
                    + dicussion.getQuestionNumber() + ","
                    + "'" + safeText1 + "'" + ","
                    + "'" + safeText2 + "'" + ","
                    + "'" + safeText3 + "'" + ","
                    + dicussion.getUpvote() + ")";
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