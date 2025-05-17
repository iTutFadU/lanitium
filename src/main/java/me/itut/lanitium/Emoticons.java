package me.itut.lanitium;

import com.google.common.collect.ImmutableList;

import java.util.concurrent.ThreadLocalRandom;

public class Emoticons {
    public static final ImmutableList<String> list = ImmutableList.of(
        ":)", ":D", ":3", ":>", ":P", ";)", ";D", ";3", ";P", "C:", ":3c",
        "^^", "^-^", "^.^", "^u^", "^w^", ">u<", ">w<", "'u'", "'w'", "OuO", "OwO",
        "8(>U<)8", "^)-(^", "(+_+)", "\\(^-^)/", "UwU"
        // More? :D
    );

    public static String getRandomEmoticon() {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
}
