package com.replacer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type RULE_LIST_TYPE = new TypeToken<List<RuleData>>() {}.getType();

    public static class TypeData {
        public String type;
        public String match;
        public String replace;

        public TypeData(String type, String match, String replace) {
            this.type = type;
            this.match = match;
            this.replace = replace;
        }
    }

    public static class RuleData {
        public String name;
        public List<TypeData> typeRows;

        public RuleData(String name, List<TypeData> typeRows) {
            this.name = name;
            this.typeRows = typeRows;
        }
    }

    public static String exportRules(List<ReplacerTab.RulePanel> rulePanels) {
        List<RuleData> rules = new ArrayList<>();
        for (ReplacerTab.RulePanel panel : rulePanels) {
            List<TypeData> typeRows = new ArrayList<>();
            for (ReplacerTab.TypeRow row : panel.getTypeRows()) {
                typeRows.add(new TypeData(row.getType(), row.getMatch(), row.getReplace()));
            }
            rules.add(new RuleData(panel.getName(), typeRows));
        }
        return GSON.toJson(rules);
    }

    public static List<RuleData> parseRules(String json) {
        List<RuleData> rules = GSON.fromJson(json, RULE_LIST_TYPE);
        return rules != null ? rules : new ArrayList<>();
    }
}
