package cn.nap;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

public class MysqlOperator {
    public static boolean isStarted(Map<String, Map<String, String>> iniProp) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        String url = "jdbc:mysql://localhost:3306/mysql";
        String user = iniProp.get("mysql配置").get("mysql账号");
        String pass = iniProp.get("mysql配置").get("mysql密码");
        try {
            DriverManager.setLoginTimeout(2);
            Connection conn = DriverManager.getConnection(url, user, pass);
            return null != conn;
        } catch (SQLException ignore) {

        }
        return false;
    }

    public static Process start(Map<String, Map<String, String>> iniProp) throws Exception {
        String path = iniProp.get("mysql配置").get("mysql路径");
        String mysqlIni = Optional.ofNullable(iniProp.get("mysql配置").get("mysql.ini路径")).orElse("");
        return Runtime.getRuntime().exec(path + File.separator + "bin" + File.separator + "mysqld.exe" + (mysqlIni.isEmpty() ? "" : " --defaults-file=" + mysqlIni));
    }

    public static Process stop(Map<String, Map<String, String>> iniProp) throws Exception {
        String path = iniProp.get("mysql配置").get("mysql路径");
        String user = iniProp.get("mysql配置").get("mysql账号");
        String pass = iniProp.get("mysql配置").get("mysql密码");
        return Runtime.getRuntime().exec(path + File.separator + "bin" + File.separator + "mysqladmin.exe -u" + user + " -p" + pass + " shutdown");
    }

    public static Process initSql(Map<String, Map<String, String>> iniProp, String db, String file) throws Exception {
        String path = iniProp.get("mysql配置").get("mysql路径");
        String user = iniProp.get("mysql配置").get("mysql账号");
        String pass = iniProp.get("mysql配置").get("mysql密码");
        return Runtime.getRuntime().exec("cmd.exe /C " + path + File.separator + "bin" + File.separator + "mysql.exe -u" + user + " -p" + pass + " " + db + " < " + file);
    }

    public static void fix(Map<String, Map<String, String>> iniProp) {
        String path = iniProp.get("mysql配置").get("mysql路径");
        File file = new File(path + File.separator + "data");
        if (!file.exists() || !file.isDirectory()) {
            return;
        }
        File[] files = file.listFiles();
        if (files == null) {
            return;
        }
        for (File childFile : files) {
            if ("binlog.index".equals(childFile.getName())) {
                MysqlUtils.clearFile(childFile);
            } else if (childFile.getName().startsWith("binlog")) {
                boolean ignore = childFile.delete();
            }
        }
    }

    public static Process outDb(Map<String, Map<String, String>> iniProp) throws Exception {
        String path = iniProp.get("mysql配置").get("mysql路径");
        String user = iniProp.get("mysql配置").get("mysql账号");
        String pass = iniProp.get("mysql配置").get("mysql密码");
        String dbs = iniProp.get("工具配置").get("导出的库名，多个用空格分割");
        return Runtime.getRuntime().exec("cmd.exe /C " + path + File.separator + "bin" + File.separator + "mysqldump.exe -u" + user + " -p" + pass + " --databases " + dbs + " > output.sql");
    }
}
