package com.jonathankee.database;

import com.jonathankee.schema.Tuple;
import org.postgresql.util.PSQLException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {

    public static void testJdbc() throws SQLException {
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

    public static void executeQueryJdbc(String sql) throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/postgres";
        try (Connection conn = DriverManager.getConnection(url, "postgres", "abc123");
             ResultSet rs = conn.createStatement().executeQuery(sql)) {

        } catch (PSQLException e) {
            e.printStackTrace();
            // Do nothing with no result because inserting data
        }
    }

    public static List<Tuple> executeQueryJdbcResult(String sql, int... columnToGet) throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/postgres";
        List<Tuple> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, "postgres", "abc123")) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Tuple tuple = new Tuple();
                        for (int i = 0; i < columnToGet.length; i++) {
                            if (columnToGet[i] == 1) {
                                tuple.setFileName("document" + rs.getString(columnToGet[i]) + ".html");
                            }
                            if (columnToGet[i] == 2) {
                                tuple.setUrl(rs.getString(columnToGet[i]));
                            }
                        }
                        list.add(tuple);
                    }
                }
            }
        }
        return list;
    }

    public static List<Tuple> executeQueryJdbcResultImage(String sql, int... columnToGet) throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/postgres";
        List<Tuple> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, "postgres", "abc123")) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Tuple tuple = new Tuple();
                        for (int i = 0; i < columnToGet.length; i++) {
                            if (columnToGet[i] == 1) {
                                tuple.setUrl(rs.getString(columnToGet[i]));
                                tuple.setFileName(null);
                            }
                        }
                        list.add(tuple);
                    }
                }
            }
        }
        return list;
    }
}
