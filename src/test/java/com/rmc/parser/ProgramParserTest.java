package com.rmc.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProgramParser.
 */
class ProgramParserTest {
    
    @Test
    @DisplayName("Should parse HTML with program cards")
    void testParseWithCards() {
        String html = """
            <html>
            <body>
                <div class="program-card">
                    <h3>Программа 1</h3>
                    <a href="/program/123">Подробнее</a>
                    <p class="description">Описание программы</p>
                </div>
                <div class="program-card">
                    <h3>Программа 2</h3>
                    <a href="/program/456">Подробнее</a>
                </div>
            </body>
            </html>
            """;
        
        ProgramParser.ParseResult result = ProgramParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals(2, result.getProgramCount());
        assertEquals("Программа 1", result.getPrograms().get(0).getTitle());
        assertEquals("Программа 2", result.getPrograms().get(1).getTitle());
    }
    
    @Test
    @DisplayName("Should extract program ID from URL")
    void testExtractIdFromUrl() {
        String html = """
            <html>
            <body>
                <article>
                    <h2>Test Program</h2>
                    <a href="/program/789">Link</a>
                </article>
            </body>
            </html>
            """;
        
        ProgramParser.ParseResult result = ProgramParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals(1, result.getProgramCount());
        assertEquals("789", result.getPrograms().get(0).getId());
    }
    
    @Test
    @DisplayName("Should extract description")
    void testExtractDescription() {
        String html = """
            <html>
            <body>
                <div class="program-card">
                    <h3>Program</h3>
                    <p class="description">This is a test description</p>
                </div>
            </body>
            </html>
            """;
        
        ProgramParser.ParseResult result = ProgramParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals("This is a test description", result.getPrograms().get(0).getDescription().orElse(""));
    }
    
    @Test
    @DisplayName("Should extract image URL")
    void testExtractImageUrl() {
        String html = """
            <html>
            <body>
                <div class="program-card">
                    <h3>Program</h3>
                    <img src="/images/program.jpg" alt="Program">
                </div>
            </body>
            </html>
            """;
        
        ProgramParser.ParseResult result = ProgramParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals("/images/program.jpg", result.getPrograms().get(0).getImageUrl().orElse(""));
    }
    
    @Test
    @DisplayName("Should handle null HTML")
    void testNullHtml() {
        ProgramParser.ParseResult result = ProgramParser.parse(null);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().isPresent());
    }
    
    @Test
    @DisplayName("Should handle empty HTML")
    void testEmptyHtml() {
        ProgramParser.ParseResult result = ProgramParser.parse("");
        
        assertFalse(result.isSuccess());
    }
    
    @Test
    @DisplayName("Should handle HTML without cards")
    void testNoCards() {
        String html = """
            <html>
            <body>
                <h1>No programs here</h1>
            </body>
            </html>
            """;
        
        ProgramParser.ParseResult result = ProgramParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals(0, result.getProgramCount());
    }
    
    @Test
    @DisplayName("Should parse course cards")
    void testParseCourseCards() {
        String html = """
            <html>
            <body>
                <div class="course-card">
                    <h3>Курс по программированию</h3>
                    <a href="/course/101">Ссылка</a>
                </div>
            </body>
            </html>
            """;
        
        ProgramParser.ParseResult result = ProgramParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals(1, result.getProgramCount());
        assertEquals("Курс по программированию", result.getPrograms().get(0).getTitle());
    }
    
    @Test
    @DisplayName("Should extract organization")
    void testExtractOrganization() {
        String html = """
            <html>
            <body>
                <div class="program-card">
                    <h3>Программа</h3>
                    <div class="organization">
                        <span class="name">Школа №1</span>
                    </div>
                </div>
            </body>
            </html>
            """;
        
        ProgramParser.ParseResult result = ProgramParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getPrograms().get(0).getOrganization().isPresent());
        assertEquals("Школа №1", result.getPrograms().get(0).getOrganization().get().getName());
    }
    
    @Test
    @DisplayName("Should extract teachers")
    void testExtractTeachers() {
        String html = """
            <html>
            <body>
                <div class="program-card">
                    <h3>Программа</h3>
                    <div class="teacher">Иванов И.И.</div>
                    <div class="teacher">Петров П.П.</div>
                </div>
            </body>
            </html>
            """;
        
        ProgramParser.ParseResult result = ProgramParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals(2, result.getPrograms().get(0).getTeachers().size());
        assertEquals("Иванов И.И.", result.getPrograms().get(0).getTeachers().get(0).getName());
    }
}
