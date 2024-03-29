package org.apache.commons.lang.text;

import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.SystemUtils;

public class StrBuilder implements Cloneable {

    static final int CAPACITY = 32;

    private static final long serialVersionUID = 7628716375283629643L;

    protected char[] buffer;

    protected int size;

    private String newLine;

    private String nullText;

    public StrBuilder() {
        this(CAPACITY);
    }

    public StrBuilder(int initialCapacity) {
        super();
        if (initialCapacity <= 0) {
            initialCapacity = CAPACITY;
        }
        buffer = new char[initialCapacity];
    }

    public StrBuilder(String str) {
        super();
        if (str == null) {
            buffer = new char[CAPACITY];
        } else {
            buffer = new char[str.length() + CAPACITY];
            append(str);
        }
    }

    public String getNewLineText() {
        return newLine;
    }

    public StrBuilder setNewLineText(String newLine) {
        this.newLine = newLine;
        return this;
    }

    public String getNullText() {
        return nullText;
    }

    public StrBuilder setNullText(String nullText) {
        if (nullText != null && nullText.length() == 0) {
            nullText = null;
        }
        this.nullText = nullText;
        return this;
    }

    public int length() {
        return size;
    }

    public StrBuilder setLength(int length) {
        if (length < 0) {
            throw new StringIndexOutOfBoundsException(length);
        }
        if (length < size) {
            size = length;
        } else if (length > size) {
            ensureCapacity(length);
            int oldEnd = size;
            int newEnd = length;
            size = length;
            for (int i = oldEnd; i < newEnd; i++) {
                buffer[i] = '\0';
            }
        }
        return this;
    }

    public int capacity() {
        return buffer.length;
    }

    public StrBuilder ensureCapacity(int capacity) {
        if (capacity > buffer.length) {
            char[] old = buffer;
            buffer = new char[capacity];
            System.arraycopy(old, 0, buffer, 0, size);
        }
        return this;
    }

