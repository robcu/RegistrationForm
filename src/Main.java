import jodd.json.JsonParser;
import jodd.json.JsonSerializer;
import org.h2.tools.Server;
import spark.Spark;

import java.sql.*;
import java.util.ArrayList;

public class Main {

    public static void createUserTable(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users(id IDENTITY, username VARCHAR, address VARCHAR, email VARCHAR);");
    }

    public static ArrayList<User> selectUsers(Connection conn) throws SQLException {
        ArrayList<User> users = new ArrayList<>();
        User user = new User();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users;");
        ResultSet results = stmt.executeQuery();
        while (results.next()) {
            Integer id = results.getInt("id");
            String name = results.getString("username");
            String address = results.getString("address");
            String email = results.getString("email");
            user = new User(id, name, address, email);
            users.add(user);
        }
        return users;
    }

    public static void insertUser(Connection conn, String name, String address, String email) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES(NULL, ?, ?, ?);");
        stmt.setString(1, name);
        stmt.setString(2, address);
        stmt.setString(3, email);
        stmt.execute();
    }

    public static void updateUser(Connection conn, String name, String address, String email, Integer userId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE users SET username=?, address=?, email=? WHERE id=?;");
        stmt.setString(1, name);
        stmt.setString(2, address);
        stmt.setString(3, email);
        stmt.setInt(4, userId);
        stmt.execute();
    }

    public static void deleteUser(Connection conn, Integer id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE id=?;");
        stmt.setInt(1, id);
        stmt.execute();
    }

    public static void main(String[] args) throws SQLException {

        Spark.externalStaticFileLocation("public");
        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createUserTable(conn);

        Spark.init();

        Spark.get("/user", (request, response) -> {
            ArrayList<User> users = selectUsers(conn);
            JsonSerializer s = new JsonSerializer();
            return s.serialize(users);
        });

        Spark.post("/user", (request, response) -> {
            String body = request.body();
            JsonParser p = new JsonParser();
            User user = p.parse(body, User.class);
            insertUser(conn, user.username, user.address, user.email);
            return "";
        });

        Spark.put("/user", (request, response) -> {
            String body = request.body();
            JsonParser p = new JsonParser();
            User user = p.parse(body, User.class);
            updateUser(conn, user.username, user.address, user.email, user.id);
            return "";
        });

        Spark.delete("/user/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            deleteUser(conn, id);
           return "";
        });


    }
}
