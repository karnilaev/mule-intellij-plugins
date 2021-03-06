package org.mule.tooling.lang.dw.launcher.configuration.ui;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.NotNull;
import org.mule.tooling.lang.dw.WeaveFileType;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class WeaveRunnerConfPanel
{

    private TextFieldWithBrowseButton weaveHome;
    private JPanel mainPanel;
    private ModulesComboBox moduleCombo;
    private TextFieldWithBrowseButton output;
    private TextFieldWithBrowseButton weaveFile;
    private JPanel inputPanel;
    private TableView<WeaveInput> myInputsTable;
    private Project project;
    private ListTableModel<WeaveInput> myModel;

    public WeaveRunnerConfPanel(final Project project)
    {
        this.project = project;

        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        getWeaveHome().addBrowseFolderListener("Select DataWeave Home", "Select DataWeave Home", project, descriptor);

        descriptor = new FileChooserDescriptor(true, true, false, false, false, false);
        getOutput().addBrowseFolderListener("Select Output", "Select Output", project, descriptor);

        final FileChooserDescriptor waveDescriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withHideIgnored(true).withShowHiddenFiles(false).withFileFilter(new Condition<VirtualFile>() {
                    @Override
                    public boolean value(VirtualFile virtualFile) {
//                        String fn = virtualFile.getName().toLowerCase();
//                        return (fn.endsWith(".wev") || fn.endsWith(".dw"));
                        java.util.List<String> extensions = WeaveFileType.getInstance().getExtensions();
                        return (virtualFile.getExtension() != null && extensions.contains(virtualFile.getExtension().toLowerCase()));
                    }
                });
        getWeaveFile().addBrowseFolderListener("Select DataWeave File", "Select DataWeave File", project, waveDescriptor);

        myModel = new ListTableModel<>(NAME, FILE);
        myInputsTable = new TableView<>(myModel);
        myInputsTable.getEmptyText().setText("No input data was defined.");
        myInputsTable.setColumnSelectionAllowed(false);
        myInputsTable.setShowGrid(false);
        myInputsTable.setDragEnabled(false);
        myInputsTable.setShowHorizontalLines(false);
        myInputsTable.setShowVerticalLines(false);
        myInputsTable.setIntercellSpacing(new Dimension(0, 0));
        inputPanel.add(ToolbarDecorator.createDecorator(myInputsTable)
                                       .setAddAction(new AddWeaveInputAction(myModel))
                                       .setRemoveAction(new RemoveWeaveInputAction(myModel))
                                       .setEditAction(new EditWeaveInputAction(myModel))
                                       .setRemoveActionUpdater(e -> myInputsTable.getSelectedRowCount() >= 1)
                                       .setEditActionUpdater(e -> myInputsTable.getSelectedRowCount() >= 1 && myInputsTable.getSelectedObject() != null)
                                       .createPanel(), BorderLayout.CENTER);
    }

    private boolean showEditorDialog(@NotNull WeaveInput options)
    {
        final WeaveInputDialog dialog = new WeaveInputDialog(project);
        dialog.init(options.getName(), options.getPath());
        if (dialog.showAndGet())
        {
            options.setName(dialog.getName());
            options.setPath(dialog.getPath());
            return true;
        }
        return false;
    }

    public JPanel getMainPanel()
    {
        return mainPanel;
    }

    public TextFieldWithBrowseButton getWeaveHome()
    {
        return weaveHome;
    }

    public ModulesComboBox getModuleCombo()
    {
        return moduleCombo;
    }

    public ListTableModel<WeaveInput> getWeaveInputs()
    {
        return myModel;
    }

    public TextFieldWithBrowseButton getOutput()
    {
        return output;
    }

    public TextFieldWithBrowseButton getWeaveFile()
    {
        return weaveFile;
    }


    private final static ColumnInfo<WeaveInput, String> FILE = new ColumnInfo<WeaveInput, String>("Path")
    {
        public String valueOf(WeaveInput object)
        {
            return object.getPath();
        }

        public Comparator<WeaveInput> getComparator()
        {
            return (o, o1) -> o.getPath().compareTo(o1.getPath());
        }
    };

    private final static ColumnInfo<WeaveInput, String> NAME = new ColumnInfo<WeaveInput, String>("Name")
    {
        public String valueOf(WeaveInput object)
        {
            return object.getName();
        }

        public Comparator<WeaveInput> getComparator()
        {
            return (o, o1) -> o.getName().compareTo(o1.getName());
        }
    };

    private class AddWeaveInputAction implements AnActionButtonRunnable
    {
        private final ListTableModel<WeaveInput> myModel;

        public AddWeaveInputAction(ListTableModel<WeaveInput> myModel)
        {
            this.myModel = myModel;
        }

        @Override
        public void run(AnActionButton button)
        {
            final WeaveInput newOptions = new WeaveInput("in" + this.myModel.getItems().size(), "");
            if (showEditorDialog(newOptions))
            {
             //   this.myModel.addRow(newOptions); -- throws UnsupportedOperationException?
                final ArrayList<WeaveInput> weaveInputs = new ArrayList<>(myModel.getItems());
                weaveInputs.add(newOptions);
                myModel.setItems(weaveInputs);
            }
        }
    }

    private class RemoveWeaveInputAction implements AnActionButtonRunnable
    {
        private final ListTableModel<WeaveInput> myModel;

        public RemoveWeaveInputAction(ListTableModel<WeaveInput> myModel)
        {
            this.myModel = myModel;
        }

        @Override
        public void run(AnActionButton button)
        {
            TableUtil.stopEditing(myInputsTable);
            final int[] selected = myInputsTable.getSelectedRows();
            if (selected.length == 0)
                return;
            final ArrayList<WeaveInput> weaveInputs = new ArrayList<>(myModel.getItems());
            for (int i : selected)
            {
                weaveInputs.remove(i);
            }

            for (int i = selected.length - 1; i >= 0; i--)
            {
                int idx = selected[i];
                myModel.fireTableRowsDeleted(idx, idx);
            }
            myModel.setItems(weaveInputs);
            myInputsTable.requestFocus();
        }
    }

    private class EditWeaveInputAction implements AnActionButtonRunnable
    {
        private final ListTableModel<WeaveInput> myModel;

        public EditWeaveInputAction(ListTableModel<WeaveInput> myModel)
        {
            this.myModel = myModel;
        }

        @Override
        public void run(AnActionButton button)
        {
            final int selectedRow = myInputsTable.getSelectedRow();
            //noinspection ConstantConditions
            showEditorDialog(myInputsTable.getSelectedObject());
            myModel.fireTableDataChanged();
            myInputsTable.setRowSelectionInterval(selectedRow, selectedRow);
        }
    }
}


