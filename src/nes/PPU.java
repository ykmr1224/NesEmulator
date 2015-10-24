package nes;
import java.awt.Color;

import debug.Debug;
import debug.MemoryViewer;



public class PPU {
	byte[] ptnTbl = new byte[0x2000];
	byte[] nameTbl = new byte[0x0800];
	byte[] palettes = new byte[0x20];
	
	CPU cpu;
	Monitor monitor;
	
	public PPU(CPU cpu, Monitor monitor) {
		this.cpu = cpu;
		this.monitor = monitor;
	}
	
	private interface MemRegion{
		int read(int addr);
		void write(int addr, int data);
	}
	
	//$0000-$1fff
	MemRegion PatternTable = new MemRegion(){
		public int read(int addr) {
			return ptnTbl[addr];
		};
		public void write(int addr, int data) {
			ptnTbl[addr] = (byte)(data&0xff);
		};
	};
	
	//$2000-$3eff
	MemRegion NameTablesH = new MemRegion(){
		public int read(int addr) {
			addr = (addr&0x3ff)|((addr&0x800)>>1);
			return nameTbl[addr];
		};
		public void write(int addr, int data) {
			addr = (addr&0x3ff)|((addr&0x800)>>1);
			nameTbl[addr] = (byte)(data&0xff);
		};
	};
	MemRegion NameTablesV = new MemRegion(){
		public int read(int addr) {
			return nameTbl[addr&0x7ff];
		};
		public void write(int addr, int data) {
			nameTbl[addr&0x7ff] = (byte)(data&0xff);
		};
	};

	MemRegion NameTables = NameTablesH;
	
	//$3f00-$3fff
	MemRegion Palettes = new MemRegion(){
		public int read(int addr) {
			Debug.println("palettes read : ["+Debug.hex2(addr&0x1f)+"]="+Debug.hex2(palettes[addr&0x1f]));
			return palettes[addr&0x1f];
		};
		public void write(int addr, int data) {
			Debug.println("palettes write : ["+Debug.hex2(addr&0x1f)+"]="+Debug.hex2(data&0xff));
			palettes[addr&0x1f] = (byte)(data&0xff);
			if((addr&0x3) == 0){
				Debug.println("palettes write : ["+Debug.hex2((addr&0x1f)^0x10)+"]="+Debug.hex2(data&0xff));
				palettes[(addr&0x1f)^0x10] = (byte)(data&0xff);
			}
		};
	};
	
	MemRegion map(int addr){
		if(addr >= 0x4000)
			addr -= 0x4000;
		
		if(addr < 0x2000){
			return PatternTable;
		}else if(addr < 0x3f00){
			return NameTables;
		}else{
			return Palettes;
		}
	}
	
	int bit(byte data, int b){return (data>>b)&1;}
	boolean bitTest(byte data, int b){return ((data>>b)&1)==1;}
	
	boolean execNMI = false;
	boolean ppuSlave = false;
	
	static final int SPRITE_SIZE8 = 0;
	static final int SPRITE_SIZE16 = 1;
	int spriteSize = SPRITE_SIZE8;
	
	int bgPatternTable = 0;
	int spPatternTable = 0;
	int ppuAddressIncrement = 1;
	int nameTableAddress = 0;
	
	void writeControlRegister1(byte data){
		execNMI = bitTest(data, 7);
		ppuSlave = bitTest(data, 6);
		spriteSize = (bitTest(data, 5)?SPRITE_SIZE16:SPRITE_SIZE8);
		bgPatternTable = bit(data, 4);
		spPatternTable = bit(data, 3);
		ppuAddressIncrement = bitTest(data,2)?32:1;
		nameTableAddress = data&3;
		
		Debug.println("wCR1:"+Debug.bit8(data));
	}
	
	static final int BGCOLOR_NONE = 0;
	static final int BGCOLOR_GREEN = 1;
	static final int BGCOLOR_BLUE = 2;
	static final int BGCOLOR_RED = 4;
	int fullBackgroundColor = BGCOLOR_NONE;

