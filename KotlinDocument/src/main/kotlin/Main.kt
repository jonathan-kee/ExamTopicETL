package com.jonathankee

import com.jonathankee.database.Database
import com.jonathankee.schema.Answer
import com.jonathankee.schema.Discussion
import com.jonathankee.schema.Question
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Node
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.SQLException
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.stream.Collectors

object Main {
    private fun Question(number: Int, exam: String, doc: Document): Question {
        // Question
        val questions = doc.selectXpath("/html/body/div[2]/div/div[4]/div/div[1]/div[2]/p")
        val questionsChildNodesLength = questions.first.childNodes().size
        val brTags = questions.first.childNodes()
        val sb = StringBuilder()

        // Extract node value without the html tags
        for (i in 0 until questionsChildNodesLength) {
            val node = brTags[i]
            val debugNode = node
            if (debugNode.outerHtml() == "<br>") {
                sb.append("\n")
            } else if (node.hasAttr("src")) {
                val srcPath = node.attr("src")
                sb.append(srcPath)
            } else {
                sb.append(node.nodeValue().trim { it <= ' ' })
            }
        }
        return Question(number, exam, sb.toString())
    }

    @Throws(NoSuchElementException::class)
    private fun Answers(questionNumber: Int, questionExam: String, doc: Document): List<Answer?> {
        // List of Answer to return
        val answers: MutableList<Answer?> = ArrayList()
        // Answer
        val q = doc.selectXpath("/html/body/div[2]/div/div[4]/div/div[1]/div[2]/div[2]/ul")
        val questionsChildNodesLength = q.first.childNodes().size
        val liTag = q.first.childNodes()
        val sb = StringBuilder()

        // Use Queue to simply logic
        val pq: Queue<String> = PriorityQueue()

        var lastAnswerDescription: String? = null
        val answerDescriptionDuplicate = false

        var i = 0
        var number = 1
        OuterPTag@ while (i < questionsChildNodesLength) {
            val answerDescription = liTag[i].nodeValue()
            var isCorrect = false

            // Is answer correct
            if (liTag[i].hasAttr("class")) {
                if (liTag[i].attr("class") == "multi-choice-item correct-hidden") isCorrect = true
            }

            // Process InnerSpanTag
            val spanTagChildNodesLength = liTag[i].childNodes().size
            val spanTag = liTag[i].childNodes()
            InnerSpanTag@ for (j in 0 until spanTagChildNodesLength) {
                val answerChoice = spanTag[j].nodeValue()
                if (answerChoice.trim { it <= ' ' } == "") continue@InnerSpanTag

                // Remove duplicate answerDescription
                if (lastAnswerDescription != null && lastAnswerDescription.trim { it <= ' ' } == answerChoice.trim { it <= ' ' }) continue@InnerSpanTag
                lastAnswerDescription = answerDescription

                // Add this first
                pq.add(answerChoice.trim { it <= ' ' })
            }
            // Add this second
            pq.add(answerDescription.trim { it <= ' ' })

            // Remove duplicate answerDescription
            sb.append(pq.poll())
            sb.append(" " + pq.poll())

            // Clean Sb
            if (sb.toString().trim { it <= ' ' } == "null") sb.delete(0, sb.length)
            else {
                sb.toString().replace("\n", "")
                val answer = Answer(number, questionNumber, questionExam, sb.toString(), isCorrect)
                answers.add(answer)
                number++
            }
            sb.delete(0, sb.length)
            i++
        }
        return answers
    }

