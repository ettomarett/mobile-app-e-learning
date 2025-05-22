# LLM Course Creation Mechanism Summary

This document summarizes the implementation of the course creation mechanism in the SkillLearn app.

## Overview

The course creation mechanism allows users to request the AI Assistant to create an entire course, including sections, content, and quizzes. The course is then automatically uploaded to Firebase and becomes available in the app's catalog.

## Components

1. **FirebaseCourseParser**
   - Parses LLM-generated course content using a structured syntax
   - Extracts course details, sections, and quizzes
   - Uploads the parsed data to Firebase

2. **LLMRepository Enhancements**
   - Added course creation detection in LLM responses
   - Added callback mechanism for course creation events
   - Updated system prompt to include course creation syntax

3. **LLMViewModel Enhancements**
   - Added LiveData observers for course creation status
   - Added methods to reset course creation state
   - Implemented CourseCreationCallback interface

4. **UI Components**
   - Added progress indicator in LLMChatFragment
   - Created dialog for course creation success
   - Added navigation to view the created course

5. **Models**
   - Created Question model for quiz questions
   - Updated Quiz model to use the new Question class
   - Updated CourseSection to include quizId property

## Course Creation Syntax

The LLM is instructed to use the following syntax for course creation:

```
<FirebaseCourse>
  <CourseDetails>
    title: [Course Title]
    description: [Detailed course description]
    category: [Course Category]
    level: [Débutant|Intermédiaire|Expert]
    durationMinutes: [Total duration in minutes]
    tags: [Comma separated tags]
    imageUrl: [URL to course image]
  </CourseDetails>
  
  <Section>
    title: [Section title]
    description: [Brief section description]
    durationMinutes: [Section duration in minutes]
    orderIndex: [Order in course, starting from 0]
    content: [HTML content with <p> tags]
    videoUrl: [YouTube video URL]
  </Section>
  
  [Additional sections...]
  
  <Quiz>
    title: [Quiz title]
    passingScore: [Score needed to pass, e.g., 70]
    
    <Question>
      question: [Question text]
      options: [Option A|Option B|Option C|Option D]
      correctOptionIndex: [Index of correct option (0-3)]
      explanation: [Explanation for the answer]
    </Question>
    
    [Additional questions...]
  </Quiz>
</FirebaseCourse>
```

## User Flow

1. User asks the AI Assistant to create a course
2. LLM generates course content using the structured syntax
3. FirebaseCourseParser detects and parses the course content
4. UI shows progress indicator during parsing and upload
5. On success, a dialog is shown with the option to view the course
6. The course is now available in the app's catalog

## Testing

Basic unit tests for the pattern matching logic have been implemented in FirebaseCourseParserTest.java. 