package com.exotic.plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts normal text into small-caps Unicode characters for a cleaner,
 * less "chunky" look than the default Minecraft font - no resource pack required.
 * (A true custom font would require shipping a resource pack with a font
 * definition; this is a lightweight, no-dependency alternative.)
 */
public final class TextStyle {

    private static final Map<Character, Character> SMALL_CAPS = new HashMap<>();
    static {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String smallCaps = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
        for (int i = 0; i < upper.length(); i++) {
            SMALL_CAPS.put(upper.charAt(i), smallCaps.charAt(i));
            SMALL_CAPS.put(Character.toLowerCase(upper.charAt(i)), smallCaps.charAt(i));
        }
    }

    private TextStyle() {}

    public static String toSmallCaps(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append(SMALL_CAPS.getOrDefault(c, c));
        }
        return sb.toString();
    }
}
