package nes;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;


public class DebugScreen extends javax.swing.JPanel{
	JFrame frame;
	BufferedImage buff;
	public DebugScreen(){
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(this);
		buff = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
		frame.setVisible(true);
		frame.pack();
	}
	
	public Graphics getGraphics(){
		return buff.getGraphics();
	}
	
	public void update(){
		this.repaint();
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(640,480);
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.drawImage(buff, 0, 0, null);
	}
}
