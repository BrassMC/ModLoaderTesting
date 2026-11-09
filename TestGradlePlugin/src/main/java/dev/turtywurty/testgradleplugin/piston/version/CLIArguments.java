package dev.turtywurty.testgradleplugin.piston.version;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record CLIArguments(List<Argument> arguments) {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    public CLIArguments {
        arguments = List.copyOf(arguments);
    }

    public static CLIArguments fromJsonArray(JsonArray array) {
        List<Argument> arguments = new ArrayList<>();
        if (array != null) {
            for (JsonElement element : array) {
                if (element.isJsonPrimitive()) {
                    arguments.add(new Argument(List.of(element.getAsString()), null));
                } else {
                    JsonObject object = element.getAsJsonObject();
                    JsonElement value = object.get("value");
                    List<String> tokens = new ArrayList<>();
                    if (value.isJsonArray()) {
                        value.getAsJsonArray().forEach(token -> tokens.add(token.getAsString()));
                    } else {
                        tokens.add(value.getAsString());
                    }

                    arguments.add(new Argument(List.copyOf(tokens), object.getAsJsonArray("rules")));
                }
            }
        }

        return new CLIArguments(arguments);
    }

    public List<String> resolve(Map<String, String> substitutions) {
        List<String> tokens = new ArrayList<>();
        for (Argument argument : arguments) {
            if (LaunchRules.allows(argument.rules())) {
                for (String token : argument.tokens()) {
                    tokens.add(substitute(token, substitutions));
                }
            }
        }

        return tokens;
    }

    public static String substitute(String token, Map<String, String> substitutions) {
        Matcher matcher = PLACEHOLDER.matcher(token);
        var result = new StringBuilder();
        while (matcher.find()) {
            String replacement = substitutions.get(matcher.group(1));
            if (replacement == null)
                throw new IllegalArgumentException("Unknown launcher placeholder: " + matcher.group());

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    public record Argument(List<String> tokens, JsonArray rules) {
    }
}
