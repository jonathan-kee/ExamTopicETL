package org.example;

import com.jonathankee.schema.Answer;
import com.jonathankee.schema.Discussion;
import com.jonathankee.schema.Question;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jonathankee.database.Database.executeQueryJdbc;

public class Main {

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

    private static List<Answer> Answers(int questionNumber, String questionExam, Document doc) throws NoSuchElementException {
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

    private static void scrapeSingleDocument(Document doc) throws SQLException {
        // Clear existing data
        executeQueryJdbc("truncate answers;");
        executeQueryJdbc("truncate discussions;");

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
        executeQueryJdbc("truncate answers;");
        executeQueryJdbc("truncate discussions;");

        Question q = Question(number, "1z0-071", doc);
        multipleQuestion.add(q);

        try {
            // Actually this is flatmap operation
            List<Answer> a = Answers(number, "1z0-071", doc); // Might throw error
            multipleAnswer.add(a);
        } catch (NoSuchElementException e) {
            System.out.println("The answers are screenshots, let Javascript handle it");
            // number is question number
            Answer answer = new Answer(1, number, "1z0-071", null, false);
            Answer answer2 = new Answer(2, number, "1z0-071", null, false);
            Answer answer3 = new Answer(3, number, "1z0-071", null, false);
            Answer answer4 = new Answer(4, number, "1z0-071", null, false);
            Answer answer5 = new Answer(5, number, "1z0-071", null, false);
            List<Answer> dirtyAnswer = List.of(answer, answer2, answer3, answer4, answer5);
            multipleAnswer.add(dirtyAnswer);
        }

        // Actually this is flatmap operation
        List<Discussion> d = Discussions(number, "1z0-071", doc);
        multipleDiscussion.add(d);
    }

    // todo: For multithreaded purpose
    // The reason I put multipleQuestion inside is avoid race condition (multiple thread access same variable / reference)
    private static List<Question> scrapeMultipleDocumentsQuestion(Document doc, int number) {
        List<Question> multipleQuestion = new ArrayList<>();
        Question q = Question(number, "1z0-071", doc);
        multipleQuestion.add(q);
        return multipleQuestion;
    }

    // todo: For multithreaded purpose
    // The reason I put multipleAnswer inside is avoid race condition (multiple thread access same variable / reference)
    private static List<Answer> scapeMultipleDocumentsAnswer(Document doc, int number) {
        List<Answer> multipleAnswer = null;
        try {
            // Actually this is flatmap operation
            multipleAnswer = Answers(number, "1z0-071", doc); // Might throw error
        } catch (NoSuchElementException e) {
            System.out.println("The answers are screenshots");
            // number is question number
            Answer answer = new Answer(1, number, "1z0-071", null, false);
            Answer answer2 = new Answer(2, number, "1z0-071", null, false);
            Answer answer3 = new Answer(3, number, "1z0-071", null, false);
            Answer answer4 = new Answer(4, number, "1z0-071", null, false);
            Answer answer5 = new Answer(5, number, "1z0-071", null, false);
            multipleAnswer = List.of(answer, answer2, answer3, answer4, answer5);
        }
        return multipleAnswer;
    }

    private static List<Discussion> scrapeMultipleDocumentsDiscussion(Document doc, int number) {
        return Discussions(number, "1z0-071", doc);
    }

    private static void singleDocument(String documentName) throws SQLException, IOException {
        // 1. Pass a File object instead of a raw String
        File input = new File("/Users/jonathankee/examTopicScraper/static_page/src/main/resources/tmp/"+documentName);

        // 2. Specify the File and character encoding (usually "UTF-8")
        Document doc = Jsoup.parse(input, "UTF-8");

        scrapeSingleDocument(doc);
    }

    // Helper method to safely extract numbers from filenames
    private static int extractNumber(File file) {
        String digits = file.getName().replaceAll("\\D+", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    private static void multipleDocuments() throws IOException, SQLException {
        Path folderPath = Paths.get("/Users/jonathankee/examTopicScraper/static_page/src/main/resources");

        List<File> files = Collections.emptyList();

        try (Stream<Path> paths = Files.list(folderPath)) {
            Optional<Path> firstPath = paths
                    .peek(path -> System.out.println(path.toAbsolutePath()))
                    .findFirst();

            if (firstPath.isPresent() && Files.isDirectory(firstPath.get())) {
                try (Stream<Path> subPaths = Files.list(firstPath.get())) {
                    files = subPaths
                            .map(Path::toFile)
                            .sorted(Comparator.comparingInt(Main::extractNumber))
                            .toList(); // Safely collected after sorting
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<File> debugFile = files;

        List<Question> questions = new ArrayList<>();
        List<List<Answer>> answers = new ArrayList<>();
        List<List<Discussion>> discussions = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            Document doc = Jsoup.parse(files.get(i), "UTF-8");
            // +1 to make sure follow normal numbering
            scrapeMultipleDocuments(doc, i + 1, questions, answers, discussions);
        }

        List<Answer> allAnswers = answers.stream()
                .flatMap(List::stream) // Flattens Stream<List<Question>> into Stream<Question>
                .collect(Collectors.toList());

        List<Discussion> allDicussion = discussions.stream()
                .flatMap(List::stream) // Flattens Stream<List<Question>> into Stream<Question>
                .collect(Collectors.toList());

        executeQueryJdbc("truncate answers;");
        executeQueryJdbc("truncate discussions;");

        String insertQuestions = Question.insertMultiple(questions);
        executeQueryJdbc(insertQuestions);

        String insertAnswers = Answer.insertMultiple(allAnswers);
        executeQueryJdbc(insertAnswers);

        String insertDiscussions = Discussion.insertMultiple(allDicussion);
        executeQueryJdbc(insertDiscussions);
    }

    private static void multipleDocumentsMultiThread() throws IOException, SQLException {
        Path folderPath = Paths.get("/Users/jonathankee/examTopicScraper/static_page/src/main/resources");

        List<File> files = Collections.emptyList();

        try (Stream<Path> paths = Files.list(folderPath)) {
            Optional<Path> firstPath = paths
                    .peek(path -> System.out.println(path.toAbsolutePath()))
                    .findFirst();

            if (firstPath.isPresent() && Files.isDirectory(firstPath.get())) {
                try (Stream<Path> subPaths = Files.list(firstPath.get())) {
                    files = subPaths
                            .map(Path::toFile)
                            .sorted(Comparator.comparingInt(Main::extractNumber))
                            .toList(); // Safely collected after sorting
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<File> debugFile = files;

        List<Question> questions = new ArrayList<>();
        List<List<Answer>> answers = new ArrayList<>();
        List<List<Discussion>> discussions = new ArrayList<>();

        int cpuCount = Runtime.getRuntime().availableProcessors();
        try (ExecutorService executor = Executors.newFixedThreadPool(cpuCount)) {
            for (int i = 0; i < files.size(); i++) {
                // submits tasks that we need result from before thread can continue
                // get() will wait for the computation to finish
                int finalI = i;
                List<File> finalFiles = files;
                var doc = executor.submit(() -> Jsoup.parse(finalFiles.get(finalI), "UTF-8"));
                // The below three task are depended on doc finishing
                // +1 to make sure follow normal numbering
                var questionsE = executor.submit(() -> scrapeMultipleDocumentsQuestion(doc.get(), finalI + 1));
                var answersE = executor.submit(() -> scapeMultipleDocumentsAnswer(doc.get(), finalI + 1));
                var discussionsE = executor.submit(() -> scrapeMultipleDocumentsDiscussion(doc.get(), finalI + 1));

                for (Question question : questionsE.get()) {
                    questions.add(question);
                }
                answers.add(answersE.get());
                discussions.add(discussionsE.get());
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<Answer> allAnswers = answers.stream()
                .flatMap(List::stream) // Flattens Stream<List<Question>> into Stream<Question>
                .collect(Collectors.toList());

        List<Discussion> allDicussion = discussions.stream()
                .flatMap(List::stream) // Flattens Stream<List<Question>> into Stream<Question>
                .collect(Collectors.toList());

        executeQueryJdbc("truncate answers;");
        executeQueryJdbc("truncate discussions;");

        try (ExecutorService executor = Executors.newFixedThreadPool(cpuCount)) {
            var insertQuestions = executor.submit(()-> Question.insertMultiple(questions) );
            var insertAnswers = executor.submit(()-> Answer.insertMultiple(allAnswers) );
            var insertDiscussions = executor.submit(()-> Discussion.insertMultiple(allDicussion) );
            try {
                executeQueryJdbc(insertQuestions.get());
                executeQueryJdbc(insertAnswers.get());
                executeQueryJdbc(insertDiscussions.get());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) throws SQLException, IOException {
        Instant startInstant = Instant.now();
        // multipleDocuments();                             // 6 seconds    (Single Threaded)
        multipleDocumentsMultiThread();                  // 2 seconds    (Multi Threaded)    (6/2) = 3x speed up

        //singleDocument("document8.html");
        Instant endInstant = Instant.now();
        Duration duration = Duration.between(startInstant, endInstant);
        System.out.println("Execution time: " + duration.toMillis() + " ms");
        System.out.println("Formatted: " + duration.toSeconds() + " seconds");
    }
}