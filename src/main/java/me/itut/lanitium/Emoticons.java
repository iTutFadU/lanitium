package me.itut.lanitium;

import com.google.common.collect.ImmutableList;

import java.util.concurrent.ThreadLocalRandom;

public abstract class Emoticons {
    public static final ImmutableList<String> list = ImmutableList.of(
        ":)", ":D", ":3", ":>", ":P", ";)", ";D", ";3", ";P", ":')", "C:",
        "^^", "^-^", "^_^", "^.^", "^u^", "^v^", "^w^", "^x^",
        ">u<", ">v<", ">w<", "'u'", "'v'", "'w'", "8(>U<)8", "^)-(^"
        // More? :D
    );

    private Emoticons() {}

    public static String getRandomEmoticon() {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
}