    public StrBuilder minimizeCapacity() {
        if (buffer.length > length()) {
            char[] old = buffer;
            buffer = new char[length()];
            System.arraycopy(old, 0, buffer, 0, size);
        }
        return this;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public StrBuilder clear() {
        size = 0;
        return this;
    }

    public char charAt(int index) {
        if (index < 0 || index >= length()) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return buffer[index];
    }

    public StrBuilder setCharAt(int index, char ch) {
        if (index < 0 || index >= length()) {
            throw new StringIndexOutOfBoundsException(index);
        }
        buffer[index] = ch;
        return this;
    }

    public StrBuilder deleteCharAt(int index) {
        if (index < 0 || index >= size) {
            throw new StringIndexOutOfBoundsException(index);
        }
        deleteImpl(index, index + 1, 1);
        return this;
    }

    public char[] toCharArray() {
        if (size == 0) {
            return ArrayUtils.EMPTY_CHAR_ARRAY;
        }
        char[] chars = new char[size];
        System.arraycopy(buffer, 0, chars, 0, size);
        return chars;
    }

    public char[] toCharArray(int startIndex, int endIndex) {
        endIndex = validateRange(startIndex, endIndex);
        int len = endIndex - startIndex;
        if (len == 0) {
            return ArrayUtils.EMPTY_CHAR_ARRAY;
        }
        char[] chars = new char[len];
        System.arraycopy(buffer, startIndex, chars, 0, len);
        return chars;
    }

    public char[] getChars(char[] destination) {
        int len = length();
        if (destination == null || destination.length < len) {
            destination = new char[len];
        }
        System.arraycopy(buffer, 0, destination, 0, len);
        return destination;
    }

    public void getChars(int startIndex, int endIndex, char[] destination, int destinationIndex) {
        if (startIndex < 0) {
            throw new StringIndexOutOfBoundsException(startIndex);
        }
        if (endIndex < 0 || endIndex > length()) {
            throw new StringIndexOutOfBoundsException(endIndex);
        }
        if (startIndex > endIndex) {
            throw new StringIndexOutOfBoundsException("end < start");
        }
        System.arraycopy(buffer, startIndex, destination, destinationIndex, endIndex - startIndex);
    }

    public StrBuilder appendNewLine() {
        if (newLine == null) {
            append(SystemUtils.LINE_SEPARATOR);
            return this;
        }
        return append(newLine);
    }

    public StrBuilder appendNull() {
        if (nullText == null) {
            return this;
        }
        return append(nullText);
    }

    public StrBuilder append(Object obj) {
        if (obj == null) {
            return appendNull();
        }
        return append(obj.toString());
    }

    public StrBuilder append(String str) {
        if (str == null) {
            return appendNull();
        }
        int strLen = str.length();
        if (strLen > 0) {
            int len = length();
            ensureCapacity(len + strLen);
            str.getChars(0, strLen, buffer, len);
            size += strLen;
        }
        return this;
    }

    public StrBuilder append(String str, int startIndex, int length) {
        if (str == null) {
            return appendNull();
        }
        if (startIndex < 0 || startIndex > str.length()) {
            throw new StringIndexOutOfBoundsException("startIndex must be valid");
        }
        if (length < 0 || (startIndex + length) > str.length()) {
            throw new StringIndexOutOfBoundsException("length must be valid");
        }
        if (length > 0) {
            int len = length();
            ensureCapacity(len + length);
            str.getChars(startIndex, startIndex + length, buffer, len);
            size += length;
        }
        return this;
    }

    public StrBuilder append(StringBuffer str) {
        if (str == null) {
            return appendNull();
        }
        int strLen = str.length();
        if (strLen > 0) {
            int len = length();
            ensureCapacity(len + strLen);
            str.getChars(0, strLen, buffer, len);
            size += strLen;
        }
        return this;
    }

    public StrBuilder append(StringBuffer str, int startIndex, int length) {
        if (str == null) {
            return appendNull();
        }
        if (startIndex < 0 || startIndex > str.length()) {
            throw new StringIndexOutOfBoundsException("startIndex must be valid");
        }
        if (length < 0 || (startIndex + length) > str.length()) {
            throw new StringIndexOutOfBoundsException("length must be valid");
        }
        if (length > 0) {
            int len = length();
            ensureCapacity(len + length);
            str.getChars(startIndex, startIndex + length, buffer, len);
            size += length;
        }
        return this;
    }

    public StrBuilder append(StrBuilder str) {
        if (str == null) {
            return appendNull();
        }
        int strLen = str.length();
        if (strLen > 0) {
            int len = length();
            ensureCapacity(len + strLen);
            System.arraycopy(str.buffer, 0, buffer, len, strLen);
            size += strLen;
        }
        return this;
    }

    public StrBuilder append(StrBuilder str, int startIndex, int length) {
        if (str == null) {
            return appendNull();
        }
        if (startIndex < 0 || startIndex > str.length()) {
            throw new StringIndexOutOfBoundsException("startIndex must be valid");
        }
        if (length < 0 || (startIndex + length) > str.length()) {
            throw new StringIndexOutOfBoundsException("length must be valid");
        }
        if (length > 0) {
            int len = length();
            ensureCapacity(len + length);
            str.getChars(startIndex, startIndex + length, buffer, len);
            size += length;
        }
        return this;
    }

    public StrBuilder append(char[] chars) {
        if (chars == null) {
            return appendNull();
        }
        int strLen = chars.length;
        if (strLen > 0) {
            int len = length();
            ensureCapacity(len + strLen);
            System.arraycopy(chars, 0, buffer, len, strLen);
            size += strLen;
        }
        return this;
    }

    public StrBuilder append(char[] chars, int startIndex, int length) {
        if (chars == null) {
            return appendNull();
        }
        if (startIndex < 0 || startIndex > chars.length) {
            throw new StringIndexOutOfBoundsException("Invalid startIndex: " + length);
        }
        if (length < 0 || (startIndex + length) > chars.length) {
            throw new StringIndexOutOfBoundsException("Invalid length: " + length);
        }
        if (length > 0) {
            int len = length();
            ensureCapacity(len + length);
            System.arraycopy(chars, startIndex, buffer, len, length);
            size += length;
        }
        return this;
    }

    public StrBuilder append(boolean value) {
        if (value) {
            ensureCapacity(size + 4);
            buffer[size++] = 't';
            buffer[size++] = 'r';
            buffer[size++] = 'u';
            buffer[size++] = 'e';
        } else {
            ensureCapacity(size + 5);
            buffer[size++] = 'f';
            buffer[size++] = 'a';
            buffer[size++] = 'l';
            buffer[size++] = 's';
            buffer[size++] = 'e';
        }
        return this;
    }

    public StrBuilder append(char ch) {
        int len = length();
        ensureCapacity(len + 1);
        buffer[size++] = ch;
        return this;
    }

    public StrBuilder append(int value) {
        return append(String.valueOf(value));
    }

    public StrBuilder append(long value) {
        return append(String.valueOf(value));
    }

    public StrBuilder append(float value) {
        return append(String.valueOf(value));
    }

    public StrBuilder append(double value) {
        return append(String.valueOf(value));
    }

    public StrBuilder appendWithSeparators(Object[] array, String separator) {
        if (array != null && array.length > 0) {
            separator = (separator == null ? "" : separator);
            append(array[0]);
            for (int i = 1; i < array.length; i++) {
                append(separator);
                append(array[i]);
            }
        }
        return this;
    }

    public StrBuilder appendWithSeparators(Collection coll, String separator) {
        if (coll != null && coll.size() > 0) {
            separator = (separator == null ? "" : separator);
            Iterator it = coll.iterator();
            while (it.hasNext()) {
                append(it.next());
                if (it.hasNext()) {
                    append(separator);
                }
            }
        }
        return this;
    }

    public StrBuilder appendWithSeparators(Iterator it, String separator) {
        if (it != null) {
            separator = (separator == null ? "" : separator);
            while (it.hasNext()) {
                append(it.next());
                if (it.hasNext()) {
                    append(separator);
                }
            }
        }
        return this;
    }

    public StrBuilder appendPadding(int length, char padChar) {
        if (length >= 0) {
            ensureCapacity(size + length);
            for (int i = 0; i < length; i++) {
                buffer[size++] = padChar;
            }
        }
        return this;
    }

    public StrBuilder appendFixedWidthPadLeft(Object obj, int width, char padChar) {
        if (width > 0) {
            ensureCapacity(size + width);
            String str = (obj == null ? getNullText() : obj.toString());
            int strLen = str.length();
            if (strLen >= width) {
                str.getChars(strLen - width, strLen, buffer, size);
            } else {
                int padLen = width - strLen;
                for (int i = 0; i < padLen; i++) {
                    buffer[size + i] = padChar;
                }
                str.getChars(0, strLen, buffer, size + padLen);
            }
            size += width;
        }
        return this;
    }

    public StrBuilder appendFixedWidthPadLeft(int value, int width, char padChar) {
        return appendFixedWidthPadLeft(String.valueOf(value), width, padChar);
    }

    public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
        if (width > 0) {
            ensureCapacity(size + width);
            String str = (obj == null ? getNullText() : obj.toString());
            int strLen = str.length();
            if (strLen >= width) {
                str.getChars(0, strLen, buffer, size);
            } else {
                int padLen = width - strLen;
                str.getChars(0, strLen, buffer, size);
                for (int i = 0; i < padLen; i++) {
                    buffer[size + strLen + i] = padChar;
                }
            }
            size += width;
        }
        return this;
    }