    private fun recursionDiscussions(node: Node, discussion: Discussion) {
        if (node.attr("class") != "comment-replies" && !node.childNodes().isEmpty()) {
            for (childNode in node.childNodes()) {
                recursionDiscussions(childNode, discussion)
            }
        }
        // There will be duplicates
        if (node.attr("class") == "comment-selected-answers badge badge-warning") {
            val debug = node
            val selectedAnswer = node.childNodes()[1].outerHtml()
            val taglessSelectedAnswer = Jsoup.parse(selectedAnswer).text()
            // System.out.println("Discussion's selectedAnswer: " + taglessSelectedAnswer);
            discussion.setSelectedAnswer(taglessSelectedAnswer)
        } else if (node.attr("class") == "comment-content") {
            val debug = node
            val selectedComment = node.outerHtml()
            val taglessSelectedComment = Jsoup.parse(selectedComment).text()
            // System.out.println("Discussion's text: "+ taglessSelectedComment);
            discussion.setText(taglessSelectedComment)
        } else if (node.attr("class") == "ml-2 upvote-text") {
            val debug = node
            val selectedUpvote = node.childNodes()[1].outerHtml()
            val taglessSelectedUpvote = Jsoup.parse(selectedUpvote).text()
            // System.out.println("Discussion's upvote: "+ taglessSelectedUpvote);
            discussion.upvote = taglessSelectedUpvote.toInt()
        }
    }

    private fun Discussions(questionNumber: Int, questionExam: String, doc: Document): List<Discussion> {
        // List of Answer to return
        val discussions: MutableList<Discussion> = ArrayList()

        // Discussions
        val Discussions = doc.selectXpath("/html/body/div[2]/div/div[4]/div/div[2]/div[2]/div/div/div[2]")
        // This will be wrong because you cannot load all elements when using static page
        val DiscussionsChildNodesLength = Discussions.first.childNodes().size

        try {
            for (i in 0 until DiscussionsChildNodesLength) {
                val d =
                    doc.selectXpath("/html/body/div[2]/div/div[4]/div/div[2]/div[2]/div/div/div[2]/div['$i']/div/div[2]")
                val elementlist = d[i].childNodes()
                val discussion = Discussion()
                discussion.number = i + 1
                discussion.questionNumber = questionNumber
                discussion.setQuestionExam(questionExam)
                for (j in elementlist.indices) {
                    val node = elementlist[j]
                    // recursionDiscussions will settle below fields
                    // - selectedAnswer
                    // - text
                    // - upvote
                    recursionDiscussions(node, discussion)
                }
                discussions.add(discussion)
            }
        } catch (e: IndexOutOfBoundsException) {
            println("Static page cannot get all elements")
        }

        return discussions
    }

    @Throws(SQLException::class)
    private fun scrapeSingleDocument(doc: Document) {
        // Clear existing data
        Database.executeQueryJdbc("truncate answers;")
        Database.executeQueryJdbc("truncate discussions;")

        val q = Question(1, "1z0-071", doc)
        val insertQuestion = Question.insertSingle(q)
        Database.executeQueryJdbc(insertQuestion)

        val a = Answers(1, "1z0-071", doc)
        val insertAnswer = Answer.insertMultiple(a)
        Database.executeQueryJdbc(insertAnswer)

        val d = Discussions(1, "1z0-071", doc)
        val insertDiscussion = Discussion.insertMultiple(d)
        Database.executeQueryJdbc(insertDiscussion)
    }

    // This function does not execute jdbc query
    @Throws(SQLException::class)
    private fun scrapeMultipleDocuments(
        doc: Document,
        number: Int,
        multipleQuestion: MutableList<Question>,
        multipleAnswer: MutableList<List<Answer?>>,
        multipleDiscussion: MutableList<List<Discussion>>
    ) {
        // Clear existing data
        Database.executeQueryJdbc("truncate scrape.answers;")
        Database.executeQueryJdbc("truncate scrape.discussions;")
        val q = Question(number, "1z0-071", doc)
        multipleQuestion.add(q)

        try {
            // Actually this is flatmap operation
            val a = Answers(number, "1z0-071", doc) // Might throw error
            multipleAnswer.add(a)
        } catch (e: NoSuchElementException) {
            println("The answers are screenshots, let Javascript handle it")
            // number is question number
            val answer = Answer(1, number, "1z0-071", null, false)
            val answer2 = Answer(2, number, "1z0-071", null, false)
            val answer3 = Answer(3, number, "1z0-071", null, false)
            val answer4 = Answer(4, number, "1z0-071", null, false)
            val answer5 = Answer(5, number, "1z0-071", null, false)
            val dirtyAnswer = java.util.List.of(answer, answer2, answer3, answer4, answer5)
            multipleAnswer.add(dirtyAnswer)
        }

        // Actually this is flatmap operation
        val d = Discussions(number, "1z0-071", doc)
        multipleDiscussion.add(d)
    }

