package me.itut.lanitium.internal.carpet;

/*
    value := bool | int | str | list | map

    bool := 'false' | 'true'
    int := ('-' | '+')? digit+
    str := ''' char* '''
    list := '[' %% ( value %% (',' %% value %%)* (',' %%)? )? ']'
    map := '{' %% ( value %% '->' %% value %% (',' %% value %% '->' %% value %%)* (',' %%)? )? '}'

    digit := '0'..='9'
    char := ' '..='\uFFFF' - ''' - '\x7F'..='\x9F'
*/

public sealed interface LanitiumConfig {
    Str VERSION = new Str("version");

    enum Bool implements LanitiumConfig { FALSE, TRUE }
    record Int(long value) implements LanitiumConfig {}
    record Str(String value) implements LanitiumConfig {}
    record List(java.util.List<LanitiumConfig> values) implements LanitiumConfig {}
    record Map(java.util.Map<LanitiumConfig, LanitiumConfig> entries) implements LanitiumConfig {}

    sealed interface Version extends Comparable<Version> {
        record Major(int number) implements Version {}

        final class Nightly implements Version {
            public static final Nightly NIGHTLY = new Nightly();

            private Nightly() {}
        }

        @Override
        default int compareTo(Version other) {
            if (!(this instanceof Major(int a))) return other == Nightly.NIGHTLY ? 0 : 1;
            if (!(other instanceof Major(int b))) return -1;
            return Integer.compare(a, b);
        }
    }

    sealed interface Structured {
        Version version();

        record Nightly() implements Structured {
            @Override
            public Version version() {
                return Version.Nightly.NIGHTLY;
            }
        }
    }
}
