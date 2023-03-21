package cn.nap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
            Connection conn = DriverManager.getConnection(url, user, pass);
            return null != conn;
        } catch (SQLException ignore) {

        }
        return false;
    }

    public static Process start(String path) throws Exception {
        return Runtime.getRuntime().exec(path + File.separator + "bin" + File.separator + "mysqld.exe");
    }

    public static boolean isInit() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        String url = "jdbc:mysql://localhost:3306/test";
        String user = "root";
        String pass = "root";

        Connection conn;
        try {
            conn = DriverManager.getConnection(url, user, pass);
        } catch (SQLException ignore) {
            return false;
        }
        if (null == conn) {
            return false;
        }
        PreparedStatement stmt = null;
        ResultSet resultSet = null;
        try {
            stmt = conn.prepareStatement("select * from init_status;");
            resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                return 1 == resultSet.getInt(1);
            }
        } catch (SQLException e) {
            return false;
        } finally {
            if (null != resultSet) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (null != stmt) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void init(String path) {
        File file = new File(path + File.separator + "sql");
        if (!file.exists() || !file.isDirectory()) {
            return;
        }
        File[] sqlFiles = file.listFiles();
        if (null == sqlFiles || sqlFiles.length == 0) {
            return;
        }
        for (File sqlFile : sqlFiles) {
            FileReader fileReader = null;
            BufferedReader reader = null;
            try {
                fileReader = new FileReader(sqlFile);
                reader = new BufferedReader(fileReader);

                String line;
                StringBuilder builder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if ("".equals(line.trim())) {
                        continue;
                    }

                    if (builder.length() > 0) {
                        builder.append(" ");
                    }
                    builder.append(line.trim());

                    if (line.endsWith(";")) {
                        update(builder.toString());
                        builder.delete(0, builder.length());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != fileReader) {
                    try {
                        fileReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (null != reader) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static int update(String sql) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
        String url = "jdbc:mysql://localhost:3306/mysql";
        String user = "root";
        String pass = "root";

        Connection conn;
        try {
            conn = DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            return -1;
        }
        if (null == conn) {
            return -1;
        }
        PreparedStatement stmt = null;
        try {
            System.out.println("sql: " + sql);
            stmt = conn.prepareStatement(sql);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (null != stmt) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static Process stop(String path) throws Exception {
        return Runtime.getRuntime().exec(path + File.separator + "bin" + File.separator + "mysqladmin.exe -uroot -proot shutdown");
    }
}