    // todo: For multithreaded purpose
    // The reason I put multipleQuestion inside is avoid race condition (multiple thread access same variable / reference)
    private fun scrapeMultipleDocumentsQuestion(doc: Document, number: Int): List<Question> {
        val multipleQuestion: MutableList<Question> = ArrayList()
        val q = Question(number, "1z0-071", doc)
        multipleQuestion.add(q)
        return multipleQuestion
    }

    // todo: For multithreaded purpose
    // The reason I put multipleAnswer inside is avoid race condition (multiple thread access same variable / reference)
    private fun scapeMultipleDocumentsAnswer(doc: Document, number: Int): List<Answer?> {
        var multipleAnswer: List<Answer?>? = null
        try {
            // Actually this is flatmap operation
            multipleAnswer = Answers(number, "1z0-071", doc) // Might throw error
        } catch (e: NoSuchElementException) {
            println("The answers are screenshots")
            // number is question number
            val answer = Answer(1, number, "1z0-071", null, false)
            val answer2 = Answer(2, number, "1z0-071", null, false)
            val answer3 = Answer(3, number, "1z0-071", null, false)
            val answer4 = Answer(4, number, "1z0-071", null, false)
            val answer5 = Answer(5, number, "1z0-071", null, false)
            multipleAnswer = java.util.List.of(answer, answer2, answer3, answer4, answer5)
        }
        return multipleAnswer!!
    }

    private fun scrapeMultipleDocumentsDiscussion(doc: Document, number: Int): List<Discussion> {
        return Discussions(number, "1z0-071", doc)
    }

    @Throws(SQLException::class, IOException::class)
    private fun singleDocument(documentName: String) {
        // 1. Pass a File object instead of a raw String
        val input = File("/Users/jonathankee/examTopicScraper/static_page/src/main/resources/tmp/$documentName")

        // 2. Specify the File and character encoding (usually "UTF-8")
        val doc = Jsoup.parse(input, "UTF-8")

        scrapeSingleDocument(doc)
    }

    // Helper method to safely extract numbers from filenames
    private fun extractNumber(file: File?): Int {
        val digits = file?.name?.replace("\\D+".toRegex(), "")
        return if (digits?.isEmpty() == true) 0 else digits?.toInt() ?: 0
    }

    @Throws(IOException::class, SQLException::class)
    private fun multipleDocuments() {
        var files = emptyList<File>()

        val folderPath = Paths.get("/Users/jonathankee/examTopicScraper/static_page/src/main/resources/tmp")

        val debugDirectory = Files.isDirectory(folderPath)

        if (debugDirectory) {
            Files.list(folderPath).use { subPaths ->
                files = subPaths
                    .map { obj: Path -> obj.toFile() }
                    .sorted(Comparator.comparingInt { obj: File? -> extractNumber(obj ?: null) })
                    .toList() // Safely collected after sorting
            }
        }

        val debugFile = files

        val questions: MutableList<Question> = ArrayList()
        val answers: MutableList<List<Answer?>> = ArrayList()
        val discussions: MutableList<List<Discussion>> = ArrayList()

        for (i in files.indices) {
            val doc = Jsoup.parse(files[i], "UTF-8")
            // +1 to make sure follow normal numbering
            scrapeMultipleDocuments(doc, i + 1, questions, answers, discussions)
        }

        val allAnswers = answers.stream()
            .flatMap { obj: List<Answer?> -> obj.stream() }  // Flattens Stream<List<Question>> into Stream<Question>
            .collect(Collectors.toList())

        val allDicussion = discussions.stream()
            .flatMap { obj: List<Discussion> -> obj.stream() }  // Flattens Stream<List<Question>> into Stream<Question>
            .collect(Collectors.toList())

        Database.executeQueryJdbc("truncate scrape.answers;")
        Database.executeQueryJdbc("truncate scrape.discussions;")

        val insertQuestions = Question.insertMultiple(questions)
        Database.executeQueryJdbc(insertQuestions)

        val insertAnswers = Answer.insertMultiple(allAnswers)
        Database.executeQueryJdbc(insertAnswers)

        val insertDiscussions = Discussion.insertMultiple(allDicussion)
        Database.executeQueryJdbc(insertDiscussions)
    }

