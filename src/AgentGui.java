package mams;

import jade.core.AID;

import java.awt.*;
import java.text.DecimalFormat;
import java.awt.event.*;
import javax.swing.*;

class AgentGui extends JFrame {
    private AgentMAMS myAgent;
    private JPanel p;

    private JTextField titleField;

AgentGui(AgentMAMS a, Double[][] cal) {
        super(a.getLocalName());

        myAgent = a;

        this.p = new JPanel();
        p.setLayout(new GridLayout(2, 2));
        
        displayCalendar(cal);

        JButton addButton = new JButton("Schedule a meeting");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    myAgent.lookForMeeting();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(AgentGui.this, "Invalid values. " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        p = new JPanel();
        p.add(addButton);
        getContentPane().add(p, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });

        setResizable(false);
    }

    public void displayCalendar(Double[][] CAL){
        String[] columns = {"TIME","Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        Object[][] data = new Object[CAL[1].length][CAL.length+1];
        for(int i = 0; i< CAL.length; i++){
            for (int j = 0; j< CAL[i].length; j++) {
                data[j][0] = j + ":00";
                data[j][i+1] = (CAL[i][j]==0?"":new DecimalFormat("#.##").format(CAL[i][j]));
                if (CAL[i][j] == 1000.0){
                    data[j][i+1] = "MEETING";
                }
            }
        }
        JTable calendar = new JTable(data,columns);
        calendar.setGridColor(Color.black);
        p.setLayout(new BorderLayout());
        p.add(calendar.getTableHeader(), BorderLayout.PAGE_START);
        p.add(calendar, BorderLayout.CENTER);
        getContentPane().add(p, BorderLayout.CENTER);
    }

    public void display() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int) screenSize.getWidth() / 2;
        int centerY = (int) screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2 -300);
        setVisible(true);
    }
}