	boolean spriteVisible = true;//t:visible, f:not visible
	boolean bgVisible = true;//t:visible, f:not visible
	boolean spriteClip = true;//t:no clipping, f:clipping
	boolean bgClip = true;//t:no clipping, f:clipping
	boolean displayType = false;//t:Monochrome, f:colour
	
	void writeControlRegister2(byte data){
		fullBackgroundColor = (data>>5)&7;
		spriteVisible = bitTest(data,4);
		bgVisible = bitTest(data,3);
		spriteClip = bitTest(data,2);
		bgClip = bitTest(data,1);
		displayType = bitTest(data,0);
	}
	
	int vblankOccurance = 1;
	int sp0Occurance = 0;
	int scanlineSpriteCount = 0;
	int vramWriteFlag = 0;
	
	byte readStatusRegister(){
		int res = 0;
		res |= vblankOccurance<<7;
		res |= sp0Occurance<<6;
		res |= scanlineSpriteCount<<5;
		res |= vramWriteFlag<<4;
		vblankOccurance = 0;
		horizontal = true;
		Debug.println("readStatusRegister : " + Debug.hex2(res));
		return (byte)res;
	}
	
	int HORIZONTAL_MIRROR = 0;
	int VERTICAL_MIRROR = 1;
	int FOUR_SCREEN = 2;
	int mirroring = HORIZONTAL_MIRROR;
	
	public void setMirroring(int mirroring){
		this.mirroring = mirroring;
		if(mirroring == HORIZONTAL_MIRROR){
			NameTables = NameTablesH;
		}else if(mirroring == VERTICAL_MIRROR){
			NameTables = NameTablesV;
		}else{
			NameTables = NameTablesH;
		}
	}
	
	int read(int addr){
		addr &= 0x3fff;
		return map(addr).read(addr);
	}

	void write(int addr, int data){
		addr &= 0x3fff;
		map(addr).write(addr, data);
	}
	
	class SPRRAM{
		byte[] data = new byte[256];
		int addr = 0;
		public void setAddress(int addr){
			this.addr = addr;
		}
		public int getAddress(){
			return addr;
		}
		public void write(byte data){
			this.data[addr++] = data;
		}
		public byte read(){
			return this.data[addr];
		}
		public void dma(byte[] src, int addr){
			for(int i=0; i<256; i++){
				data[(this.addr+i)&0xff] = src[addr+i];
			}
		}
	}
	
	SPRRAM sprram = new SPRRAM();
	
	public SPRRAM getSPRRAM(){
		return sprram;
	}
	
	//$2005
	boolean horizontal = true;//t:pan, f:scroll;
	int hscroll = 0;
	int vscroll = 0;
	
	void setScroll(byte val){
		if(horizontal){
			hscroll = val&0xff;
		}else{
			vscroll = val&0xff;
		}
		horizontal = !horizontal;
	}
	
	int vramaddr = 0;
	
	void setVramAddr(byte addr){
//		System.out.println("setVramAddr:"+Debug.hex2(addr));
		vramaddr = ((vramaddr<<8)&0xff00)|(addr&0xff);
	}
	int vramBuffer = 0;
	int readVram(){
//		System.out.println("readVram:"+Debug.hex4(vramaddr));
		int res = vramBuffer;
		vramBuffer = read(vramaddr);
		if(vramaddr >= 0x3f00){
			res = vramBuffer;
			vramBuffer = read(0x2f00+(vramaddr&0xff));
		}
		vramaddr = vramaddr + ppuAddressIncrement;
		return res;
	}
	void writeVram(int data){
//		System.out.println("writeVram:"+Debug.hex4(vramaddr));
		write(vramaddr, data);
		vramaddr = vramaddr + ppuAddressIncrement;
	}

	private byte[][] makePattern(byte upper, int i){
		byte[][] pattern = new byte[8][8];
		for(int j=0; j<8; j++){
			byte data0 = ptnTbl[i*16+j];
			byte data1 = ptnTbl[i*16+8+j];
			for(int k=0; k<8; k++){
				pattern[j][k] = (byte)(upper|((data0>>(7-k))&1)|(((data1>>(7-k))&1)<<1));
			}
		}
		return pattern;
	}
	
