package me.itut.lanitium.internal.carpet;

import carpet.script.Token;

import java.util.List;

public record InterpolatedString(List<String> substrings, List<List<Token>> interpolations, StringBuilder next) {
    /*
        'My name is \(name) and I am \(age) years old'

        <STRING>    'My name is '
        <MARKER>    I{
        <VARIABLE>  name
        <STRING>    ' and I am '
        <MARKER>    I;
        <VARIABLE>  age
        <STRING>    ' years old'
        <MARKER>    I}

        sum('My name is ', name, ' and I am ', age, ' years old')
    */

    public static final char MARKER = 'I';
    public static final String FIRST = MARKER + "{";
    public static final String NEXT = MARKER + ";";
    public static final String END = MARKER + "}";

    public void append(InterpolatedString str) {
        for (int i = 0, size = str.substrings.size(); i < size; i++) {
            next.append(str.substrings.get(i));
            nextInterpolation(str.interpolations.get(i));
        }
        next.append(str.next);
    }

    public boolean dedent(String indent) {
        int length = indent.length();

        if (substrings.isEmpty()) {
            if (next.toString().startsWith(indent)) next.delete(0, length);
            else if (indent.startsWith(next.toString())) next.setLength(0);
            else return false;
            return true;
        }

        String first = substrings.getFirst();
        if (first.startsWith(indent)) substrings.set(0, first.substring(length));
        else if (indent.startsWith(first)) substrings.set(0, "");
        else return false;
        return true;
    }

    public void nextInterpolation(List<Token> interpolation) {
        substrings.add(next.toString());
        interpolations.add(interpolation);
        next.setLength(0);
    }
}
