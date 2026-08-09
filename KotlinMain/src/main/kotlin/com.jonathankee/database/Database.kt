package com.jonathankee.database

import com.jonathankee.schema.Tuple
import org.postgresql.util.PSQLException
import java.sql.DriverManager
import java.sql.SQLException

object Database {
    @Throws(SQLException::class)
    fun testJdbc() {
        val url = "jdbc:postgresql://localhost:5432/postgres"
        DriverManager.getConnection(url, "postgres", "abc123").use { conn ->
            conn.prepareStatement(
                "SELECT * FROM questions"
            ).use { ps ->
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        println(rs.getString(1))
                        println(rs.getString(2))
                        println(rs.getString(3))
                    }
                }
            }
        }
    }

    @Throws(SQLException::class)
    fun executeQueryJdbc(sql: String?) {
        val url = "jdbc:postgresql://localhost:5432/exam_topic"
        try {
            DriverManager.getConnection(url, "postgres", "abc123")
                .use { conn -> conn.createStatement().executeQuery(sql).use { rs -> } }
        } catch (e: PSQLException) {
            e.printStackTrace()
            // Do nothing with no result because inserting data
        }
    }

    @Throws(SQLException::class)
    fun executeQueryJdbcResult(sql: String?, vararg columnToGet: Int): List<Tuple> {
        val url = "jdbc:postgresql://localhost:5432/exam_topic"
        val list: MutableList<Tuple> = ArrayList()
        DriverManager.getConnection(url, "postgres", "abc123").use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val tuple = Tuple()
                        for (i in columnToGet.indices) {
                            if (columnToGet[i] == 1) {
                                tuple.setFileName("document" + rs.getString(columnToGet[i]) + ".html")
                            }
                            if (columnToGet[i] == 2) {
                                tuple.setUrl(rs.getString(columnToGet[i]))
                            }
                        }
                        list.add(tuple)
                    }
                }
            }
        }
        return list
    }

    @Throws(SQLException::class)
    fun executeQueryJdbcResultImage(sql: String?, vararg columnToGet: Int): List<Tuple> {
        val url = "jdbc:postgresql://localhost:5432/exam_topic"
        val list: MutableList<Tuple> = ArrayList()
        DriverManager.getConnection(url, "postgres", "abc123").use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val tuple = Tuple()
                        for (i in columnToGet.indices) {
                            if (columnToGet[i] == 1) {
                                tuple.setUrl(rs.getString(columnToGet[i]))
                                tuple.setFileName(null)
                            }
                        }
                        list.add(tuple)
                    }
                }
            }
        }
        return list
    }
}