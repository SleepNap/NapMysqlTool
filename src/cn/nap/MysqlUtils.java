package cn.nap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MysqlUtils {

    public static Map<String, Map<String, String>> readIniFile(String file) {
        String line;
        String currSection = null;
        Map<String, Map<String, String>> iniMap = new HashMap<>();
        Map<String, String> sectionMap = null;

        try (FileReader fr = new FileReader(file);
             BufferedReader reader = new BufferedReader(fr)) {
            while (null != (line = reader.readLine())) {
                line = line.trim();
                // 空白行跳过
                if (line.isEmpty()) {
                    continue;
                }
                // 注释跳过
                if (line.startsWith(";")) {
                    continue;
                }
                // 获取当前节点
                if (line.startsWith("[") && line.endsWith("]")) {
                    currSection = line.substring(0, line.length() - 1).substring(1);
                    continue;
                }
                // 如果当前行还没到第一个节点，继续下一行
                if (null == currSection) {
                    continue;
                }
                // 如果开始了一个新的节点，就重新new一个keyMap，不然会将之前的值给覆盖
                if (!iniMap.containsKey(currSection)) {
                    sectionMap = new HashMap<>();
                    iniMap.put(currSection, sectionMap);
                }
                // 将值赋值到sectionMap中
                String[] split = line.split("=", 2);
                if (split.length <= 1) {
                    continue;
                }
                sectionMap.put(split[0].trim(), split[1].trim());
            }
            return iniMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void writeIniFile(Map<String, Map<String, String>> iniMap, String file) {
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter writer = new BufferedWriter(fw)) {
            for (Map.Entry<String, Map<String, String>> sectionEntry : iniMap.entrySet()) {
                writer.write("[" + sectionEntry.getKey() + "]");
                writer.newLine();
                for (Map.Entry<String, String> valueEntry : sectionEntry.getValue().entrySet()) {
                    writer.write(valueEntry.getKey() + "=" + valueEntry.getValue());
                    writer.newLine();
                }
                writer.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> readFinishedList(String file) {
        List<String> finishedList = new ArrayList<>();
        try (FileReader fr = new FileReader(file);
             BufferedReader reader = new BufferedReader(fr)) {
            String line;
            while (null != (line = reader.readLine())) {
                line = line.trim();
                // 空白行跳过
                if (line.isEmpty()) {
                    continue;
                }
                finishedList.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return finishedList;
    }

    public static void appendFinishedList(String file, String finished) {
        try (FileWriter fw = new FileWriter(file, true);
             BufferedWriter writer = new BufferedWriter(fw)) {
            writer.write(finished);
            writer.newLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
