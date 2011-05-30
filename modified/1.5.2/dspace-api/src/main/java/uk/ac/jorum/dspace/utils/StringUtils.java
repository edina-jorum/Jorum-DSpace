package uk.ac.jorum.dspace.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * User: robin
 * Date: 15/02/11
 * Time: 15:21
 */
public class StringUtils
{

    public static List<String> splitOnComma(String multivalue)
    {
        return splitMultivalues(multivalue, ',');
    }

    /**
     * Split a string into tokens. Substrings within quotes will not be split.
     *
     * @param multivalue - The String to be split.
     * @param delimiter - comma,semicolon,etc.
     * @return A List of the tokens (Strings).
     */
    public static List<String> splitMultivalues(String multivalue, char delimiter)
    {
        List<String> tokens = new ArrayList<String>();
        StringBuffer token = new StringBuffer("");
        boolean withinQuotes = false;

        for (char character : multivalue.toCharArray())
        {
            if (character == '\"')
            {
                // Is it an opening or closing quote ?
                if (!withinQuotes)
                {
                    // Its a opening quote.
                    withinQuotes = true;

                    // If we have an unsaved token then save it. The user has been naughty and forgotten the delimiter.
                    writeToken(tokens, token);
                    token = new StringBuffer("");
                }
                else
                {
                    // Its an closing quote so we have a completed token.
                    withinQuotes = false;
                    writeToken(tokens, token);
                    token = new StringBuffer("");
                }
            }
            else
            {
                if (withinQuotes)
                {
                    token.append(character);
                }
                else
                {
                    if (character != delimiter)
                    {
                        token.append(character);
                    }
                    else
                    {
                        // We have hit a delimiter so we have a completed token.
                        writeToken(tokens, token);
                        token = new StringBuffer("");
                    }
                }
            }
        }

        // Save the last token.
        writeToken(tokens, token);

        return tokens;

    }

    private static void writeToken(List<String> tokens, StringBuffer token)
    {
        String tokenString = token.toString().trim();
        if (tokenString.length() > 0)
        {
            tokens.add(tokenString);
        }
    }


    /*
    * Put quotes around a string if contains a delimiter, prior to displaying it for
    * edit on the submission page.
    *
    */
    public static String encloseInQuotes(String theString)
    {
        if (containsComma(theString))
        {
            return "\"" + theString + "\"";
        }

        return theString;
    }

    public static boolean containsComma(String theString)
    {
        return containsDelimiter(theString, ',');
    }

    public static boolean containsDelimiter(String theString, char delimiter)
    {
        // Returns true if it contains the delimiter.
        return theString.indexOf(delimiter) != -1;
    }


    public static void main(String[] args)
    {
        // For testing purposes...
        List<String> tokens = splitOnComma("John Smith, Robin Taylor \"Dept of Physics, Uni of Edin\" , Jane Doe");
        for (String token : tokens)
        {
            System.out.println("token is : " + token);
        }
    }

}
