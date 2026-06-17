package com.rmc.ui.newui;

import com.rmc.parser.model.Program;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Контроллер для отображения программ в TableView.
 */
public class ProgramTableController {
    
    private final TableView<Program> tableView;
    private final ObservableList<Program> programs;
    
    public ProgramTableController() {
        this.programs = FXCollections.observableArrayList();
        this.tableView = createTableView();
    }
    
    private TableView<Program> createTableView() {
        TableView<Program> table = new TableView<>();
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Название
        TableColumn<Program, String> titleCol = new TableColumn<>("Название");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setMinWidth(200);
        titleCol.setPrefWidth(250);
        
        // Организация
        TableColumn<Program, String> orgCol = new TableColumn<>("Организация");
        orgCol.setCellValueFactory(cellData -> {
            Program p = cellData.getValue();
            if (p.getOrganization().isPresent()) {
                return new javafx.beans.property.SimpleStringProperty(
                        p.getOrganization().get().getName());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        orgCol.setMinWidth(150);
        orgCol.setPrefWidth(180);
        
        // Направление
        TableColumn<Program, String> directionCol = new TableColumn<>("Направление");
        directionCol.setCellValueFactory(new PropertyValueFactory<>("direction"));
        directionCol.setMinWidth(100);
        directionCol.setPrefWidth(120);
        
        // Возраст
        TableColumn<Program, String> ageCol = new TableColumn<>("Возраст");
        ageCol.setCellValueFactory(new PropertyValueFactory<>("age"));
        ageCol.setMinWidth(80);
        ageCol.setPrefWidth(100);
        
        // Часы
        TableColumn<Program, String> hoursCol = new TableColumn<>("Часы");
        hoursCol.setCellValueFactory(new PropertyValueFactory<>("hours"));
        hoursCol.setMinWidth(60);
        hoursCol.setPrefWidth(80);
        
        // Цена
        TableColumn<Program, String> priceCol = new TableColumn<>("Цена");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setMinWidth(80);
        priceCol.setPrefWidth(100);
        
        // Преподаватели
        TableColumn<Program, String> teachersCol = new TableColumn<>("Преподаватели");
        teachersCol.setCellValueFactory(cellData -> {
            Program p = cellData.getValue();
            String teachers = p.getTeachers().stream()
                    .map(t -> t.getName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            return new javafx.beans.property.SimpleStringProperty(teachers);
        });
        teachersCol.setMinWidth(120);
        teachersCol.setPrefWidth(150);
        
        table.getColumns().addAll(titleCol, orgCol, directionCol, ageCol, hoursCol, priceCol, teachersCol);
        table.setItems(programs);
        
        return table;
    }
    
    public TableView<Program> getTableView() {
        return tableView;
    }
    
    public void setPrograms(java.util.List<Program> programList) {
        programs.clear();
        if (programList != null) {
            programs.addAll(programList);
        }
    }
    
    public void addProgram(Program program) {
        programs.add(program);
    }
    
    public void clear() {
        programs.clear();
    }
    
    public int getProgramCount() {
        return programs.size();
    }
}