    @Throws(IOException::class, SQLException::class)
    private fun multipleDocumentsMultiThread() {
        var files = emptyList<File>()

        val folderPath = Paths.get("/Users/jonathankee/examTopicScraper/static_page/src/main/resources/tmp/")

        val debugDirectory = Files.isDirectory(folderPath)

        if (debugDirectory) {
            Files.list(folderPath).use { subPaths ->
                files = subPaths
                    .map { obj: Path -> obj.toFile() }
                    .sorted(Comparator.comparingInt { obj: File? -> extractNumber(obj) })
                    .toList() // Safely collected after sorting
            }
        }

        val debugFile = files

        val questions: MutableList<Question> = ArrayList()
        val answers: MutableList<List<Answer?>> = ArrayList()
        val discussions: MutableList<List<Discussion>> = ArrayList()

        val cpuCount = Runtime.getRuntime().availableProcessors()
        try {
            Executors.newFixedThreadPool(cpuCount).use { executor ->
                for (i in files.indices) {
                    // submits tasks that we need result from before thread can continue
                    // get() will wait for the computation to finish
                    val finalI = i
                    val finalFiles = files
                    val doc = executor.submit<Document> {
                        Jsoup.parse(
                            finalFiles[finalI],
                            "UTF-8"
                        )
                    }
                    // The below three task are depended on doc finishing
                    // +1 to make sure follow normal numbering
                    val questionsE = executor.submit<List<Question>> {
                        scrapeMultipleDocumentsQuestion(
                            doc.get(),
                            finalI + 1
                        )
                    }
                    val answersE = executor.submit<List<Answer?>> {
                        scapeMultipleDocumentsAnswer(
                            doc.get(),
                            finalI + 1
                        )
                    }
                    val discussionsE = executor.submit<List<Discussion>> {
                        scrapeMultipleDocumentsDiscussion(
                            doc.get(),
                            finalI + 1
                        )
                    }

                    for (question in questionsE.get()) {
                        questions.add(question)
                    }
                    answers.add(answersE.get())
                    discussions.add(discussionsE.get())
                }
            }
        } catch (e: ExecutionException) {
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }

        val allAnswers = answers.stream()
            .flatMap { obj: List<Answer?> -> obj.stream() }  // Flattens Stream<List<Question>> into Stream<Question>
            .collect(Collectors.toList())

        val allDicussion = discussions.stream()
            .flatMap { obj: List<Discussion> -> obj.stream() }  // Flattens Stream<List<Question>> into Stream<Question>
            .collect(Collectors.toList())

        Database.executeQueryJdbc("truncate scrape.answers;")
        Database.executeQueryJdbc("truncate scrape.discussions;")

        Executors.newFixedThreadPool(cpuCount).use { executor ->
            val insertQuestions = executor.submit<String> {
                Question.insertMultiple(
                    questions
                )
            }
            val insertAnswers = executor.submit<String> { Answer.insertMultiple(allAnswers) }
            val insertDiscussions = executor.submit<String> {
                Discussion.insertMultiple(
                    allDicussion
                )
            }
            try {
                Database.executeQueryJdbc(insertQuestions.get())
                Database.executeQueryJdbc(insertAnswers.get())
                Database.executeQueryJdbc(insertDiscussions.get())
            } catch (e: SQLException) {
                throw RuntimeException(e)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            } catch (e: ExecutionException) {
                throw RuntimeException(e)
            }
        }
    }

    private data class ScrapedResult(
        val questions: List<Question>,
        val answers: List<Answer?>,
        val discussions: List<Discussion>
    )

