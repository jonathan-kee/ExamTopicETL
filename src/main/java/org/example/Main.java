package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.postgresql.util.PSQLException;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

// https://scrapfly.io/blog/posts/web-scraping-java-jsoup-html-parsing
public class Main {

    static class Question {
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
                    INSERT INTO browserless_questions
                    (number, exam, text)
                    VALUES
                    (%d,'%s','%s');
                    """.formatted(question.number, safeText1, safeText2);
            return insert;
        }

        public static String insertMultiple(List<Question> questions) {
            String insertBoilerPlate = """
                    INSERT INTO browserless_questions
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

    static public class Answer {
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
                    INSERT INTO browserless_answers
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

    static class Discussion {
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
                    INSERT INTO browserless_discussions
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

    private static Question Question(int number, String exam, Document doc) {
        // Question
        Elements questions = doc.selectXpath("/html/body/div[2]/div/div[4]/div/div[1]/div[2]/p");
        int questionsChildNodesLength = questions.getFirst().childNodes().size();
        List<Node> brTags = questions.getFirst().childNodes();
        StringBuilder sb = new StringBuilder();

        // Extract node value without the html tags
        for (int i = 0; i < questionsChildNodesLength; i++) {
            Node node = brTags.get(i);
            Node debugNode = node;
            if (debugNode.outerHtml().equals("<br>")) {
                sb.append("\n");
            } else if (node.hasAttr("src")) {
                String srcPath = node.attr("src");
                sb.append(srcPath);
            } else {
                sb.append(node.nodeValue().trim());
            }
        }
        return new Question(number, exam, sb.toString());
    }

    private static List<Answer> Answers(int questionNumber, String questionExam, Document doc) {
        // List of Answer to return
        List<Answer> answers = new ArrayList<>();

        // Answer
        Elements q = doc.selectXpath("/html/body/div[2]/div/div[4]/div/div[1]/div[2]/div[2]/ul");
        int questionsChildNodesLength = q.getFirst().childNodes().size();
        List<Node> liTag = q.getFirst().childNodes();
        StringBuilder sb = new StringBuilder();

        // Use Queue to simply logic
        Queue<String> pq = new PriorityQueue<>();

        String lastAnswerDescription = null;
        boolean answerDescriptionDuplicate = false;

        // Process OuterPTag
        OuterPTag:
        for (int i = 0, number = 1; i < questionsChildNodesLength; i++) {
            String answerDescription = liTag.get(i).nodeValue();
            boolean isCorrect = false;

            // Is answer correct
            if (liTag.get(i).hasAttr("class")) {
                if (liTag.get(i).attr("class").equals("multi-choice-item correct-hidden"))
                    isCorrect = true;
            }

            // Process InnerSpanTag
            int spanTagChildNodesLength = liTag.get(i).childNodes().size();
            List<Node> spanTag = liTag.get(i).childNodes();
            InnerSpanTag:
            for (int j = 0; j < spanTagChildNodesLength; j++) {
                var answerChoice = spanTag.get(j).nodeValue();
                if (answerChoice.trim().equals("")) continue InnerSpanTag;

                // Remove duplicate answerDescription
                if (lastAnswerDescription != null && lastAnswerDescription.trim().equals(answerChoice.trim()))
                    continue InnerSpanTag;
                lastAnswerDescription = answerDescription;

                // Add this first
                pq.add(answerChoice.trim());
            }
            // Add this second
            pq.add(answerDescription.trim());

            // Remove duplicate answerDescription
            sb.append(pq.poll());
            sb.append(" " + pq.poll());

            // Clean Sb
            if (sb.toString().trim().equals("null")) sb.delete(0, sb.length());
            else {
                sb.toString().replace("\n", "");
                Answer answer = new Answer(number, questionNumber, questionExam, sb.toString(), isCorrect);
                answers.add(answer);
                number++;
            }
            sb.delete(0, sb.length());
        }
        return answers;
    }

    private static void recursionDiscussions(Node node, Discussion discussion) {
        if (!node.attr("class").equals("comment-replies") && !node.childNodes().isEmpty()) {
            for (Node childNode : node.childNodes()) {
                recursionDiscussions(childNode, discussion);
            }
        }
        // There will be duplicates
        if (node.attr("class").equals("comment-selected-answers badge badge-warning")) {
            Node debug = node;
            String selectedAnswer = node.childNodes().get(1).outerHtml();
            String taglessSelectedAnswer = Jsoup.parse(selectedAnswer).text();
            // System.out.println("Discussion's selectedAnswer: " + taglessSelectedAnswer);
            discussion.setSelectedAnswer(taglessSelectedAnswer);
        }
        // There will be duplicates
        else if (node.attr("class").equals("comment-content")) {
            Node debug = node;
            String selectedComment = node.outerHtml();
            String taglessSelectedComment = Jsoup.parse(selectedComment).text();
            // System.out.println("Discussion's text: "+ taglessSelectedComment);
            discussion.setText(taglessSelectedComment);
        }
        // There will be duplicates
        else if (node.attr("class").equals("ml-2 upvote-text")) {
            Node debug = node;
            String selectedUpvote = node.childNodes().get(1).outerHtml();
            String taglessSelectedUpvote = Jsoup.parse(selectedUpvote).text();
            // System.out.println("Discussion's upvote: "+ taglessSelectedUpvote);
            discussion.setUpvote(Integer.parseInt(taglessSelectedUpvote));
        }
    }

