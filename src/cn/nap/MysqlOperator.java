package cn.nap;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MysqlOperator {
    public static boolean isStarted() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        String url = "jdbc:mysql://localhost:3306/mysql";
        String user = "root";
        String pass = "root";
        try {
            DriverManager.setLoginTimeout(2);
            Connection conn = DriverManager.getConnection(url, user, pass);
            return null != conn;
        } catch (SQLException ignore) {

        }
        return false;
    }

    public static Process start(String path) throws Exception {
        return Runtime.getRuntime().exec(path + File.separator + "bin" + File.separator + "mysqld.exe");
    }

    public static Process stop(String path) throws Exception {
        return Runtime.getRuntime().exec(path + File.separator + "bin" + File.separator + "mysqladmin.exe -uroot -proot shutdown");
    }
}
