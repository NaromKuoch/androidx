/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.graphics;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.content.res.FontResourcesParserCompat.FontFamilyFilesResourceEntry;
import androidx.core.content.res.FontResourcesParserCompat.FontFileResourceEntry;
import androidx.core.provider.FontsContractCompat.FontInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

/**
 * Implementation of the Typeface compat methods for API 14 and above.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
class TypefaceCompatBaseImpl {
    private static final String TAG = "TypefaceCompatBaseImpl";
    private static final int INVALID_KEY = 0;

    /**
     * Maps a unique identifier from a Typeface to it's family
     */
    @SuppressLint("BanConcurrentHashMap")
    private java.util.concurrent.ConcurrentHashMap<Long, FontFamilyFilesResourceEntry>
            mFontFamilies = new java.util.concurrent.ConcurrentHashMap<>();

    private interface StyleExtractor<T> {
        int getWeight(T t);
        boolean isItalic(T t);
    }

    private static <T> T findBestFont(T[] fonts, int style, StyleExtractor<T> extractor) {
        final int targetWeight = (style & Typeface.BOLD) == 0 ? 400 : 700;
        final boolean isTargetItalic = (style & Typeface.ITALIC) != 0;
        return findBestFont(fonts, targetWeight, isTargetItalic, extractor);
    }

    private static <T> T findBestFont(T[] fonts, int targetWeight, boolean isTargetItalic,
            StyleExtractor<T> extractor) {
        T best = null;
        int bestScore = Integer.MAX_VALUE;  // smaller is better

        for (final T font : fonts) {
            final int score = (Math.abs(extractor.getWeight(font) - targetWeight) * 2)
                    + (extractor.isItalic(font) == isTargetItalic ? 0 : 1);

            if (best == null || bestScore > score) {
                best = font;
                bestScore = score;
            }
        }
        return best;
    }

    private static long getUniqueKey(@Nullable final Typeface typeface) {
        if (typeface == null) {
            return INVALID_KEY;
        }

        try {
            final Field field = Typeface.class.getDeclaredField("native_instance");
            field.setAccessible(true);
            final Number num = (Number) field.get(typeface);
            return num.longValue();
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "Could not retrieve font from family.", e);
            return INVALID_KEY;
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Could not retrieve font from family.", e);
            return INVALID_KEY;
        }
    }

    protected FontInfo findBestInfo(FontInfo[] fonts, int style) {
        return findBestFont(fonts, style, new StyleExtractor<FontInfo>() {
            @Override
            public int getWeight(FontInfo info) {
                return info.getWeight();
            }

            @Override
            public boolean isItalic(FontInfo info) {
                return info.isItalic();
            }
        });
    }

    // Caller must close the stream.
    protected Typeface createFromInputStream(Context context, InputStream is) {
        final File tmpFile = TypefaceCompatUtil.getTempFile(context);
        if (tmpFile == null) {
            return null;
        }
        try {
            if (!TypefaceCompatUtil.copyToFile(tmpFile, is)) {
                return null;
            }
            return Typeface.createFromFile(tmpFile.getPath());
        } catch (RuntimeException e) {
            // This was thrown from Typeface.createFromFile when a Typeface could not be loaded,
            // such as due to an invalid ttf or unreadable file. We don't want to throw that
            // exception anymore.
            return null;
        } finally {
            tmpFile.delete();
        }
    }

    @Nullable
    public Typeface createFromFontInfo(Context context,
            @Nullable CancellationSignal cancellationSignal, @NonNull FontInfo[] fonts, int style) {
        // When we load from file, we can only load one font so just take the first one.
        if (fonts.length < 1) {
            return null;
        }
        FontInfo font = findBestInfo(fonts, style);
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(font.getUri());
            return createFromInputStream(context, is);
        } catch (IOException e) {
            return null;
        } finally {
            TypefaceCompatUtil.closeQuietly(is);
        }
    }

    private FontFileResourceEntry findBestEntry(FontFamilyFilesResourceEntry entry, int style) {
        return findBestFont(entry.getEntries(), style, new StyleExtractor<FontFileResourceEntry>() {
            @Override
            public int getWeight(FontFileResourceEntry entry) {
                return entry.getWeight();
            }

            @Override
            public boolean isItalic(FontFileResourceEntry entry) {
                return entry.isItalic();
            }
        });
    }

    private FontFileResourceEntry findBestEntry(FontFamilyFilesResourceEntry entry, int weight,
            boolean italic) {
        return findBestFont(entry.getEntries(), weight, italic,
                new StyleExtractor<FontFileResourceEntry>() {
                    @Override
                    public int getWeight(FontFileResourceEntry entry) {
                        return entry.getWeight();
                    }

                    @Override
                    public boolean isItalic(FontFileResourceEntry entry) {
                        return entry.isItalic();
                    }
                });
    }

    @Nullable
    public Typeface createFromFontFamilyFilesResourceEntry(Context context,
            FontFamilyFilesResourceEntry entry, Resources resources, int style) {
        FontFileResourceEntry best = findBestEntry(entry, style);
        if (best == null) {
            return null;
        }
        final Typeface typeface = TypefaceCompat.createFromResourcesFontFile(
                context, resources, best.getResourceId(), best.getFileName(), 0, style);

        addFontFamily(typeface, entry);

        return typeface;
    }

    @Nullable
    Typeface createFromFontFamilyFilesResourceEntry(Context context,
            FontFamilyFilesResourceEntry entry, Resources resources, int weight, boolean italic) {
        FontFileResourceEntry best = findBestEntry(entry, weight, italic);
        if (best == null) {
            return null;
        }
        final Typeface typeface = TypefaceCompat.createFromResourcesFontFile(
                context, resources, best.getResourceId(), best.getFileName(), 0, Typeface.NORMAL);

        addFontFamily(typeface, entry);

        return typeface;
    }

    /**
     * Used by Resources to load a font resource of type font file.
     */
    @Nullable
    public Typeface createFromResourcesFontFile(
            Context context, Resources resources, int id, String path, int style) {
        final File tmpFile = TypefaceCompatUtil.getTempFile(context);
        if (tmpFile == null) {
            return null;
        }
        try {
            if (!TypefaceCompatUtil.copyToFile(tmpFile, resources, id)) {
                return null;
            }
            return Typeface.createFromFile(tmpFile.getPath());
        } catch (RuntimeException e) {
            // This was thrown from Typeface.createFromFile when a Typeface could not be loaded.
            // such as due to an invalid ttf or unreadable file. We don't want to throw that
            // exception anymore.
            return null;
        } finally {
            tmpFile.delete();
        }
    }

    @NonNull
    Typeface createWeightStyle(@NonNull Context context, @NonNull Typeface base,
            int weight, boolean italic) {
        Typeface out = null;
        try {
            out = WeightTypefaceApi14.createWeightStyle(this, context, base, weight, italic);
        } catch (RuntimeException fallbackFailed) {
            // ignore
        }
        if (out == null) {
            out = base;
        }
        return out;
    }

    /**
     * Retrieves the font family resource entries given a unique identifier for a Typeface
     */
    @Nullable
    FontFamilyFilesResourceEntry getFontFamily(final Typeface typeface) {
        final long key = getUniqueKey(typeface);
        if (key == INVALID_KEY) {
            return null;
        }
        return mFontFamilies.get(key);
    }

    private void addFontFamily(final Typeface typeface, final FontFamilyFilesResourceEntry entry) {
        final long key = getUniqueKey(typeface);
        if (key != INVALID_KEY) {
            mFontFamilies.put(key, entry);
        }
    }
}