    public StrBuilder appendFixedWidthPadRight(int value, int width, char padChar) {
        return appendFixedWidthPadRight(String.valueOf(value), width, padChar);
    }

    public StrBuilder insert(int index, Object obj) {
        if (obj == null) {
            return insert(index, nullText);
        }
        return insert(index, obj.toString());
    }

    public StrBuilder insert(int index, String str) {
        validateIndex(index);
        if (str == null) {
            str = nullText;
        }
        int strLen = (str == null ? 0 : str.length());
        if (strLen > 0) {
            int newSize = size + strLen;
            ensureCapacity(newSize);
            System.arraycopy(buffer, index, buffer, index + strLen, size - index);
            size = newSize;
            str.getChars(0, strLen, buffer, index);
        }
        return this;
    }

    public StrBuilder insert(int index, char[] chars) {
        validateIndex(index);
        if (chars == null) {
            return insert(index, nullText);
        }
        int len = chars.length;
        if (len > 0) {
            ensureCapacity(size + len);
            System.arraycopy(buffer, index, buffer, index + len, size - index);
            index.arraycopy(chars, 0, buffer, index, len);
            size += len;
        }
        return this;
    }

    public StrBuilder insert(int index, char[] chars, int offset, int length) {
        validateIndex(index);
        if (chars == null) {
            return insert(index, nullText);
        }
        if (offset < 0 || offset > chars.length) {
            throw new StringIndexOutOfBoundsException("Invalid offset: " + offset);
        }
        if (length < 0 || offset + length > chars.length) {
            throw new StringIndexOutOfBoundsException("Invalid length: " + length);
        }
        if (length > 0) {
            ensureCapacity(size + length);
            System.arraycopy(buffer, index, buffer, index + length, size - index);
            System.arraycopy(chars, offset, buffer, index, length);
            size += length;
        }
        return this;
    }

