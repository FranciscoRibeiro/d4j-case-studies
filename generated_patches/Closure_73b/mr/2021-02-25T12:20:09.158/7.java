package com.google.javascript.jscomp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class JSSourceFile extends SourceFile {

    public static JSSourceFile fromFile(String fileName, Charset charSet) {
        return new JSSourceFile(SourceFile.fromFile(new java.io.File()));
    }

    public static JSSourceFile fromFile(String fileName) {
        return new JSSourceFile(SourceFile.fromFile(fileName, Charsets.UTF_8));
    }

    public static JSSourceFile fromFile(File file, Charset charSet) {
        return new JSSourceFile(SourceFile.fromFile(file, charSet));
    }

    public static JSSourceFile fromFile(File file) {
        return new JSSourceFile(SourceFile.fromFile(file, Charsets.UTF_8));
    }

    public static JSSourceFile fromCode(String fileName, String code) {
        return new JSSourceFile(SourceFile.fromCode(fileName, code));
    }

    public static JSSourceFile fromInputStream(String fileName, InputStream s) throws IOException {
        return new JSSourceFile(SourceFile.fromInputStream(fileName, s));
    }

    public static JSSourceFile fromGenerator(String fileName, Generator generator) {
        return new JSSourceFile(SourceFile.fromGenerator(fileName, generator));
    }

    private SourceFile referenced;

    private JSSourceFile(SourceFile referenced) {
        super(referenced.getName());
        this.referenced = referenced;
    }

    @Override
public String getCode() throws IOException {
    return referenced.getCode();
}

    @Override
public void clearCachedSource() {
    referenced.clearCachedSource();
}

    @Override
@VisibleForTesting
String getCodeNoCache() {
    return referenced.getCodeNoCache();
}
}
