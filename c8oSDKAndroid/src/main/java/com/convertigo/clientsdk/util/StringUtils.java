package com.convertigo.clientsdk.util;

public class StringUtils {

	/**
     * Normalizes a string, i.e. replaces all blank spaces by underline character,
     * all accentuated characters by their unaccentuated character, and makes
     * the first character non digit if needed. It also deletes starting and
     * trailing spaces.
     *
     * @param text the text to normalize.
     *
     * @return the normalized text.
     */
    public static String normalize(String text) {
        return StringUtils.normalize(text, true);
    }
    
    /**
     * Normalizes a string, i.e. replaces all blank spaces by underline character,
     * all accentuated characters by their unaccentuated character, and makes
     * the first character non digit if needed. It also deletes starting and
     * trailing spaces.
     *
     * @param text the text to normalize.
     * @param bIncludeNonAlphanumericCharacters defines if non alphanumeric characters
     * should be included (as '_' character).
     *
     * @return the normalized text.
     */
    public static String normalize(String text, boolean bIncludeNonAlphanumericCharacters) {
        // First trim the text
        text = text.trim();
        
        if (text.length() == 0)
            return ("");
        
        char[] aText = new char[text.length()];
        
        int strLen = text.length();
        char c;
        
        int len = 0;
        
        for (int i = 0 ; i < strLen ; i++) {
            c = text.charAt(i);

            if (c == ' ') {
                aText[len++] = '_';
            }
            else if ((c == 'à') || (c == 'â') || (c == 'ä')) {
                aText[len++] = 'a';
            }
            else if ((c == 'é') || (c == 'è') || (c == 'ê') || (c == 'ë')) {
                aText[len++] = 'e';
            }
            else if ((c == 'î') || (c == 'ï')) {
                aText[len++] = 'i';
            }
            else if ((c == 'ô') || (c == 'ö')) {
                aText[len++] = 'o';
            }
            else if ((c == 'ù') || (c == 'û') || (c == 'ü')) {
                aText[len++] = 'u';
            }
            else if ((c == 'ÿ')) {
                aText[len++] = 'y';
            }
            else if ((c == 'ç')) {
                aText[len++] = 'c';
            }
            else if ((c >= (char) 48) && (c <= (char) 57)) { // Numbers
                aText[len++] = c;
            }
            else if ((c >= (char) 65) && (c <= (char) 90)) { // Uppercase letters
                aText[len++] = c;
            }
            else if ((c >= (char) 97) && (c <= (char) 122)) { // Lowercase letters
                aText[len++] = c;
            }
            else if (bIncludeNonAlphanumericCharacters) {
                aText[len++] = '_';
            }
        }
        
        String res = new String(aText, 0, len);
        
        // First char must only be a letter, if not '_' is legal
        if ((res.length() > 0) && (Character.isDigit(res.charAt(0)))) {
            res = "_" + res;
        }
        
        return res;
    }
	
}