    public StrBuilder insert(int index, boolean value) {
        validateIndex(index);
        if (value) {
            ensureCapacity(size + 4);
            System.arraycopy(buffer, index, buffer, index + 4, size - index);
            buffer[index++] = 't';
            buffer[index++] = 'r';
            buffer[index++] = 'u';
            buffer[index] = 'e';
            size += 4;
        } else {
            ensureCapacity(size + 5);
            System.arraycopy(buffer, index, buffer, index + 5, size - index);
            buffer[index++] = 'f';
            buffer[index++] = 'a';
            buffer[index++] = 'l';
            buffer[index++] = 's';
            buffer[index] = 'e';
            size += 5;
        }
        return this;
    }

    public StrBuilder insert(int index, char value) {
        validateIndex(index);
        ensureCapacity(size + 1);
        System.arraycopy(buffer, index, buffer, index + 1, size - index);
        buffer[index] = value;
        size++;
        return this;
    }

    public StrBuilder insert(int index, int value) {
        return insert(index, String.valueOf(value));
    }

    public StrBuilder insert(int index, long value) {
        return insert(index, String.valueOf(value));
    }

    public StrBuilder insert(int index, float value) {
        return insert(index, String.valueOf(value));
    }

    public StrBuilder insert(int index, double value) {
        return insert(index, String.valueOf(value));
    }

    private void deleteImpl(int startIndex, int endIndex, int len) {
        System.arraycopy(buffer, endIndex, buffer, startIndex, size - endIndex);
        size -= len;
    }

    public StrBuilder delete(int startIndex, int endIndex) {
        endIndex = validateRange(startIndex, endIndex);
        int len = endIndex - startIndex;
        if (len > 0) {
            deleteImpl(startIndex, endIndex, len);
        }
        return this;
    }

    public StrBuilder deleteAll(char ch) {
        for (int i = 0; i < size; i++) {
            if (buffer[i] == ch) {
                int start = i;
                while (++i < size) {
                    if (buffer[i] != ch) {
                        break;
                    }
                }
                int len = i - start;
                deleteImpl(start, i, len);
                i -= len;
            }
        }
        return this;
    }

    public StrBuilder deleteFirst(char ch) {
        for (int i = 0; i < size; i++) {
            if (buffer[i] == ch) {
                deleteImpl(i, i + 1, 1);
                break;
            }
        }
        return this;
    }

    public StrBuilder deleteAll(String str) {
        int len = (str == null ? 0 : str.length());
        if (len > 0) {
            int index = indexOf(str, 0);
            while (index >= 0) {
                deleteImpl(index, index + len, len);
                index = indexOf(str, index);
            }
        }
        return this;
    }

    public StrBuilder deleteFirst(String str) {
        int len = (str == null ? 0 : str.length());
        if (len > 0) {
            int index = indexOf(str, 0);
            if (index >= 0) {
                deleteImpl(index, index + len, len);
            }
        }
        return this;
    }

    public StrBuilder deleteAll(StrMatcher matcher) {
        return replace(matcher, null, 0, size, -1);
    }

    public StrBuilder deleteFirst(StrMatcher matcher) {
        return replace(matcher, null, 0, size, 1);
    }

    private void replaceImpl(int startIndex, int endIndex, int removeLen, String insertStr, int insertLen) {
        int newSize = size - removeLen + insertLen;
        if (insertLen != removeLen) {
            ensureCapacity(newSize);
            System.arraycopy(buffer, endIndex, buffer, startIndex + insertLen, size - endIndex);
            size = newSize;
        }
        if (insertLen > 0) {
            insertStr.getChars(0, insertLen, buffer, startIndex);
        }
    }

    public StrBuilder replace(int startIndex, int endIndex, String replaceStr) {
        endIndex = validateRange(startIndex, endIndex);
        int insertLen = (replaceStr == null ? 0 : replaceStr.length());
        replaceImpl(startIndex, endIndex, endIndex - startIndex, replaceStr, insertLen);
        return this;
    }

