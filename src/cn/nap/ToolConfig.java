package cn.nap;

import java.util.List;

public class ToolConfig {
    public Core core;
    public List<Instance> instances;

    public static class Info<T> {
        public T data;
        public int sort;
        public String comment;

        public Info() {
        }

        public Info(T data, int sort, String comment) {
            this.data = data;
            this.sort = sort;
            this.comment = comment;
        }
    }

    public static class Core {
        public Info<String> section;
        public Info<String> language;
        public Info<String> theme;

        public Core() {
        }

        public Core(Info<String> section, Info<String> language, Info<String> theme) {
            this.section = section;
            this.language = language;
            this.theme = theme;
        }
    }

    public static class Instance {
        public Info<String> section;
        public Info<String> path;
        public Info<String> username;
        public Info<String> password;
        public Info<String> port;
        public Info<String> database;

        public Instance() {
        }

        public Instance(Info<String> section, Info<String> path, Info<String> username, Info<String> password, Info<String> port, Info<String> database) {
            this.section = section;
            this.path = path;
            this.username = username;
            this.password = password;
            this.port = port;
            this.database = database;
        }
    }
}
