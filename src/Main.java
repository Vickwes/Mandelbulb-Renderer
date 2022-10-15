import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args){
        JFrame testFrame = new JFrame();
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Raymarcher comp = new Raymarcher();
        comp.setPreferredSize(new Dimension(700, 700));
        testFrame.getContentPane().add(comp);
        comp.setFocusable(true);
        comp.requestFocusInWindow();
        testFrame.pack();
        testFrame.setVisible(true);
        while(true){
            try{
                comp.repaint();
                Thread.sleep(20);
            }catch(InterruptedException ie){

            }
        }
    }
}