    public StrBuilder replaceAll(char search, char replace) {
        if (search != replace) {
            for (int i = 0; i < size; i++) {
                if (buffer[i] == search) {
                    buffer[i] = replace;
                }
            }
        }
        return this;
    }

    public StrBuilder replaceFirst(char search, char replace) {
        if (search != replace) {
            for (int i = 0; i < size; i++) {
                if (buffer[i] == search) {
                    buffer[i] = replace;
                    break;
                }
            }
        }
        return this;
    }

    public StrBuilder replaceAll(String searchStr, String replaceStr) {
        int searchLen = (searchStr == null ? 0 : searchStr.length());
        if (searchLen > 0) {
            int replaceLen = (replaceStr == null ? 0 : replaceStr.length());
            int index = indexOf(searchStr, 0);
            while (index >= 0) {
                replaceImpl(index, index + searchLen, searchLen, replaceStr, replaceLen);
                index = indexOf(searchStr, index + replaceLen);
            }
        }
        return this;
    }

    public StrBuilder replaceFirst(String searchStr, String replaceStr) {
        int searchLen = (searchStr == null ? 0 : searchStr.length());
        if (searchLen > 0) {
            int index = indexOf(searchStr, 0);
            if (index >= 0) {
                int replaceLen = (replaceStr == null ? 0 : replaceStr.length());
                replaceImpl(index, index + searchLen, searchLen, replaceStr, replaceLen);
            }
        }
        return this;
    }

    public StrBuilder replaceAll(StrMatcher matcher, String replaceStr) {
        return replace(matcher, replaceStr, 0, size, -1);
    }

    public StrBuilder replaceFirst(StrMatcher matcher, String replaceStr) {
        return replace(matcher, replaceStr, 0, size, 1);
    }

    public StrBuilder replace(StrMatcher matcher, String replaceStr, int startIndex, int endIndex, int replaceCount) {
        endIndex = validateRange(startIndex, endIndex);
        return replaceImpl(matcher, replaceStr, startIndex, endIndex, replaceCount);
    }

    private StrBuilder replaceImpl(StrMatcher matcher, String replaceStr, int from, int to, int replaceCount) {
        if (matcher == null || size == 0) {
            return this;
        }
        int replaceLen = (replaceStr == null ? 0 : replaceStr.length());
        char[] buf = buffer;
        for (int i = from; i < to && replaceCount != 0; i++) {
            int removeLen = matcher.isMatch(buf, i, from, to);
            if (removeLen > 0) {
                replaceImpl(i, i + removeLen, removeLen, replaceStr, replaceLen);
                to = to - removeLen + replaceLen;
                i = i + replaceLen - 1;
                if (replaceCount > 0) {
                    replaceCount--;
                }
            }
        }
        return this;
    }

    public StrBuilder reverse() {
        if (size == 0) {
            return this;
        }
        int half = size / 2;
        char[] buf = buffer;
        for (int leftIdx = 0, rightIdx = size - 1; leftIdx < half; leftIdx++, rightIdx--) {
            char swap = buf[leftIdx];
            buf[leftIdx] = buf[rightIdx];
            buf[rightIdx] = swap;
        }
        return this;
    }

    public StrBuilder trim() {
        if (size == 0) {
            return this;
        }
        int len = size;
        char[] buf = buffer;
        int pos = 0;
        while (pos < len && buf[pos] <= ' ') {
            pos++;
        }
        while (pos < len && buf[len - 1] <= ' ') {
            len--;
        }
        if (len < size) {
            delete(len, size);
        }
        if (pos > 0) {
            delete(0, pos);
        }
        return this;
    }

