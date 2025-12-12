//package com.Nexus.Chatter.util;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class SimpleChunker {
//
//    public static List<String> split(String text) {
//        List<String> chunks = new ArrayList<>();
//        // Simple strategy: Split by double newlines (paragraphs)
//        String[] paragraphs = text.split("\\n\\n");
//
//        for (String p : paragraphs) {
//            // Ignore tiny empty lines or noise
//            if (p.trim().length() > 20) {
//                chunks.add(p.trim());
//            }
//        }
//        return chunks;
//    }
//}


package com.Nexus.Chatter.util;




import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import java.io.File;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class SimpleChunker {

    private static final Tika tika = new Tika();
    private static final int MAX_CHUNK_SIZE = 600;
    private static final int OVERLAP_SENTENCES = 2; // Keep previous 2 sentences for context

    /** 1. Extract raw text but KEEP newlines for structure detection */
    public static String extractText(File file) throws IOException, TikaException {
        // Do NOT use replaceAll("\\s+", " ") here!
        return tika.parseToString(file);
    }

    public static List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        // 2. Normalize line endings (Windows \r\n -> Unix \n)
        String rawText = text.replace("\r\n", "\n").replace("\r", "\n");

        // 3. Split into logical blocks (Paragraphs) first
        // We look for double newlines, which usually indicate a new paragraph in PDFs
        String[] paragraphs = rawText.split("\n\\s*\n");

        StringBuilder currentChunk = new StringBuilder();
        List<String> currentChunkSentences = new ArrayList<>();

        for (String paragraph : paragraphs) {
            // Now we clean the paragraph (remove extra internal spaces)
            String cleanParagraph = paragraph.replaceAll("\\s+", " ").trim();
            if (cleanParagraph.isEmpty()) continue;

            // 4. Smart Sentence Splitting
            List<String> sentences = getSentences(cleanParagraph);

            for (String sentence : sentences) {
                // Check if adding this sentence breaks the limit
                if (currentChunk.length() + sentence.length() > MAX_CHUNK_SIZE) {
                    // Save the chunk
                    if (currentChunk.length() > 0) {
                        chunks.add(currentChunk.toString().trim());
                    }

                    // 5. Sliding Window Logic (The "Overlap")
                    // Instead of starting empty, we keep the last N sentences
                    currentChunk = new StringBuilder();
                    int startOverlap = Math.max(0, currentChunkSentences.size() - OVERLAP_SENTENCES);

                    // Re-add the overlap sentences to the new chunk
                    for (int i = startOverlap; i < currentChunkSentences.size(); i++) {
                        currentChunk.append(currentChunkSentences.get(i)).append(" ");
                    }

                    // Reset the tracking list to just the overlap
                    List<String> newTracker = new ArrayList<>();
                    for (int i = startOverlap; i < currentChunkSentences.size(); i++) {
                        newTracker.add(currentChunkSentences.get(i));
                    }
                    currentChunkSentences = newTracker;
                }

                // Add new sentence
                currentChunk.append(sentence).append(" ");
                currentChunkSentences.add(sentence);
            }
        }

        // Add any remaining text
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private static List<String> getSentences(String text) {
        List<String> sentences = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
        iterator.setText(text);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            sentences.add(text.substring(start, end).trim());
        }
        return sentences;
    }
}