	private byte getUpperBit(int scr, int x, int y){
		int i = (y/4)*8 + (x/4);
		int bit = ((y%4)/2)*2 + ((x%4)/2);
		return (byte)(((nameTbl[0x3C0+0x400*scr+i]>>(bit*2))&3)<<2);
	}
	
	private int getColor(int palettes, byte color){
		return this.palettes[0x10*palettes+color];
	}
	
	static final int IMAGE_PALETTE = 0;
	static final int SPRITE_PALETTE = 1;
	
	private void renderPattern(byte[][] pattern, int x, int y, int palettes, boolean vflip, boolean hflip){
		for(int i=0; i<8; i++){
			for(int j=0; j<8; j++){
				byte c = pattern[vflip?7-i:i][hflip?7-j:j];
				if((c&0x3)!=0)
					monitor.setPixel(x+j, y+i, getColor(palettes, c));
			}
		}
	}
	
	private void drawBG(int scr, int voffset, int hoffset){
		for(int i=0; i<30; i++){
			for(int j=0; j<32; j++){
				int pattern = 0xff&nameTbl[i*32+j+scr*0x400];
				byte upper = getUpperBit(scr, j, i);
				byte[][] p = makePattern(upper, bgPatternTable*256+pattern);
				renderPattern(p, hoffset+j*8, voffset+i*8, IMAGE_PALETTE, false, false);
			}
		}
	}
	
	private void drawBG(){
		if(mirroring == HORIZONTAL_MIRROR){
			drawBG(nameTableAddress/2, -vscroll, -hscroll);
			drawBG((nameTableAddress/2+1)%2, 30*8-vscroll, 0);
		}else if(mirroring == VERTICAL_MIRROR){
			drawBG(nameTableAddress%2, -vscroll, 0);
			drawBG((nameTableAddress%2+1)%2, 30*8-vscroll, 0);
		}
	}
	
	private void drawSprites(int bg){
		for(int i=0; i<64; i++){
			byte attr = sprram.data[i*4+2];
			if(((attr>>5)&1)==bg){
				int y = 0xff&sprram.data[i*4];
				int x = 0xff&sprram.data[i*4+3];
				int tile = 0xff&sprram.data[i*4+1];
				byte upper = (byte)((attr&3)<<2);
				if(spriteSize == SPRITE_SIZE8){
					byte[][] pat = makePattern(upper, spPatternTable*256+tile);
					renderPattern(pat, x, y, SPRITE_PALETTE, ((attr>>7)&1)==1, ((attr>>6)&1)==1);
				}else if(spriteSize == SPRITE_SIZE16){
					int table = tile%2;
					byte[][] pat = makePattern(upper, table*256+(tile/2*2));
					renderPattern(pat, x, y, SPRITE_PALETTE, ((attr>>7)&1)==1, ((attr>>6)&1)==1);
					pat = makePattern(upper, table*256+(tile/2*2+1));
					renderPattern(pat, x, y+8, SPRITE_PALETTE, ((attr>>7)&1)==1, ((attr>>6)&1)==1);
				}
			}
		}
	}

	int dbgCount = 0;
	public void render(boolean draw){
		if(draw && !monitor.painting){
			monitor.clear();
			sp0Occurance = 1;
			drawSprites(1);
			drawBG();
			drawSprites(0);
			monitor.g.setColor(Color.BLUE);
			monitor.g.drawString("Flame:"+dbgCount++, 0, 20);
			monitor.g.drawString("Clock:"+cpu.clock, 0, 40);
			monitor.g.setColor(Color.black);
			monitor.refresh();
		}
		vblankOccurance = 1;
		sp0Occurance = 0;
		if(execNMI){
//			monitor.dbgClicked = false;
			cpu.setNMI();
		}
	}
	
	public void loadCHRROM(byte[] data, int offset){
		for(int j=0; j<0x2000; j++){
			ptnTbl[j] = data[offset+j];
		}
	}
	
	public void showDebugWindow(){
		new MemoryViewer("PatternTable", ptnTbl);
		new MemoryViewer("NameTable", nameTbl);
		new MemoryViewer("Palettes", palettes);
		new MemoryViewer("SPRRAM", sprram.data);
	}
}
