
package org.cougaar.lib.aggagent.test;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *  Test program that connects to a keep-alive PSP and continuously reports its
 *  findings to a GUI display.
 */
public class KeepAliveClient {
  public static void startUi () {
    System.out.println("KeepAliveClient::startUi:  starting");
    JFrame f = new JFrame();
    Container c = f.getContentPane();
    c.setLayout(new BorderLayout());
    c.add(new Display(), BorderLayout.CENTER);
    f.setSize(600, 600);
    f.setVisible(true);
    f.addWindowListener(new WindowEar());
    System.out.println("KeepAliveClient::startUi:  done");
  }

  private static class WindowEar implements WindowListener {
    public void windowClosing (WindowEvent e) {
      System.exit(0);
    }

    public void windowClosed (WindowEvent e) {
      System.exit(0);
    }

    public void windowOpened (WindowEvent e) { }
    public void windowIconified (WindowEvent e) { }
    public void windowDeiconified (WindowEvent e) { }
    public void windowActivated (WindowEvent e) { }
    public void windowDeactivated (WindowEvent e) { }
  }

  private static class Display extends JPanel {
    private JTextArea text = new JTextArea();
    private JScrollPane scroll = new JScrollPane(text);
    private JTextField input = new JTextField();
    private JButton button = new JButton("Go");

    public Display () {
      text.setEditable(false);
      text.setBackground(Color.lightGray);
      input.setText("http://localhost:5555/$TestSink/test/ka.psp");
      button.addActionListener(new GoEar());
      setLayout(new LayoutMeister());
      setBackground(Color.white);
      add(scroll);
      add(input);
      add(button);
    }

    public void appendLine (String s) {
      text.append(s);
      text.append("\n");
    }

    public void appendChar (char c) {
      if (c == '\r')
        return;
      text.append(String.valueOf(c));
    }

    public void go (URL u) {
      new Thread(new ConnReader(u)).start();
    }

    private class ConnReader implements Runnable {
      private URL url = null;

      public ConnReader (URL u) {
        url = u;
      }

      public void run () {
        try {
          URLConnection conn = url.openConnection();
          conn.setDoOutput(false);
          conn.setDoInput(true);
          InputStream i = conn.getInputStream();
          int c = -1;
          while ((c = i.read()) != -1) {
            appendChar((char) c);
          }
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    private class GoEar implements ActionListener {
      public void actionPerformed (ActionEvent ae) {
        System.out.println("KeepAliveClient::Display::GoEar::actionPerformed");
        System.out.println(" -> Gathered input:  \"" + input.getText() + "\"");
        try {
          URL u = new URL(input.getText());
          go(u);
        }
        catch (Exception eek) {
          System.out.println("KeepAliveClient::Display::GoEar::actionPerformed:  bad URL");
        }
      }
    }

    private class LayoutMeister implements LayoutManager {
      public void addLayoutComponent (String name, Component comp) { }
      public void removeLayoutComponent (Component comp) { }

      public Dimension minimumLayoutSize (Container c) {
        Dimension ret = new Dimension();
        Dimension butSize = button.getPreferredSize();
        ret.width = Math.max(300, 250 + butSize.width);
        ret.height = Math.max(300, 250 +
          Math.max(butSize.height, input.getPreferredSize().height));
        return ret;
      }

      public Dimension preferredLayoutSize (Container c) {
        return minimumLayoutSize (c);
      }

      public void layoutContainer (Container c) {
        Dimension d = c.getSize();
        Dimension butSize = button.getPreferredSize();
        int x1 = d.width - butSize.width;
        int y1 = d.height -
          Math.max(butSize.height, input.getPreferredSize().height);
        scroll.setBounds(0, 0, d.width, y1 - 2);
        input.setBounds(0, y1, x1 - 2, d.height - y1);
        button.setBounds(x1, y1, d.width - x1, d.height - y1);
      }
    }
  }

  public static void main (String[] argv) {
    startUi();
  }
}