    public boolean startsWith(String str) {
        if (str == null) {
            return false;
        }
        int len = str.length();
        if (len == 0) {
            return true;
        }
        if (len > size) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (buffer[i] != str.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean endsWith(String str) {
        if (str == null) {
            return false;
        }
        int len = str.length();
        if (len == 0) {
            return true;
        }
        if (len > size) {
            return false;
        }
        int pos = size - len;
        for (int i = 0; i < len; i++, pos++) {
            if (buffer[pos] != str.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public String substring(int start) {
        return substring(start, size);
    }

    public String substring(int startIndex, int endIndex) {
        endIndex = validateRange(startIndex, endIndex);
        return new String(buffer, startIndex, endIndex - startIndex);
    }

    public String leftString(int length) {
        if (length <= 0) {
            return "";
        } else if (length >= size) {
            return new String(buffer, 0, size);
        } else {
            return new String(buffer, 0, length);
        }
    }

    public String rightString(int length) {
        if (length <= 0) {
            return "";
        } else if (length >= size) {
            return new String(buffer, 0, size);
        } else {
            return new String(buffer, size - length, length);
        }
    }

    public String midString(int index, int length) {
        if (index < 0) {
            index = 0;
        }
        if (length <= 0 || index >= size) {
            return "";
        }
        if (size <= index + length) {
            return new String(buffer, index, size - index);
        } else {
            return new String(buffer, index, length);
        }
    }

    public boolean contains(char ch) {
        char[] thisBuf = buffer;
        for (int i = 0; i < this.size; i++) {
            if (thisBuf[i] == ch) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(String str) {
        return indexOf(str, 0) >= 0;
    }

    public boolean contains(StrMatcher matcher) {
        return indexOf(matcher, 0) >= 0;
    }

    public int indexOf(char ch) {
        return indexOf(ch, 0);
    }

    public int indexOf(char ch, int startIndex) {
        startIndex = (startIndex < 0 ? 0 : startIndex);
        if (startIndex >= size) {
            return -1;
        }
        char[] thisBuf = buffer;
        for (int i = startIndex; i < size; i++) {
            if (thisBuf[i] == ch) {
                return i;
            }
        }
        return -1;
    }

    public int indexOf(String str) {
        return indexOf(str, 0);
    }

    public int indexOf(String str, int startIndex) {
        startIndex = (startIndex < 0 ? 0 : startIndex);
        if (str == null || startIndex >= size) {
            return -1;
        }
        int strLen = str.length();
        if (strLen == 1) {
            return indexOf(str.charAt(0), startIndex);
        }
        if (strLen == 0) {
            return startIndex;
        }
        if (strLen > size) {
            return -1;
        }
        char[] thisBuf = buffer;
        int len = size - strLen + 1;
        outer: for (int i = startIndex; i < len; i++) {
            for (int j = 0; j < strLen; j++) {
                if (str.charAt(j) != thisBuf[i + j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public int indexOf(StrMatcher matcher) {
        return indexOf(matcher, 0);
    }

    public int indexOf(StrMatcher matcher, int startIndex) {
        startIndex = (startIndex < 0 ? 0 : startIndex);
        if (matcher == null || startIndex >= size) {
            return -1;
        }
        int len = size;
        char[] buf = buffer;
        for (int i = startIndex; i < len; i++) {
            if (matcher.isMatch(buf, i, startIndex, len) > 0) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(char ch) {
        return lastIndexOf(ch, size - 1);
    }

    public int lastIndexOf(char ch, int startIndex) {
        startIndex = (startIndex >= size ? size - 1 : startIndex);
        if (startIndex < 0) {
            return -1;
        }
        for (int i = startIndex; i >= 0; i--) {
            if (buffer[i] == ch) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(String str) {
        return lastIndexOf(str, size - 1);
    }

    public int lastIndexOf(String str, int startIndex) {
        startIndex = (startIndex >= size ? size - 1 : startIndex);
        if (str == null || startIndex < 0) {
            return -1;
        }
        int strLen = str.length();
        if (strLen > 0 && strLen <= size) {
            if (strLen == 1) {
                return lastIndexOf(str.charAt(0), startIndex);
            }
            outer: for (int i = startIndex - strLen + 1; i >= 0; i--) {
                for (int j = 0; j < strLen; j++) {
                    if (str.charAt(j) != buffer[i + j]) {
                        continue outer;
                    }
                }
                return i;
            }
        } else if (strLen == 0) {
            return startIndex;
        }
        return -1;
    }

    public int lastIndexOf(StrMatcher matcher) {
        return lastIndexOf(matcher, size);
    }

    public int lastIndexOf(StrMatcher matcher, int startIndex) {
        startIndex = (startIndex >= size ? size - 1 : startIndex);
        if (matcher == null || startIndex < 0) {
            return -1;
        }
        char[] buf = buffer;
        int endIndex = startIndex + 1;
        for (int i = startIndex; i >= 0; i--) {
            if (matcher.isMatch(buf, i, 0, endIndex) > 0) {
                return i;
            }
        }
        return -1;
    }

    public StrTokenizer asTokenizer() {
        return new StrBuilderTokenizer();
    }

    public Reader asReader() {
        return new StrBuilderReader();
    }

    public Writer asWriter() {
        return new StrBuilderWriter();
    }

    public boolean equalsIgnoreCase(StrBuilder other) {
        if (this == other) {
            return true;
        }
        if (this.size != other.size) {
            return false;
        }
        char[] thisBuf = this.buffer;
        char[] otherBuf = other.buffer;
        for (int i = size - 1; i >= 0; i--) {
            char c1 = thisBuf[i];
            char c2 = otherBuf[i];
            if (c1 != c2 && Character.toUpperCase(c1) != Character.toUpperCase(c2)) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(StrBuilder other) {
        if (this == other) {
            return true;
        }
        if (this.size != other.size) {
            return false;
        }
        char[] thisBuf = this.buffer;
        char[] otherBuf = other.buffer;
        for (int i = size - 1; i >= 0; i--) {
            if (thisBuf[i] != otherBuf[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object obj) {
        if (obj instanceof StrBuilder) {
            return equals((StrBuilder) obj);
        }
        return false;
    }

    public int hashCode() {
        char[] buf = buffer;
        int hash = 0;
        for (int i = size - 1; i >= 0; i--) {
            hash = 31 * hash + buf[i];
        }
        return hash;
    }

    public String toString() {
        return new String(buffer, 0, size);
    }

    public StringBuffer toStringBuffer() {
        return new StringBuffer(size).append(buffer, 0, size);
    }

    protected int validateRange(int startIndex, int endIndex) {
        if (startIndex < 0) {
            throw new StringIndexOutOfBoundsException(startIndex);
        }
        if (endIndex > size) {
            endIndex = size;
        }
        if (startIndex > endIndex) {
            throw new StringIndexOutOfBoundsException("end < start");
        }
        return endIndex;
    }

    protected void validateIndex(int index) {
        if (index < 0 || index > size) {
            throw new StringIndexOutOfBoundsException(index);
        }
    }

    class StrBuilderTokenizer extends StrTokenizer {

        StrBuilderTokenizer() {
            super();
        }

        protected List tokenize(char[] chars, int offset, int count) {
            if (chars == null) {
                return super.tokenize(StrBuilder.this.buffer, 0, StrBuilder.this.size());
            } else {
                return super.tokenize(chars, offset, count);
            }
        }

        public String getContent() {
            String str = super.getContent();
            if (str == null) {
                return StrBuilder.this.toString();
            } else {
                return str;
            }
        }
    }

    class StrBuilderReader extends Reader {

        private int pos;

        private int mark;

        StrBuilderReader() {
            super();
        }

        public void close() {
        }

        public int read() {
            if (ready() == false) {
                return -1;
            }
            return StrBuilder.this.charAt(pos++);
        }

        public int read(char[] b, int off, int len) {
            if (off < 0 || len < 0 || off > b.length || (off + len) > b.length || (off + len) < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }
            if (pos >= StrBuilder.this.size()) {
                return -1;
            }
            if (pos + len > size()) {
                len = StrBuilder.this.size() - pos;
            }
            StrBuilder.this.getChars(pos, pos + len, b, off);
            pos += len;
            return len;
        }

        public long skip(long n) {
            if (pos + n > StrBuilder.this.size()) {
                n = StrBuilder.this.size() - pos;
            }
            if (n < 0) {
                return 0;
            }
            pos += n;
            return n;
        }

        public boolean ready() {
            return pos < StrBuilder.this.size();
        }

        public boolean markSupported() {
            return true;
        }

        public void mark(int readAheadLimit) {
            mark = pos;
        }

        public void reset() {
            pos = mark;
        }
    }

    class StrBuilderWriter extends Writer {

        StrBuilderWriter() {
            super();
        }

        public void close() {
        }

        public void flush() {
        }

        public void write(int c) {
            StrBuilder.this.append((char) c);
        }

        public void write(char[] cbuf) {
            StrBuilder.this.append(cbuf);
        }

        public void write(char[] cbuf, int off, int len) {
            StrBuilder.this.append(cbuf, off, len);
        }

        public void write(String str) {
            StrBuilder.this.append(str);
        }

        public void write(String str, int off, int len) {
            StrBuilder.this.append(str, off, len);
        }
    }
}
