package com.library.android.network;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.library.android.BuildConfig;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Gson TypeAdapterFactory — 修复服务端 UTF-8 双重编码（Mojibake）.
 *
 * <p>数据库字段经 Latin-1→UTF-8 二次编码后变为乱码，本适配器在反序列化
 * String 时检测并自动修复。通过 TypeAdapterFactory 注册以避免与 Gson 内部
 * 类型系统冲突。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class Utf8FixTypeAdapterFactory implements TypeAdapterFactory {

    private static final String TAG = "Utf8Fix";

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() != String.class) {
            return null;
        }
        return (TypeAdapter<T>) new Utf8FixStringAdapter();
    }

    private static class Utf8FixStringAdapter extends TypeAdapter<String> {

        @Override
        public void write(JsonWriter out, String value) throws IOException {
            out.value(value);
        }

        @Override
        public String read(JsonReader in) throws IOException {
            // 正确处理 null
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String original = in.nextString();
            if (original == null || original.isEmpty()) {
                return original;
            }
            try {
                return fixDoubleEncodedUtf8(original);
            } catch (Exception e) {
                return original;
            }
        }

        /** Windows-1252 charset，惰性加载. */
        private static Charset windows1252;

        private static Charset getWindows1252() {
            if (windows1252 == null) {
                try {
                    windows1252 = Charset.forName("windows-1252");
                } catch (Exception e) {
                    windows1252 = StandardCharsets.ISO_8859_1;
                }
            }
            return windows1252;
        }

        /**
         * 检测并修复 UTF-8 双重编码（Windows-1252 / Latin-1 路径）.
         * <p>
         * 双重编码模式：MySQL 连接为 latin1，原始 UTF-8 字节被
         * 解释为 Windows-1252/Latin-1 字符 → 再次编码为 UTF-8。
         * 逆转：按 Windows-1252 取字节 → 按 UTF-8 重建字符串。
         */
        static String fixDoubleEncodedUtf8(String input) {
            // 快速放行：含 CJK 字符 = 编码正确，无需修复
            int len = input.length();
            for (int i = 0; i < len; i++) {
                char c = input.charAt(i);
                if (c >= 0x4E00 && c <= 0x9FFF || c >= 0x3040 && c <= 0x30FF) {
                    return input;
                }
            }

            // 检测是否有非 ASCII 字符（纯 ASCII 无需修复）
            boolean hasNonAscii = false;
            for (int i = 0; i < len; i++) {
                if (input.charAt(i) >= 0x80) {
                    hasNonAscii = true;
                    break;
                }
            }
            if (!hasNonAscii) return input;

            // 用 Windows-1252 逆转换（覆盖 0x80-0x9F 区域）
            try {
                byte[] raw = input.getBytes(getWindows1252());
                String fixed = new String(raw, StandardCharsets.UTF_8);
                if (hasCjkOrShorter(fixed, input)) return fixed;
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Log.w(TAG, "Windows-1252 decode failed, trying fallback", e);
            }

            // 回退：ISO-8859-1
            try {
                byte[] raw = input.getBytes(StandardCharsets.ISO_8859_1);
                String fixed = new String(raw, StandardCharsets.UTF_8);
                if (hasCjkOrShorter(fixed, input)) return fixed;
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Log.w(TAG, "ISO-8859-1 fallback decode failed", e);
            }

            return input;
        }

        /** 检查修复后字符串是否含 CJK 字符，或比原文更短（去除了冗余编码字节）. */
        private static boolean hasCjkOrShorter(String fixed, String original) {
            if (fixed.length() < original.length()) return true;
            for (int i = 0; i < fixed.length(); i++) {
                char c = fixed.charAt(i);
                if (c >= 0x4E00 && c <= 0x9FFF || c >= 0x3040 && c <= 0x30FF) {
                    return true;
                }
            }
            return false;
        }
    }
}