    private static List<Discussion> Discussions(int questionNumber, String questionExam, Document doc) {
        // List of Answer to return
        List<Discussion> discussions = new ArrayList<>();

        // Discussions
        Elements Discussions = doc.selectXpath("/html/body/div[2]/div/div[4]/div/div[2]/div[2]/div/div/div[2]");
        // This will be wrong because you cannot load all elements when using static page
        int DiscussionsChildNodesLength = Discussions.getFirst().childNodes().size();

        try {
            for (int i = 0; i < DiscussionsChildNodesLength; i++) {
                Elements d = doc.selectXpath("/html/body/div[2]/div/div[4]/div/div[2]/div[2]/div/div/div[2]/div['" + i + "']/div/div[2]");
                List<Node> elementlist = d.get(i).childNodes();
                Discussion discussion = new Discussion();
                discussion.setNumber(i + 1);
                discussion.setQuestionNumber(questionNumber);
                discussion.setQuestionExam(questionExam);
                for (int j = 0; j < elementlist.size(); j++) {
                    Node node = elementlist.get(j);
                    // recursionDiscussions will settle below fields
                    // - selectedAnswer
                    // - text
                    // - upvote
                    recursionDiscussions(node, discussion);
                }
                discussions.add(discussion);
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Static page cannot get all elements");
        }

        return discussions;
    }

    private static void testJdbc() throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/postgres";
        try (Connection conn = DriverManager.getConnection(url, "postgres", "abc123");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM questions");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.println(rs.getString(1));
                System.out.println(rs.getString(2));
                System.out.println(rs.getString(3));
            }
        }
    }

    private static void executeQueryJdbc(String sql) throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/postgres";
        try (Connection conn = DriverManager.getConnection(url, "postgres", "abc123");
             ResultSet rs = conn.createStatement().executeQuery(sql)) {

        } catch (PSQLException e) {
            e.printStackTrace();
            // Do nothing with no result because inserting data
        }
    }

    private static void scrapeSingleDocument(Document doc) throws SQLException {
        // Clear existing data
        executeQueryJdbc("truncate browserless_answers;");
        executeQueryJdbc("truncate browserless_discussions;");

        Question q = Question(1, "1z0-071", doc);
        String insertQuestion = Question.insertSingle(q);
        executeQueryJdbc(insertQuestion);

        List<Answer> a = Answers(1, "1z0-071", doc);
        String insertAnswer = Answer.insertMultiple(a);
        executeQueryJdbc(insertAnswer);

        List<Discussion> d = Discussions(1, "1z0-071", doc);
        String insertDiscussion = Discussion.insertMultiple(d);
        executeQueryJdbc(insertDiscussion);
    }

    // This function does not execute jdbc query
    private static void scrapeMultipleDocuments(Document doc,
                                                int number,
                                                List<Question> multipleQuestion,
                                                List<List<Answer>> multipleAnswer,
                                                List<List<Discussion>> multipleDiscussion) throws SQLException {
        // Clear existing data
        executeQueryJdbc("truncate browserless_answers;");
        executeQueryJdbc("truncate browserless_discussions;");

        Question q = Question(number, "1z0-071", doc);
        multipleQuestion.add(q);

        // Actually this is flatmap operation
        List<Answer> a = Answers(number, "1z0-071", doc);
        multipleAnswer.add(a);

        // Actually this is flatmap operation
        List<Discussion> d = Discussions(number, "1z0-071", doc);
        multipleDiscussion.add(d);
    }

    private static void singleDocument() throws SQLException, IOException {
        // 1. Pass a File object instead of a raw String
        File input = new File("/Users/jonathankee/examTopicScraper/document/Document1.html");

        // 2. Specify the File and character encoding (usually "UTF-8")
        Document doc = Jsoup.parse(input, "UTF-8");

        scrapeSingleDocument(doc);
    }

    private static void multipleDocuments() throws IOException, SQLException {
        String[] paths = {
                "/Users/jonathankee/examTopicScraper/document/Document1.html",
                "/Users/jonathankee/examTopicScraper/document/Document2.html",
                "/Users/jonathankee/examTopicScraper/document/Document3.html",
                "/Users/jonathankee/examTopicScraper/document/Document4.html",
                "/Users/jonathankee/examTopicScraper/document/Document5.html"
        };

        List<File> files = new ArrayList<>();
        for (String path : paths) {
            File file = new File(path);
            files.add(file);
        }

        List<Question> questions = new ArrayList<>();
        List<List<Answer>> answers = new ArrayList<>();
        List<List<Discussion>> discussions = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            Document doc = Jsoup.parse(files.get(i), "UTF-8");
            // +1 to make sure follow normal numbering
            scrapeMultipleDocuments(doc, i+1, questions, answers, discussions);
        }

        List<Answer> allAnswers = answers.stream()
                .flatMap(List::stream) // Flattens Stream<List<Question>> into Stream<Question>
                .collect(Collectors.toList());

        List<Discussion> allDicussion = discussions.stream()
                .flatMap(List::stream) // Flattens Stream<List<Question>> into Stream<Question>
                .collect(Collectors.toList());

        executeQueryJdbc("truncate browserless_answers;");
        executeQueryJdbc("truncate browserless_discussions;");

        String insertQuestions = Question.insertMultiple(questions);
        executeQueryJdbc(insertQuestions);

        String insertAnswers = Answer.insertMultiple(allAnswers);
        executeQueryJdbc(insertAnswers);

        String insertDiscussions = Discussion.insertMultiple(allDicussion);
        executeQueryJdbc(insertDiscussions);
    }

    public static void main(String[] args) throws SQLException, IOException {
        multipleDocuments();
    }
}



