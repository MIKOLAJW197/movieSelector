package com.drools.movieSelector;

import org.kie.api.KieServices;
import org.kie.api.logger.KieRuntimeLogger;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MovieSelector {

    public static void main(String[] args) {
        try {
            KieServices ks = KieServices.Factory.get();
            KieContainer kContainer = ks.getKieClasspathContainer();
            KieSession kSession = kContainer.newKieSession("ksession-rules");
            KieRuntimeLogger kLogger = ks.getLoggers().newFileLogger(kSession, "test");

            MovieUI ui = new MovieUI(new StartCallback(kSession), new AcceptCallback(kSession));
            ui.createAndShowGUI();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static class MovieUI extends JPanel {
        private JTextArea output;
        private JPanel optionsContainer;
        private StartCallback startCallback; // note: obsługa klawisza START
        private AcceptCallback acceptCallback; // note: obsługa klawisza NextQuestion

        public MovieUI(StartCallback startCallback, AcceptCallback acceptCallback) {
            super(new BorderLayout());

            this.startCallback = startCallback;
            this.acceptCallback = acceptCallback;

            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            add(splitPane,
                    BorderLayout.CENTER);

            //create top half of split panel and add to parent
            JPanel topHalf = new JPanel();
            topHalf.setLayout(new BoxLayout(topHalf, BoxLayout.X_AXIS));
            topHalf.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
            topHalf.setMinimumSize(new Dimension(400,
                    50));
            topHalf.setPreferredSize(new Dimension(450,
                    250));
            splitPane.add(topHalf);

            //create bottom top half of split panel and add to parent
            JPanel bottomHalf = new JPanel(new BorderLayout());
            bottomHalf.setMinimumSize(new Dimension(400,
                    50));
            bottomHalf.setPreferredSize(new Dimension(450,
                    300));
            splitPane.add(bottomHalf);

            //Container that list container that shows available store items
            optionsContainer = new JPanel(new GridLayout(0, 1));
            optionsContainer.setBorder(BorderFactory.createTitledBorder("Options"));
            topHalf.add(optionsContainer);


            JPanel tableContainer = new JPanel(new GridLayout(1,
                    1));
            tableContainer.setBorder(BorderFactory.createTitledBorder("Table"));
            topHalf.add(tableContainer);


            //Create panel for checkout button and add to bottomHalf parent
            JPanel buttonPane = new JPanel();
            JButton button = new JButton("Start");
            button.setVerticalTextPosition(AbstractButton.CENTER);
            button.setHorizontalTextPosition(AbstractButton.LEADING);
            //attach handler to assert items into working memory
            button.addMouseListener(new StartButtonHandler());
            button.setActionCommand("checkout");
            buttonPane.add(button);
            bottomHalf.add(buttonPane, BorderLayout.NORTH);

            button = new JButton("Next Question!");
            button.setVerticalTextPosition(AbstractButton.CENTER);
            button.setHorizontalTextPosition(AbstractButton.TRAILING);
            //attach handler to assert items into working memory
            button.addMouseListener(new AcceptButtonHandler());
            button.setActionCommand("accept");
            buttonPane.add(button);
            bottomHalf.add(buttonPane, BorderLayout.NORTH);


            output = new JTextArea(1, 10);
            output.setEditable(false);
            JScrollPane outputPane = new JScrollPane(output,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            bottomHalf.add(outputPane,
                    BorderLayout.CENTER);
//            output.append("tutaj chyba bysmy mogli wypisywać pytanie\n");
//            output.append("tam gdzie jest \"List\" mozna chyba wyswietlac dostepne opcje do pytania\n");

            this.startCallback.setTextArea(output);
            this.startCallback.setOptionsContainer(optionsContainer);
            this.acceptCallback.setOptionsContainer(optionsContainer);
        }

        public void createAndShowGUI() {
            JFrame frame = new JFrame("Movie Selector for GF");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            setOpaque(true);
            frame.setContentPane(this);

            frame.pack();
            frame.setVisible(true);
        }

        private class StartButtonHandler extends MouseAdapter {
            public void mouseReleased(MouseEvent e) {
                JButton button = (JButton) e.getComponent();
                startCallback.checkout((JFrame) button.getTopLevelAncestor());
            }
        }

        private class AcceptButtonHandler extends MouseAdapter {
            public void mouseReleased(MouseEvent e) {
                acceptCallback.accept();
            }
        }
    }

    public static class StartCallback {
        KieSession kSession;
        JTextArea textArea;
        JPanel optionsContainer;

        public StartCallback(KieSession kSession) {
            this.kSession = kSession;
        }

        public void setTextArea(JTextArea textArea) {
            this.textArea = textArea;
        }

        public void setOptionsContainer(JPanel optionsContainer) {
            this.optionsContainer = optionsContainer;
        }


        public void checkout(JFrame frame) {
            kSession.setGlobal("frame", frame);
            kSession.setGlobal("optionsContainer", this.optionsContainer);
            kSession.setGlobal("textArea", this.textArea);
            kSession.fireAllRules();
        }
    }

    public static class AcceptCallback {
        KieSession kSession;
        JPanel optionsContainer;

        public AcceptCallback(KieSession kSession) {
            this.kSession = kSession;
        }

        public void setOptionsContainer(JPanel optionsContainer) {
            this.optionsContainer = optionsContainer;
        }

        public void accept() {
            // note: obsługa wybrania odpowiedniego klawisza z odpowiedzia
            for (Component c : optionsContainer.getComponents()) {
                if (c instanceof JRadioButton && ((JRadioButton) c).isSelected()) {
                    QueryResults results = kSession.getQueryResults("getQuestionsWithNoAnswer");
                    for (QueryResultsRow row : results) {
                        Question question = (Question) row.get("question"); // note: pytania bez odpowiedzi
                        FactHandle fh = this.kSession.getFactHandle(question);
                        question.setAnswer(((JRadioButton) c).getText());

                        this.kSession.update(fh,question);

                        kSession.fireAllRules();
                        //note: wyswietl pytanie w konsoli
                        //System.out.println(question.getText());
                    }
                    // note: wyswietl odpowiedz wybrana
                    //System.out.println(((JRadioButton) c).getText());
                    break;
                }
            }
        }
    }

    public static class Question {
        String text;
        String answer;

        public Question() {
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }
}
