package com.google.javascript.jscomp;

import static com.google.javascript.jscomp.SourceExcerptProvider.SourceExcerpt.LINE;
import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.SourceExcerptProvider.ExcerptFormatter;
import com.google.javascript.jscomp.SourceExcerptProvider.SourceExcerpt;

public class LightweightMessageFormatter extends AbstractMessageFormatter {

    private SourceExcerpt excerpt;

    private static final ExcerptFormatter excerptFormatter = new LineNumberingFormatter();

    private LightweightMessageFormatter() {
        super(null);
        this.excerpt = LINE;
    }

    public LightweightMessageFormatter(SourceExcerptProvider source) {
        this(source, LINE);
    }

    public LightweightMessageFormatter(SourceExcerptProvider source, SourceExcerpt excerpt) {
        super(source);
        Preconditions.checkNotNull(source);
        this.excerpt = excerpt;
    }

    static LightweightMessageFormatter withoutSource() {
        return new LightweightMessageFormatter();
    }

    public String formatError(JSError error) {
        return format(error, false);
    }

    public String formatWarning(JSError warning) {
        return format(warning, true);
    }

    private String format(JSError error, boolean warning) {
        // extract source excerpt
SourceExcerptProvider source = getSource();
        String sourceExcerpt = source == null ? null : excerpt.get(source, error.sourceName, error.lineNumber, excerptFormatter);
        // formatting the message
StringBuilder b = new StringBuilder();
        if (error.sourceName != null) {
            b.append(error.sourceName);
            if (error.lineNumber > 0) {
                b.append(':');
                b.append(error.lineNumber);
            }
            b.append(": ");
        }
        b.append(getLevelName(warning ? CheckLevel.WARNING : CheckLevel.ERROR));
        b.append(" - ");
        b.append(error.description);
        b.append('\n');
        if (sourceExcerpt != null) {
            b.append(sourceExcerpt);
            b.append('\n');
            int charno = error.getCharno();
            if (excerpt.equals(LINE) && 0 <= charno && charno < sourceExcerpt.length()) {
                for (int i = 0; i <= charno; i++) {
                    char c = sourceExcerpt.charAt(i);
                    if (Character.isWhitespace(c)) {
                        b.append(c);
                    } else {
                        b.append(' ');
                    }
                }
                b.append("^\n");
            }
        }
        return b.toString();
    }

    static class LineNumberingFormatter implements ExcerptFormatter {

        public String formatLine(String line, int lineNumber) {
            return line;
        }

        public String formatRegion(Region region) {
            if (region == null) {
                return null;
            }
            String code = region.getSourceExcerpt();
            if (code.length() == 0) {
                return null;
            }
            // max length of the number display
int numberLength = Integer.toString(region.getEndingLineNumber()).length();
            // formatting
StringBuilder builder = new StringBuilder(code.length() * 2);
            int start = 0;
            int end = code.indexOf('\n', start);
            int lineNumber = region.getBeginningLineNumber();
            while (start >= 0) {
                // line extraction
String line;
                if (end < 0) {
                    line = code.substring(start);
                    if (line.length() == 0) {
                        return builder.substring(0, builder.length() - 1);
                    }
                } else {
                    line = code.substring(start, end);
                }
                builder.append("  ");
                // nice spaces for the line number
int spaces = numberLength - Integer.toString(lineNumber).length();
                builder.append(Strings.repeat(" ", spaces));
                builder.append(lineNumber);
                builder.append("| ");
                if (end < 0) {
                    builder.append(line);
                    start = -1;
                } else {
                    builder.append(line);
                    builder.append('\n');
                    start = end + 1;
                    end = code.indexOf('\n', start);
                    lineNumber++;
                }
            }
            return builder.toString();
        }
    }
}
