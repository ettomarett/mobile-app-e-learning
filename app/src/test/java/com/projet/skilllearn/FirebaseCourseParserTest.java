package com.projet.skilllearn;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for course creation pattern matching
 */
public class FirebaseCourseParserTest {

    @Test
    public void testFirebaseCoursePattern() {
        // Test the regex pattern for extracting course content
        Pattern coursePattern = Pattern.compile("<FirebaseCourse>(.*?)</FirebaseCourse>", Pattern.DOTALL);
        
        // Test with valid input
        String validInput = "<FirebaseCourse>\n" +
                "  <CourseDetails>\n" +
                "    title: Test Course\n" +
                "  </CourseDetails>\n" +
                "</FirebaseCourse>";
        
        Matcher matcher = coursePattern.matcher(validInput);
        assertTrue("Pattern should match valid input", matcher.find());
        assertNotNull("Extracted content should not be null", matcher.group(1));
        assertTrue("Extracted content should contain CourseDetails", 
                matcher.group(1).contains("<CourseDetails>"));
        
        // Test with invalid input
        String invalidInput = "This is not a valid course format";
        matcher = coursePattern.matcher(invalidInput);
        assertFalse("Pattern should not match invalid input", matcher.find());
    }
    
    @Test
    public void testCourseDetailsPattern() {
        // Test the regex pattern for extracting course details
        Pattern detailsPattern = Pattern.compile("<CourseDetails>(.*?)</CourseDetails>", Pattern.DOTALL);
        
        // Test with valid input
        String validInput = "<CourseDetails>\n" +
                "  title: Test Course\n" +
                "  description: This is a test course\n" +
                "  category: Test\n" +
                "</CourseDetails>";
        
        Matcher matcher = detailsPattern.matcher(validInput);
        assertTrue("Pattern should match valid input", matcher.find());
        assertNotNull("Extracted content should not be null", matcher.group(1));
        assertTrue("Extracted content should contain title", 
                matcher.group(1).contains("title: Test Course"));
        
        // Test with invalid input
        String invalidInput = "This is not a valid course details format";
        matcher = detailsPattern.matcher(invalidInput);
        assertFalse("Pattern should not match invalid input", matcher.find());
    }
    
    @Test
    public void testPropertyExtraction() {
        // Test the regex pattern for extracting properties
        String content = "  title: Test Course\n" +
                "  description: This is a test\n" +
                "    with multiple lines\n" +
                "  category: Test";
        
        Pattern pattern = Pattern.compile("title:\\s*(.*?)(?=\\n\\s*\\w+:|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        
        assertTrue("Should find title property", matcher.find());
        assertEquals("Test Course", matcher.group(1).trim());
        
        pattern = Pattern.compile("description:\\s*(.*?)(?=\\n\\s*\\w+:|$)", Pattern.DOTALL);
        matcher = pattern.matcher(content);
        
        assertTrue("Should find description property", matcher.find());
        assertEquals("This is a test\n    with multiple lines", matcher.group(1).trim());
    }
} 