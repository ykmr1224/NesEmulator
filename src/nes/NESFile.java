package nes;
import java.awt.Color;
import java.awt.Graphics;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;


public class NESFile {
	File file;
	FileInputStream in;
	int PRGROMCount;
	int CHRROMCount;
	byte ROMCtr1;
	byte ROMCtr2;
	int mirror;
	int sram;
	int trainer;
	int fourscreen;
	int mapper;
	
	byte[][] PRGROM;
	byte[][] CHRROM;
	
	public NESFile(File file){
		this.file = file;
	}
	
	private boolean match(byte[] a, byte[] b, int start, int len){
		for(int i=start; i<start+len; i++)
			if(a[i] != b[i]) return false;
		return true;
	}
	
	public boolean load() throws IOException{
		in = new FileInputStream(file);
		
		byte[] buff = new byte[16];
		in.read(buff);
		
		if(!match(buff, new byte[]{'N', 'E', 'S', 0x1A}, 0, 4)){
			debug(new String(buff));
			return false;
		}
		
		PRGROMCount = (int)buff[4]; debug("PRG-ROM Count : " + PRGROMCount);
		CHRROMCount = (int)buff[5]; debug("CHR-ROM Count : " + CHRROMCount);
		ROMCtr1 = buff[6]; debug("ROM Control Byte #1 : " + bit(ROMCtr1));
		ROMCtr2 = buff[7]; debug("ROM Control Byte #2 : " + bit(ROMCtr2));
		
		mirror = ROMCtr1&1; debug("Mirroring : " + mirror);
		sram = (ROMCtr1>>1)&1; debug("SRAM enabled : " + sram);
		trainer = (ROMCtr1>>2)&1; debug("512-byte trainer present : " + trainer);
		fourscreen = (ROMCtr1>>3)&1; debug("Four-screen mirroring : " + fourscreen);
		mapper = ((ROMCtr1>>4)&0xf)|(ROMCtr2&0xf0); debug("Mapper : " + mapper);
		
		PRGROM = new byte[PRGROMCount][0x4000];
		CHRROM = new byte[CHRROMCount][0x2000];
		
		for(int i=0; i<PRGROMCount; i++)
			in.read(PRGROM[i]);
		for(int i=0; i<CHRROMCount; i++)
			in.read(CHRROM[i]);
		
		in.close();
		
		return true;
	}
	
	private void printPattern(Graphics g, byte[] chr, int i){
		byte[][] pattern = new byte[8][8];
		Color[] c = new Color[]{Color.black, Color.red, Color.blue, Color.green};
		for(int j=0; j<8; j++){
			byte data0 = chr[i*16+j];
			byte data1 = chr[i*16+8+j];
			for(int k=0; k<8; k++){
				pattern[j][k] = (byte)(((data0>>(7-k))&1)|(((data1>>(7-k))&1)<<1));
			}
		}
		for(int j=0; j<8; j++){
			StringBuffer buff = new StringBuffer();
			for(int k=0; k<8; k++){
				buff.append(pattern[j][k]);
				g.setColor(c[pattern[j][k]]);
				g.fillRect((i%32)*18+k*2, (i/32)*18+j*2, 2, 2);
			}
			debug(buff.toString());
		}
		debug("");
	}
	
	private void printPatterns(){
		DebugScreen scr = new DebugScreen();
		Graphics g = scr.getGraphics();
		
		for(int i=0; i<0x2000/16; i++)
			printPattern(g, CHRROM[0], i);
		
	}
	
	private String bit(byte b){
		StringBuffer buff = new StringBuffer();
		for(int i=0; i<8; i++)
			buff.append((b>>(7-i))&1);
		return buff.toString();
	}
	
	private void debug(String str){
		System.out.println(str);
	}
	
	private void showPatterns(){
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	public static void main(String[] args) throws IOException {
		JFileChooser ch = new JFileChooser();
		int res = ch.showOpenDialog(null);
		if(res == JFileChooser.APPROVE_OPTION){
			NESFile file = new NESFile(ch.getSelectedFile());
			file.load();
			file.printPatterns();
		}
	}
}
