package com.rmc.ui.newui;

import com.rmc.parser.model.Organization;
import com.rmc.parser.model.Program;
import com.rmc.parser.model.Teacher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import javafx.scene.control.TableView;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProgramTableController.
 */
class ProgramTableControllerTest {
    
    @Test
    @DisplayName("Should create table controller")
    void testCreate() {
        ProgramTableController controller = new ProgramTableController();
        
        assertNotNull(controller.getTableView());
        assertEquals(0, controller.getProgramCount());
    }
    
    @Test
    @DisplayName("Should add programs to table")
    void testAddPrograms() {
        ProgramTableController controller = new ProgramTableController();
        
        Program program = Program.builder()
                .id("1")
                .title("Test Program")
                .organization(Organization.builder().name("Test Org").build())
                .age("7-10 лет")
                .hours("36 часов")
                .price("5000 руб")
                .build();
        
        controller.addProgram(program);
        
        assertEquals(1, controller.getProgramCount());
    }
    
    @Test
    @DisplayName("Should set programs list")
    void testSetPrograms() {
        ProgramTableController controller = new ProgramTableController();
        
        List<Program> programs = List.of(
                Program.builder().id("1").title("Program 1").build(),
                Program.builder().id("2").title("Program 2").build()
        );
        
        controller.setPrograms(programs);
        
        assertEquals(2, controller.getProgramCount());
    }
    
    @Test
    @DisplayName("Should clear programs")
    void testClear() {
        ProgramTableController controller = new ProgramTableController();
        
        controller.addProgram(Program.builder().id("1").title("Test").build());
        assertEquals(1, controller.getProgramCount());
        
        controller.clear();
        assertEquals(0, controller.getProgramCount());
    }
    
    @Test
    @DisplayName("Should handle null program list")
    void testNullList() {
        ProgramTableController controller = new ProgramTableController();
        
        controller.setPrograms(null);
        assertEquals(0, controller.getProgramCount());
    }
    
    @Test
    @DisplayName("Should display teachers in table")
    void testTeachers() {
        ProgramTableController controller = new ProgramTableController();
        
        Program program = Program.builder()
                .id("1")
                .title("Test Program")
                .addTeacher(Teacher.builder().name("Иванов И.И.").build())
                .addTeacher(Teacher.builder().name("Петров П.П.").build())
                .build();
        
        controller.addProgram(program);
        
        assertEquals(1, controller.getProgramCount());
    }
}
