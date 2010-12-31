<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.DriverManager" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>
<html>
<body>
<h2>Hello World! From
    <%
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        Connection conn = DriverManager.getConnection("jdbc:mysql://mysqlDB/webapp?user=web&password=pwd");
        Statement stmt = conn.createStatement();
        java.sql.ResultSet resultSet = stmt.executeQuery("SELECT NAME FROM PERSON");
        while (resultSet.next()) {
            out.print(resultSet.getString("NAME") + " ");
        }
    %>
</h2>
</body>
</html>