    suspend fun multipleDocumentsCoroutine() = coroutineScope {
        val folderPath = Paths.get("/Users/jonathankee/examTopicScraper/static_page/src/main/resources/tmp/")

        // 1. DISK I/O: Reading directory and file paths -> Dispatchers.IO
        val files: List<File> = withContext(Dispatchers.IO) {
            if (Files.isDirectory(folderPath)) {
                Files.list(folderPath).use { subPaths ->
                    subPaths
                        .map { it.toFile() }
                        .sorted(Comparator.comparingInt { obj: File? -> extractNumber(obj) })
                        .toList()
                }
            } else {
                emptyList()
            }
        }

        if (files.isEmpty()) return@coroutineScope

        // 2. PARSING & SCRAPING:
        // Reading file bytes from disk (IO) -> Parsing HTML DOM & Traversing nodes (CPU)
        val scrapedResults: List<ScrapedResult> = files.mapIndexed { index, file ->
            async(Dispatchers.IO) {
                // Read file & parse HTML into DOM (Disk I/O + parse)
                val doc: Document = Jsoup.parse(file, "UTF-8")
                val documentIndex = index + 1

                // CPU-BOUND: Extracting elements from the parsed Jsoup DOM tree
                withContext(Dispatchers.Default) {
                    val questionsDeferred = async { scrapeMultipleDocumentsQuestion(doc, documentIndex) }
                    val answersDeferred = async { scapeMultipleDocumentsAnswer(doc, documentIndex) }
                    val discussionsDeferred = async { scrapeMultipleDocumentsDiscussion(doc, documentIndex) }

                    ScrapedResult(
                        questions = questionsDeferred.await(),
                        answers = answersDeferred.await(),
                        discussions = discussionsDeferred.await()
                    )
                }
            }
        }.awaitAll()

        // 3. CPU-BOUND: List operations & transformations -> Dispatchers.Default
        val (questions, allAnswers, allDiscussions) = withContext(Dispatchers.Default) {
            Triple(
                scrapedResults.flatMap { it.questions },
                scrapedResults.flatMap { it.answers },
                scrapedResults.flatMap { it.discussions }
            )
        }

        // 4. CPU-BOUND: Generating SQL insert strings concurrently -> Dispatchers.Default
        val (insertQuestionsSql, insertAnswersSql, insertDiscussionsSql) = withContext(Dispatchers.Default) {
            val qDeferred = async { Question.insertMultiple(questions) }
            val aDeferred = async { Answer.insertMultiple(allAnswers) }
            val dDeferred = async { Discussion.insertMultiple(allDiscussions) }

            Triple(qDeferred.await(), aDeferred.await(), dDeferred.await())
        }

        // 5. BLOCKING JDBC I/O: Database truncate and write operations -> Dispatchers.IO
        withContext(Dispatchers.IO) {
            // Database.executeQueryJdbc("truncate scrape.answers;")
            // Database.executeQueryJdbc("truncate scrape.discussions;")

            Database.executeQueryJdbc(insertQuestionsSql)
            Database.executeQueryJdbc(insertAnswersSql)
            Database.executeQueryJdbc(insertDiscussionsSql)
        }
    }

    @Throws(SQLException::class, IOException::class)
    @JvmStatic
     fun main(args: Array<String> = arrayOf("1z0-071")) {

         if(args.size > 0 ) {
             val startInstant = Instant.now()
             // multipleDocuments();                             // 6 seconds    (Single Threaded)
             // multipleDocumentsMultiThread() // 2 seconds    (Multi Threaded)    (6/2) = 3x speed up

             runBlocking {
                 multipleDocumentsCoroutine() // 1 seconds     (Multi Threaded / Coroutine)    (6/1) = 6x speed up
             }

             //singleDocument("document8.html");
             val endInstant = Instant.now()
             val duration = Duration.between(startInstant, endInstant)
             println("Execution time: " + duration.toMillis() + " ms")
             println("Formatted: " + duration.toSeconds() + " seconds")
         } else {
             println("Usage: java -jar /Users/jonathankee/examTopicScraper/static_page/KotlinDocument/build/libs/KotlinDocument-1.0-SNAPSHOT-all.jar \"1z0-071\"")
         }
    }
}