package com.mimedia.poc.jade.agent;

import javax.swing.*;

import com.mimedia.poc.jade.agent.form.ProcessTaskForm;

public class ProcessTasksGui {
    private final ProcessTasksAgent agent;

    private JFrame frame;

    public ProcessTasksGui(ProcessTasksAgent agent) {
        this.agent = agent;
    }

    public void show() {
        ProcessTaskForm processTaskForm = new ProcessTaskForm();
        processTaskForm.getProcessTasksButton().addActionListener(e -> new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                agent.processNextTasks();
                return null;
            }
        }.execute());
        frame = new JFrame();
        frame.add(processTaskForm.getMainPanel());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void dispose() {
        frame.setVisible(false);
        frame.dispose();
        frame = null;
    }
}
